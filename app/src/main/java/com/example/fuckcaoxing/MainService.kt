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

class MainService {
    companion object {

        suspend fun getAnswer(question: String, context: Context) {
            return withContext(Dispatchers.IO) {
                var response: String? = null
                val url = "http://so.studypro.club/api/search"
                val key = getSavedKey(context)
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

                var resultString = ""

                searchResults.forEach { result ->
                    resultString += "Question: ${result.question.replace("\n", "")}\nAnswer: ${result.answer.replace("\n", "")}\n\n"
                }

                Log.d("MainService", "Search Results: $resultString")

                val intent = Intent(context, FloatingWindowService::class.java)
                intent.putExtra("newText", resultString)
                context.startService(intent)
                Log.d("MainService", "Service started with newText: $resultString")

                delay(2000)

                val clearIntent = Intent(context, FloatingWindowService::class.java)
                clearIntent.putExtra("newText", "")
                context.startService(clearIntent)
                Log.d("MainService", "Service started to clear newText")
            }
        }

        private fun getSavedKey(context: Context): String? {
            val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val key = sharedPreferences.getString("key", null)
            Log.d("MainService", "Retrieved saved key: $key")
            return key
        }
    }
}