package com.example.artsan_finder.data.remote

import com.example.artsan_finder.data.model.Artisan
import com.example.artsan_finder.data.model.Category
import com.example.artsan_finder.data.model.Inquiry
import com.example.artsan_finder.data.model.Service
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ArtisanApiService {
    @GET("categories")
    suspend fun getCategories(): List<Category>

    @GET("artisans")
    suspend fun getArtisans(@Query("category_id") categoryId: String? = null): List<Artisan>

    @GET("artisans/{id}")
    suspend fun getArtisanDetails(@Path("id") artisanId: String): Artisan

    @GET("artisans/{id}/services")
    suspend fun getArtisanServices(@Path("id") artisanId: String): List<Service>

    @POST("inquiries")
    suspend fun sendInquiry(@Body inquiry: Inquiry): Inquiry

    @GET("users/{userId}/inquiries")
    suspend fun getUserInquiries(@Path("userId") userId: String): List<Inquiry>
}
