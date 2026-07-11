package com.example.artsan_finder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A single message inside an inquiry conversation between a customer and an artisan.
 * The inquiryId groups messages into a chat thread.
 */
@JsonClass(generateAdapter = true)
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey
    @param:Json(name = "id")
    val id: String = "",
    @param:Json(name = "inquiry_id")
    val inquiryId: String = "",
    @param:Json(name = "sender_id")
    val senderId: String = "",
    @param:Json(name = "text")
    val text: String = "",
    @param:Json(name = "timestamp")
    val timestamp: Long = 0L
)