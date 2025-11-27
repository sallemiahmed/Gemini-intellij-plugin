package com.sallemi.gemini.api

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Fetches available Gemini models from the API
 * Calls: GET https://generativelanguage.googleapis.com/v1beta/models
 */
class GeminiModelsListFetcher(private val apiKey: String) {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    /**
     * Data class representing a Gemini model from the API
     */
    data class ModelInfo(
        val name: String,
        val displayName: String,
        val description: String,
        val supportedMethods: List<String>,
        val inputTokenLimit: Int,
        val outputTokenLimit: Int
    )

    /**
     * Fetches the list of available models from the Gemini API
     * @return List of ModelInfo or throws exception on error
     */
    fun listModels(): List<ModelInfo> {
        require(apiKey.isNotBlank()) { "API key is required" }

        val uri = URI.create("$baseUrl/models?key=$apiKey")
        val request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw Exception("Failed to list models (${response.statusCode()}): ${response.body()}")
        }

        return parseModelsResponse(response.body())
    }

    /**
     * Parses the JSON response from the models list endpoint
     */
    private fun parseModelsResponse(jsonResponse: String): List<ModelInfo> {
        val models = mutableListOf<ModelInfo>()

        try {
            val json = JSONObject(jsonResponse)
            val modelsArray = json.optJSONArray("models") ?: return emptyList()

            for (i in 0 until modelsArray.length()) {
                val modelJson = modelsArray.getJSONObject(i)

                // Extract model name (e.g., "models/gemini-1.5-flash" -> "gemini-1.5-flash")
                val fullName = modelJson.optString("name", "")
                val modelId = fullName.substringAfter("models/")

                val displayName = modelJson.optString("displayName", modelId)
                val description = modelJson.optString("description", "")

                // Extract supported methods
                val methodsArray = modelJson.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                val supportedMethods = mutableListOf<String>()
                for (j in 0 until methodsArray.length()) {
                    supportedMethods.add(methodsArray.getString(j))
                }

                // Extract token limits
                val inputTokenLimit = modelJson.optInt("inputTokenLimit", 0)
                val outputTokenLimit = modelJson.optInt("outputTokenLimit", 0)

                models.add(
                    ModelInfo(
                        name = modelId,
                        displayName = displayName,
                        description = description,
                        supportedMethods = supportedMethods,
                        inputTokenLimit = inputTokenLimit,
                        outputTokenLimit = outputTokenLimit
                    )
                )
            }
        } catch (e: Exception) {
            throw Exception("Failed to parse models response: ${e.message}", e)
        }

        return models
    }

    /**
     * Formats model info as human-readable string
     */
    fun formatModelInfo(model: ModelInfo): String {
        return buildString {
            appendLine("Model: ${model.name}")
            appendLine("Display Name: ${model.displayName}")
            if (model.description.isNotBlank()) {
                appendLine("Description: ${model.description}")
            }
            appendLine("Supported Methods: ${model.supportedMethods.joinToString(", ")}")
            appendLine("Input Token Limit: ${model.inputTokenLimit}")
            appendLine("Output Token Limit: ${model.outputTokenLimit}")
        }
    }
}
