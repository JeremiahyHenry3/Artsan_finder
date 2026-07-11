package com.example.artsan_finder.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class UserRole {
    @Json(name = "ADMIN") ADMIN,
    @Json(name = "ARTISAN") ARTISAN,
    @Json(name = "CUSTOMER") CUSTOMER
}
