package com.sallemi.gemini.actions

import com.sallemi.gemini.services.GeminiIntent
import com.sallemi.gemini.services.RefactorIntent

class RefactorAction : GeminiBaseEditorAction(
    "Suggest Refactoring",
    "Suggest refactorings for selected code"
) {
    override val intent: GeminiIntent = RefactorIntent
}
