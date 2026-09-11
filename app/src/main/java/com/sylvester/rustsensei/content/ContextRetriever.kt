package com.sylvester.rustsensei.content

/**
 * Supplies retrieval-augmented context for a chat question.
 *
 * Extracted from [RagRetriever] so the chat use case can depend on a narrow
 * interface instead of a Context-bound asset reader, following the same
 * segregation as `InferenceConfigProvider`.
 */
interface ContextRetriever {
    suspend fun retrieveContext(query: String, topK: Int = 3): String?
}
