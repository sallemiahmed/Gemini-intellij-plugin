package com.sallemi.gemini.actions

import com.sallemi.gemini.services.DocumentationIntent
import com.sallemi.gemini.services.GeminiIntent

class GenerateDocsAction : GeminiBaseEditorAction(
    "Generate Documentation",
    "Generate documentation for selected code with Gemini"
) {
    override val intent: GeminiIntent = DocumentationIntent
}
