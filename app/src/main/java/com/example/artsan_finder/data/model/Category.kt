package com.example.artsan_finder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    @param:Json(name = "id")
    val id: String = "",
    @param:Json(name = "name")
    val name: String = "",
    @param:Json(name = "icon_url")
    val iconUrl: String = ""
)
