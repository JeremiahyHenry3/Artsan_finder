package com.example.artsan_finder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.artsan_finder.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE inquiryId = :inquiryId ORDER BY timestamp ASC")
    fun getMessagesForInquiry(inquiryId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE inquiryId = :inquiryId")
    suspend fun getMessageCountForInquiry(inquiryId: String): Int
}