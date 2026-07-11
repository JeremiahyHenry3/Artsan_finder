package com.example.artsan_finder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "inquiries")
data class Inquiry(
    @PrimaryKey
    @param:Json(name = "id")
    val id: String = "",
    @param:Json(name = "user_id")
    val userId: String = "",
    @param:Json(name = "artisan_id")
    val artisanId: String = "",
    @param:Json(name = "message")
    val message: String = "",
    @param:Json(name = "status")
    val status: String = "pending", // e.g., "pending", "replied"
    @param:Json(name = "created_at")
    val createdAt: Long = 0L,
    // Updated on every chat message so conversation lists stay sorted by activity
    @param:Json(name = "last_activity_at")
    val lastActivityAt: Long = 0L,
    @param:Json(name = "last_message")
    val lastMessage: String = ""
)
