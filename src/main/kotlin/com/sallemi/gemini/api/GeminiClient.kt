package com.sallemi.gemini.api

import com.sallemi.gemini.settings.GeminiSettingsState
import com.sallemi.gemini.ui.GeminiConversation
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Gemini API client with correct payload format for Gemini 2.5 and Gemini 3
 *
 * Key fixes:
 * - Removed "stream", "temperature", "maxOutputTokens", "model" from root JSON
 * - Moved temperature and maxOutputTokens into "generationConfig"
 * - Model is now only in the URL path
 * - API key is in query parameter, not JSON body
 */
class GeminiClient(private val settings: GeminiSettingsState.State) {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    fun generateContent(
        conversation: GeminiConversation,
        onChunk: ((String) -> Unit)? = null
    ): String {
        val apiKey = settings.apiKey.orEmpty()
        require(apiKey.isNotBlank()) { "Gemini API key is missing" }

        val modelId = settings.model.ifBlank { GeminiModels.default().id }

        // Correct endpoint paths - add alt=sse for proper streaming format
        val path = if (settings.streaming && onChunk != null) {
            "models/$modelId:streamGenerateContent?alt=sse&key=$apiKey"
        } else {
            "models/$modelId:generateContent?key=$apiKey"
        }

        val uri = URI.create("$baseUrl/$path")
        val requestBody = buildRequestBody(conversation)

        val request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        return if (settings.streaming && onChunk != null) {
            readStreaming(request, onChunk)
        } else {
            readSingle(request)
        }
    }

    private fun readSingle(request: HttpRequest): String {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw GeminiHttpException("Gemini request failed (${response.statusCode()}): ${response.body()}")
        }
        return extractText(response.body())
    }

    private fun readStreaming(request: HttpRequest, onChunk: (String) -> Unit): String {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw GeminiHttpException("Gemini streaming failed (${response.statusCode()}): ${response.body()}")
        }

        val builder = StringBuilder()
        val responseBody = response.body()

        // Handle SSE format: each line starts with "data: " followed by JSON
        responseBody.lines().forEach { line ->
            if (line.startsWith("data:")) {
                val payload = line.removePrefix("data:").trim()
                if (payload.isBlank() || payload == "[DONE]") return@forEach

                try {
                    val chunk = extractText(payload)
                    if (chunk.isNotEmpty()) {
                        builder.append(chunk)
                        onChunk(chunk)
                    }
                } catch (e: Exception) {
                    // Skip malformed chunks
                }
            }
        }

        // If no SSE data was found, try parsing as JSON array (fallback)
        if (builder.isEmpty() && responseBody.trim().startsWith("[")) {
            try {
                val jsonArray = JSONArray(responseBody)
                for (i in 0 until jsonArray.length()) {
                    val chunk = extractText(jsonArray.getJSONObject(i).toString())
                    if (chunk.isNotEmpty()) {
                        builder.append(chunk)
                        onChunk(chunk)
                    }
                }
            } catch (e: Exception) {
                // Try single object
                val chunk = extractText(responseBody)
                if (chunk.isNotEmpty()) {
                    builder.append(chunk)
                    onChunk(chunk)
                }
            }
        }

        return builder.toString()
    }

    /**
     * Builds correct API payload for Gemini 2.5 and Gemini 3
     *
     * Correct format:
     * {
     *   "contents": [
     *     {
     *       "parts": [{ "text": "..." }]
     *     }
     *   ],
     *   "generationConfig": {
     *     "temperature": 0.7,
     *     "maxOutputTokens": 2048
     *   }
     * }
     *
     * IMPORTANT: Do NOT include "model", "stream", or config values at root level!
     */
    private fun buildRequestBody(conversation: GeminiConversation): String {
        val root = JSONObject()

        // Build contents array
        val contents = JSONArray()
        val snapshot = conversation.messages.toList()

        snapshot.forEach { message ->
            val content = JSONObject()
            // Note: Gemini API uses "user" and "model" roles, not "assistant"
            val role = if (message.role == "model") "model" else "user"
            content.put("role", role)

            val parts = JSONArray()
            parts.put(JSONObject().put("text", message.text))
            content.put("parts", parts)

            contents.put(content)
        }

        root.put("contents", contents)

        // Build generationConfig (CRITICAL: config must be nested here, not at root!)
        val generationConfig = JSONObject()
        generationConfig.put("temperature", settings.temperature)
        if (settings.maxTokens > 0) {
            generationConfig.put("maxOutputTokens", settings.maxTokens)
        }
        root.put("generationConfig", generationConfig)

        return root.toString()
    }

    private fun extractText(body: String): String {
        return try {
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates") ?: return ""
            val first = candidates.optJSONObject(0) ?: return ""
            val content = first.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            val part = parts.optJSONObject(0) ?: return ""
            part.optString("text").orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    class GeminiHttpException(message: String) : RuntimeException(message)
}
