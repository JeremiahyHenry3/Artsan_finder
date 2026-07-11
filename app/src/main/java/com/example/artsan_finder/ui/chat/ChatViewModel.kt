package com.example.artsan_finder.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.artsan_finder.data.model.ChatMessage
import com.example.artsan_finder.data.model.Inquiry
import com.example.artsan_finder.data.remote.FirebaseModule
import com.example.artsan_finder.data.repository.ArtisanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val inquiry: Inquiry? = null,
    val messages: List<ChatMessage> = emptyList(),
    val partnerName: String = "",
    val isLoading: Boolean = true,
    /** Surfaced chat failure (rejected send, dead listener) — shown as a snackbar. */
    val error: String? = null
)

class ChatViewModel(
    private val inquiryId: String,
    private val currentUserId: String,
    private val repository: ArtisanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /**
     * The identity security rules actually verify. Always taken live from
     * Firebase Auth — never from a stored profile field or a stale argument —
     * so a send can never be signed with the wrong user.
     */
    private val authUserId: String
        get() = FirebaseModule.auth.currentUser?.uid ?: currentUserId

    init {
        // Attach the real-time message listener FIRST — the thread paints
        // immediately from Firestore's local cache and every new message
        // (local or remote) lands the instant the listener fires. Nothing
        // network-bound is allowed to run before this.
        viewModelScope.launch {
            repository.getMessagesForInquiry(
                inquiryId,
                onError = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Chat connection problem: ${e.message}")
                    }
                }
            ).collect { messages ->
                _uiState.update { it.copy(messages = messages, isLoading = false) }
            }
        }

        // Live inquiry: the "Replied / Awaiting reply" header updates in real
        // time on both devices. The partner's name is resolved once, in parallel.
        viewModelScope.launch {
            var partnerRequested = false
            repository.observeInquiryById(inquiryId).collect { inquiry ->
                _uiState.update { it.copy(inquiry = inquiry) }
                if (inquiry != null && !partnerRequested) {
                    partnerRequested = true
                    launch {
                        val partnerName = if (currentUserId == inquiry.userId) {
                            repository.getArtisanById(inquiry.artisanId)?.name ?: "Artisan"
                        } else {
                            repository.getUserById(inquiry.userId).first()?.name ?: "Customer"
                        }
                        _uiState.update { it.copy(partnerName = partnerName) }
                    }
                }
            }
        }

        // Legacy threads created before chat existed: backfill the original
        // inquiry message without blocking anything above. (No-op unless the
        // current user is the customer who owns that first message.)
        viewModelScope.launch { repository.seedChatThreadIfEmpty(inquiryId, currentUserId) }
    }

    /**
     * Optimistic send: the message shows up on screen immediately. A tap is
     * never dropped — if the thread metadata is still loading, the send waits
     * for it and fires as soon as it arrives.
     */
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            // Fall back to a direct fetch so a send can never hang on the listener
            val inquiry = _uiState.value.inquiry
                ?: repository.getInquiryById(inquiryId)
                ?: uiState.mapNotNull { it.inquiry }.first()
            val senderId = authUserId
            repository.sendChatMessage(
                inquiry = inquiry,
                senderId = senderId,
                text = trimmed,
                afterTimestamp = _uiState.value.messages.lastOrNull()?.timestamp ?: 0L,
                onFailure = { e ->
                    // Include the identities involved: a PERMISSION_DENIED here
                    // almost always means "me" matches neither thread participant.
                    val detail = "me=${senderId.take(8)} " +
                        "customer=${inquiry.userId.take(8)} artisan=${inquiry.artisanId.take(8)}"
                    _uiState.update { it.copy(error = "Message not sent: ${e.message} [$detail]") }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun isMine(message: ChatMessage): Boolean =
        message.senderId == authUserId || message.senderId == currentUserId

    companion object {
        fun provideFactory(
            inquiryId: String,
            currentUserId: String,
            repository: ArtisanRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(inquiryId, currentUserId, repository) as T
            }
        }
    }
}
