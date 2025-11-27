package com.sallemi.gemini.api

/**
 * Gemini API models enum with commonly available model IDs
 *
 * Note: The settings UI now auto-loads available models from the API,
 * so this enum serves as a fallback when API is unavailable.
 *
 * See: https://ai.google.dev/gemini-api/docs/models/gemini
 */
enum class GeminiModels(val id: String, val displayName: String) {
    // Recommended stable models
    GEMINI_1_5_FLASH("gemini-1.5-flash", "Gemini 1.5 Flash (Recommended)"),
    GEMINI_1_5_PRO("gemini-1.5-pro", "Gemini 1.5 Pro"),

    // Experimental models
    GEMINI_2_0_FLASH("gemini-2.0-flash", "Gemini 2.0 Flash"),
    GEMINI_2_0_FLASH_LITE("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite"),

    // Legacy models
    GEMINI_1_0_PRO("gemini-1.0-pro", "Gemini 1.0 Pro (Legacy)"),
    GEMINI_PRO("gemini-pro", "Gemini Pro (Legacy)");

    companion object {
        fun fromId(id: String?): GeminiModels = values().firstOrNull { it.id == id } ?: default()

        // Default to stable Flash model
        fun default(): GeminiModels = GEMINI_1_5_FLASH
    }
}
