package com.sallemi.gemini.actions

import com.sallemi.gemini.services.FixCodeIntent
import com.sallemi.gemini.services.GeminiIntent

class FixCodeAction : GeminiBaseEditorAction(
    "Gemini Fix Code",
    "Find and fix issues in the current file with Gemini"
) {
    override val intent: GeminiIntent = FixCodeIntent
}
