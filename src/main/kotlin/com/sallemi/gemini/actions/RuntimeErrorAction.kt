package com.sallemi.gemini.actions

import com.sallemi.gemini.services.GeminiIntent
import com.sallemi.gemini.services.RuntimeErrorIntent

class RuntimeErrorAction : GeminiBaseEditorAction(
    "Explain Runtime Error",
    "Explain runtime errors using Gemini"
) {
    override val intent: GeminiIntent = RuntimeErrorIntent
}
