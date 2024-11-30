package com.example.fuckcaoxing

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class Answer(val choice: List<String>, val answer: List<String>)
data class Message(val role: String, val content: String)
data class Choice(val finish_reason: String, val message: Message)
data class Output(val text: String?, val finish_reason: String?, val choices: List<Choice>)
data class ApiResponse(
    val status_code: Int,
    val request_id: String,
    val code: String,
    val message: String,
    val output: Output,
    val usage: JsonObject
)

class MainService {
    companion object {
        private const val TAG = "MainService"
        private const val API_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"

        fun getAnswer(problem: String, content: Context): Answer {
            val client = OkHttpClient.Builder().build()

            val systemPrompt =
                """
            Give me answer about《毛泽东思想和中国特色社会主义理论体系概论》. 
            Please parse the "choice" and "answer" and output them in JSON format.

            EXAMPLE INPUT: 
            
            1.新中国初期建立社会主义国营经济的主要途径是()
            A, 没收帝国主义在华企业
            B, 剥地主阶级的财产
            C, 赎买民族资产阶级的财产
            D, 没收官僚资本
            
            EXAMPLE JSON OUTPUT:
            {
                "choice": ["D"],
                "answer": ["没收官僚资本"]
            }
            
            EXAMPLE INPUT: 
            
            1.共享是中国特色社会主义的本质要求。其内涵主要包括()
            A、全民共享
            B、全面共享
            C、共建共享
            D、渐进共享
            
            EXAMPLE JSON OUTPUT:
            {
                'choice': ["A", "B", "C", "D"],
                "answer": ["全民共享", "全面共享", "共建共享", "渐进共享"]
            }
                """.trimIndent()

            val messages = listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to problem)
            )

            val parameters = mapOf(
                "model" to "qwen-plus",
                "messages" to messages,
                "result_format" to "message",
                "top_p" to 0.8,
                "temperature" to 0.7,
                "enable_search" to true,
                "response_format" to mapOf("type" to "json_object")
            )

            val gson = Gson()
            val messagesJson = gson.toJson(messages)
            val parametersJson = gson.toJson(parameters)

            val mediaType = "application/json".toMediaType()
            val body = """
                {
                    "model": "qwen-plus",
                    "input":{
                        "messages": $messagesJson
                    },
                    "parameters": $parametersJson
                }
            }
            """.trimIndent().toRequestBody(mediaType)

            val API_KEY = getSavedKey(content)

            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val apiResponse = gson.fromJson(responseBody, ApiResponse::class.java)
                        val answerContent = apiResponse.output.choices[0].message.content
                        val parsedContent = JsonParser.parseString(answerContent).asJsonObject

                        val choice = parsedContent.getAsJsonArray("choice").map { it.asString }
                        val answer = parsedContent.getAsJsonArray("answer").map { it.asString }

                        return Answer(choice, answer)
                    } else {
                        Log.e(TAG, "Response body is null")
                    }
                } else {
                    Log.e(TAG, "HTTP request failed with status: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during API call: ${e.message}", e)
            }

            return Answer(emptyList(), emptyList())
        }

        private fun getSavedKey(context: Context): String? {
            val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            return sharedPreferences.getString("key", null)
        }
    }
}