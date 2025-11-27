package com.sallemi.gemini.actions

import com.sallemi.gemini.services.ExplainCodeIntent
import com.sallemi.gemini.services.GeminiIntent

class ExplainCodeAction : GeminiBaseEditorAction(
    "Explain Code with Gemini",
    "Explain selected code using Gemini"
) {
    override val intent: GeminiIntent = ExplainCodeIntent
}
