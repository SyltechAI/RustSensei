package com.sylvester.rustsensei.testdoubles

import com.sylvester.rustsensei.content.ContextRetriever

/** Returns a canned RAG snippet, or throws to simulate a bad bundled asset. */
class FakeContextRetriever(
    private var result: String? = null
) : ContextRetriever {

    var failWith: Throwable? = null

    override suspend fun retrieveContext(query: String, topK: Int): String? {
        failWith?.let { throw it }
        return result
    }
}
