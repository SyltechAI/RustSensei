package com.sylvester.rustsensei.domain

import app.cash.turbine.test
import com.sylvester.rustsensei.data.ChatRepository
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.testdoubles.FakeChatDao
import com.sylvester.rustsensei.testdoubles.FakeContextRetriever
import com.sylvester.rustsensei.testdoubles.FakeInferenceEngine
import com.sylvester.rustsensei.testdoubles.FakeModelLifecycle
import com.sylvester.rustsensei.viewmodel.ChatContext
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * The storage and retrieval steps of the chat pipeline used to run outside any
 * try/catch. The collector lives in viewModelScope, so a throw there took the
 * process down instead of showing an error. These cover that boundary.
 */
class SendChatMessageUseCaseTest {

    private lateinit var dao: FakeChatDao
    private lateinit var repository: ChatRepository
    private lateinit var retriever: FakeContextRetriever
    private lateinit var engine: FakeInferenceEngine
    private lateinit var lifecycle: FakeModelLifecycle
    private lateinit var useCase: SendChatMessageUseCase

    private val config = InferenceConfig()

    @Before
    fun setUp() {
        dao = FakeChatDao()
        repository = ChatRepository(dao)
        retriever = FakeContextRetriever()
        engine = FakeInferenceEngine()
        lifecycle = FakeModelLifecycle()
        useCase = SendChatMessageUseCase(repository, retriever, engine, lifecycle)
    }

    @Test
    fun `happy path streams tokens then completes and persists the reply`() = runTest {
        engine.tokensToEmit = listOf("Own", "ership")

        useCase(1L, "what is ownership", ChatContext.General, config).test {
            assertEquals("Own", (awaitItem() as ChatStreamEvent.Token).displayText)
            assertEquals("Ownership", (awaitItem() as ChatStreamEvent.Token).displayText)
            val done = awaitItem() as ChatStreamEvent.Completed
            assertEquals("Ownership", done.fullText)
            awaitComplete()
        }

        val roles = dao.allMessages().map { it.role }
        assertEquals(listOf("user", "assistant"), roles)
    }

    @Test
    fun `storage failure while saving the question emits Error instead of throwing`() = runTest {
        dao.failOnInsertMessage = IOException("ENOSPC: No space left on device")

        useCase(1L, "what is ownership", ChatContext.General, config).test {
            val error = awaitItem() as ChatStreamEvent.Error
            assertTrue(error.message.contains("Could not prepare your message"))
            awaitComplete()
        }

        assertEquals(0, engine.generateCallCount)
    }

    @Test
    fun `history read failure emits Error instead of throwing`() = runTest {
        dao.failOnGetMessages = IllegalStateException("database disk image is malformed")

        useCase(1L, "what is ownership", ChatContext.General, config).test {
            val error = awaitItem() as ChatStreamEvent.Error
            assertTrue(error.message.contains("Could not prepare your message"))
            awaitComplete()
        }

        assertEquals(0, engine.generateCallCount)
    }

    @Test
    fun `context retrieval failure emits Error instead of throwing`() = runTest {
        retriever.failWith = RuntimeException("malformed rag/chunks.json")

        useCase(1L, "what is ownership", ChatContext.General, config).test {
            val error = awaitItem() as ChatStreamEvent.Error
            assertTrue(error.message.contains("Could not prepare your message"))
            awaitComplete()
        }

        assertEquals(0, engine.generateCallCount)
    }

    @Test
    fun `generation failure emits Error and does not write an assistant turn`() = runTest {
        engine.shouldFailGenerate = true

        useCase(1L, "what is ownership", ChatContext.General, config).test {
            val error = awaitItem() as ChatStreamEvent.Error
            assertEquals("Fake generation error", error.message)
            awaitComplete()
        }

        // Previously this path wrote a literal "Error: ..." turn into history.
        assertEquals(listOf("user"), dao.allMessages().map { it.role })
    }

    @Test
    fun `unavailable model reports a download hint without touching storage`() = runTest {
        lifecycle.ensureLoadedResult = false

        useCase(1L, "what is ownership", ChatContext.General, config).test {
            val error = awaitItem() as ChatStreamEvent.Error
            assertTrue(error.message.contains("Download from Settings"))
            awaitComplete()
        }

        assertTrue(dao.allMessages().isEmpty())
    }
}
