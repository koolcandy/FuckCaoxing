package com.example.fuckcaoxing

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// 数据模型
data class SearchResult(
    val scores: Double,
    val question: String,
    val answer: String
)

data class Part(
    val text: String // 修改为 String 类型
)

data class Content(
    val parts: List<Part>,
    val role: String
)

data class Candidate(
    val content: Content,
    val finishReason: String,
    val avgLogprobs: Double
)

data class UsageMetadata(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int
)

data class ApiResponse(
    val candidates: List<Candidate>,
    val usageMetadata: UsageMetadata,
    val modelVersion: String
)

data class Text(
    val answer: String,
    val isrelated: Boolean
)

class MainService {
    companion object {

        suspend fun getans(problemList: String, context: Context) {
            val answer = getAnswerByApi(problemList, context).replace("\n", "").replace(" ", "")
            val sortingBuilder = """problem:{{{${problemList}}}}reference:{{${answer}}},just give me answer,not the choice"""
            val geminiAnswer = askGemini(sortingBuilder, context)
            Log.d("MainService", "Gemini Answer: $geminiAnswer")
            val intent = Intent(context, FloatingWindowService::class.java)
            intent.putExtra("newText", geminiAnswer)
            context.startService(intent)
            Log.d("MainService", "Service started with newText: $geminiAnswer")

            delay(2000)

            val clearIntent = Intent(context, FloatingWindowService::class.java)
            clearIntent.putExtra("newText", "")
            context.startService(clearIntent)
            Log.d("MainService", "Service started to clear newText")
        }

        private suspend fun getAnswerByApi(question: String, context: Context): String {
            return withContext(Dispatchers.IO) {
                var response: String? = null
                val url = "http://so.studypro.club/api/search"
                val key = getSavedKey(context, 1)
                val gson = Gson()
                val payload = gson.toJson(mapOf("question" to question, "phone" to key))
                val headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")

                try {
                    val urlObj = URL(url)
                    val connection = urlObj.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
                    connection.doOutput = true

                    val writer = OutputStreamWriter(connection.outputStream)
                    writer.write(payload)
                    writer.flush()
                    writer.close()

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        response = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d("MainService", "Response: $response")
                    } else {
                        Log.d("MainService", "Error: ${connection.responseMessage}")
                    }
                } catch (e: Exception) {
                    Log.d("MainService", "Error: ${e.message}")
                }

                val type = object : TypeToken<List<SearchResult>>() {}.type
                val searchResults: List<SearchResult> = gson.fromJson(response, type)

                val resultString = buildString {
                    searchResults.forEach { result ->
                        append("Question: ${result.question.replace("\n", "")}\nAnswer: ${result.answer.replace("\n", "")}\n\n")
                    }
                }

                Log.d("MainService", "Search Results: $resultString")
                return@withContext resultString
            }
        }

        private suspend fun askGemini(inputText: String, context: Context): String {
            val apiKey = getSavedKey(context, 2)
            val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val gson = Gson()

            val requestBody = mapOf(
                "contents" to listOf(
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(mapOf("text" to inputText))
                    )
                ),
                "generationConfig" to mapOf(
                    "temperature" to 1,
                    "topK" to 40,
                    "topP" to 0.95,
                    "maxOutputTokens" to 8192,
                    "responseMimeType" to "application/json",
                    "responseSchema" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "answer" to mapOf("type" to "string"),
                            "isrelated" to mapOf("type" to "boolean")
                        ),
                        "required" to listOf("answer", "isrelated")
                    )
                )
            )

            val jsonRequestBody = gson.toJson(requestBody)

            Log.d("API_REQUEST", "Request: $jsonRequestBody")

            return withContext(Dispatchers.IO) {
                var response: String? = null
                var connection: HttpURLConnection? = null
                try {
                    val url = URL(urlString)
                    connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true

                    OutputStreamWriter(connection.outputStream).use { writer ->
                        writer.write(jsonRequestBody)
                        writer.flush()
                    }

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.bufferedReader().use { reader ->
                            response = reader.readText()
                            Log.d("API_REQUEST", "Response: $response")
                        }
                    } else {
                        Log.e("API_REQUEST", "请求失败，响应码：${connection.responseCode}")
                    }
                } catch (e: Exception) {
                    Log.e("API_REQUEST", "请求异常：${e.message}", e)
                } finally {
                    connection?.disconnect()
                }

                val type1 = object : TypeToken<ApiResponse>() {}.type
                val apiResponse: ApiResponse = Gson().fromJson(response, type1)
                val rawText = apiResponse.candidates[0].content.parts[0].text

                // 解析嵌套的 JSON
                val parsedText = Gson().fromJson(rawText, Text::class.java)
                val answer = parsedText.answer
                val isrelated = parsedText.isrelated

                if (isrelated) {
                    return@withContext answer
                } else {
                    return@withContext ""
                }
            }
        }

        private fun getSavedKey(context: Context, key: Int): String? {
            val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val key1 = sharedPreferences.getString("key1", null)
            val key2 = sharedPreferences.getString("key2", null)
            return if (key == 1) key1 else key2
        }
    }
}
