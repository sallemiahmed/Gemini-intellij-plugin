package com.sallemi.gemini.actions

import com.sallemi.gemini.services.GeminiIntent
import com.sallemi.gemini.services.GenerateTestsIntent

class GenerateTestsAction : GeminiBaseEditorAction(
    "Generate Unit Tests",
    "Generate unit tests for selected code"
) {
    override val intent: GeminiIntent = GenerateTestsIntent
}
