package com.sallemi.gemini.actions

import com.sallemi.gemini.services.GeminiIntent
import com.sallemi.gemini.services.SuggestNamesIntent

class SuggestNamesAction : GeminiBaseEditorAction(
    "Suggest Better Names",
    "Suggest better names for identifiers in selected code"
) {
    override val intent: GeminiIntent = SuggestNamesIntent
}
