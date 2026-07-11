package com.example.artsan_finder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "services")
data class Service(
    @PrimaryKey
    @param:Json(name = "id")
    val id: String = "",
    @param:Json(name = "artisan_id")
    val artisanId: String = "",
    @param:Json(name = "name")
    val name: String = "",
    @param:Json(name = "price")
    val price: Double = 0.0
)
