package com.example.artsan_finder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "artisans")
data class Artisan(
    @PrimaryKey
    @param:Json(name = "id")
    val id: String = "",
    @param:Json(name = "name")
    val name: String = "",
    @param:Json(name = "category_id")
    val categoryId: String = "",
    @param:Json(name = "rating")
    val rating: Float = 0f,
    @param:Json(name = "latitude")
    val latitude: Double = 0.0,
    @param:Json(name = "longitude")
    val longitude: Double = 0.0,
    @param:Json(name = "phone_number")
    val phoneNumber: String = "",
    @param:Json(name = "email")
    val email: String = "",
    @param:Json(name = "bio")
    val bio: String = "",
    @param:Json(name = "image_url")
    val imageUrl: String = "",
    // Named "verified" (not "isVerified") so the property round-trips with the
    // Firestore field of the same name — Firestore strips the "is" prefix when
    // writing, which silently breaks reads for Kotlin "isX" boolean properties.
    @param:Json(name = "is_verified")
    val verified: Boolean = false,
    @param:Json(name = "region")
    val region: String = "",
    @param:Json(name = "ward")
    val ward: String = "",
    @param:Json(name = "street")
    val street: String = ""
)
