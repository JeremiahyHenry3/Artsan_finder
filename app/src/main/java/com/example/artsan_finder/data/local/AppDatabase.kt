package com.example.artsan_finder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.artsan_finder.data.model.Artisan
import com.example.artsan_finder.data.model.Category
import com.example.artsan_finder.data.model.ChatMessage
import com.example.artsan_finder.data.model.Inquiry
import com.example.artsan_finder.data.model.Service
import com.example.artsan_finder.data.model.User

@Database(
    entities = [Artisan::class, Category::class, Service::class, Inquiry::class, User::class, ChatMessage::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artisanDao(): ArtisanDao
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "artisan_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
