package com.example.artsan_finder.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NidaProfile(
    val nidaNumber: String,
    val firstName: String,
    val middleName: String,
    val lastName: String,
    val gender: String,
    val dateOfBirth: String,
    val photoUrl: String,
    val nationality: String = "Tanzanian"
) {
    val fullName: String get() = "$firstName $middleName $lastName"
}
