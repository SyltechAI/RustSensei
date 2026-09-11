package com.sylvester.rustsensei.testdoubles

import com.sylvester.rustsensei.data.ChatDao
import com.sylvester.rustsensei.data.ChatMessage
import com.sylvester.rustsensei.data.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [ChatDao]. Set [failOnInsertMessage] to simulate the storage
 * failures (disk full, corrupt database) that used to escape the chat flow.
 */
class FakeChatDao : ChatDao {

    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private var nextMessageId = 1L
    private var nextConversationId = 1L

    var failOnInsertMessage: Throwable? = null
    var failOnGetMessages: Throwable? = null

    override suspend fun insertMessage(message: ChatMessage): Long {
        failOnInsertMessage?.let { throw it }
        val id = nextMessageId++
        messages.value = messages.value + message.copy(id = id)
        return id
    }

    override suspend fun insertConversation(conversation: Conversation): Long {
        val id = nextConversationId++
        conversations.value = conversations.value + conversation.copy(id = id)
        return id
    }

    override suspend fun updateConversation(conversation: Conversation) {
        conversations.value = conversations.value.map {
            if (it.id == conversation.id) conversation else it
        }
    }

    override fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessage>> =
        messages.map { all -> all.filter { it.conversationId == conversationId } }

    override suspend fun getMessagesForConversationOnce(conversationId: Long): List<ChatMessage> {
        failOnGetMessages?.let { throw it }
        return messages.value.filter { it.conversationId == conversationId }
    }

    override suspend fun getConversation(conversationId: Long): Conversation? =
        conversations.value.firstOrNull { it.id == conversationId }

    override fun getAllConversations(): Flow<List<Conversation>> = conversations

    override suspend fun deleteMessagesForConversation(conversationId: Long) {
        messages.value = messages.value.filterNot { it.conversationId == conversationId }
    }

    override suspend fun deleteConversation(conversationId: Long) {
        conversations.value = conversations.value.filterNot { it.id == conversationId }
    }

    override suspend fun deleteAllMessages() {
        messages.value = emptyList()
    }

    override suspend fun deleteAllConversations() {
        conversations.value = emptyList()
    }

    fun allMessages(): List<ChatMessage> = messages.value
}
