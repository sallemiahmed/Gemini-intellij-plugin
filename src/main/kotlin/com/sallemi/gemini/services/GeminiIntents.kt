package com.sallemi.gemini.services

sealed class GeminiIntent(
    val title: String,
    private val promptTemplate: String
) {
    open fun buildPrompt(content: String, context: String? = null): String =
        listOf(promptTemplate.trim(), content.trim(), context.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
}

object CompleteCodeIntent : GeminiIntent(
    "Complete Code",
    "Complete the following code. Provide only the completion and short reasoning."
)

object ExplainCodeIntent : GeminiIntent(
    "Explain Code",
    "Explain what this code does, its complexity, and any surprising behavior."
)

object CommitMessageIntent : GeminiIntent(
    "Generate Commit Message",
    "Generate a concise, clear git commit message for the following diff or changes."
)

object RuntimeErrorIntent : GeminiIntent(
    "Explain Runtime Error",
    "Explain the runtime error and how to fix it."
)

object DocumentationIntent : GeminiIntent(
    "Generate Documentation",
    "Generate documentation and comments for the following code."
)

object SuggestNamesIntent : GeminiIntent(
    "Suggest Names",
    "Suggest clearer, more descriptive names for identifiers and APIs in the snippet."
)

object RefactorIntent : GeminiIntent(
    "Suggest Refactoring",
    "Suggest refactoring ideas, simplifications, and improvements while keeping behavior."
)

object BugFinderIntent : GeminiIntent(
    "Find Bugs",
    "Find potential bugs, edge cases, and provide fixes for the following code."
)

object FixCodeIntent : GeminiIntent(
    "Fix Code",
    "Fix defects, suggest safe improvements, and return the corrected code."
)

object GenerateTestsIntent : GeminiIntent(
    "Generate Tests",
    "Write concise unit tests that cover critical paths and edge cases."
)

object ConvertLanguageIntent : GeminiIntent(
    "Convert Language",
    "Convert this code to another language. Keep comments explaining important changes."
)

object VcsQuestionIntent : GeminiIntent(
    "VCS Help",
    "Answer questions about version control usage and best practices related to this context."
)

object ExplainCommitIntent : GeminiIntent(
    "Explain Commits",
    "Explain the intent of these commits and how they impact the codebase."
)

object ProgrammingQuestionIntent : GeminiIntent(
    "Programming Q&A",
    "Answer the following programming question or request with clear steps and examples."
)
