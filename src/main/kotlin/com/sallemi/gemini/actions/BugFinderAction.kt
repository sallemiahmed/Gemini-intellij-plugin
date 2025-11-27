package com.sallemi.gemini.actions

import com.sallemi.gemini.services.BugFinderIntent
import com.sallemi.gemini.services.GeminiIntent

class BugFinderAction : GeminiBaseEditorAction(
    "Find Bugs",
    "Find bugs and issues in selected code with Gemini"
) {
    override val intent: GeminiIntent = BugFinderIntent
}
