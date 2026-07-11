package com.example.artsan_finder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.artsan_finder.data.model.Artisan
import com.example.artsan_finder.data.model.Category
import com.example.artsan_finder.data.model.Inquiry
import com.example.artsan_finder.data.model.Service
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtisanDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Query("SELECT * FROM artisans")
    fun getAllArtisans(): Flow<List<Artisan>>

    @Query("SELECT * FROM artisans WHERE categoryId = :categoryId")
    fun getArtisansByCategory(categoryId: String): Flow<List<Artisan>>

    @Query("SELECT * FROM artisans WHERE id = :artisanId")
    suspend fun getArtisanById(artisanId: String): Artisan?

    @Query("SELECT * FROM artisans WHERE id = :artisanId")
    fun observeArtisanById(artisanId: String): Flow<Artisan?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtisans(artisans: List<Artisan>)

    @Query("SELECT * FROM services WHERE artisanId = :artisanId")
    fun getServicesForArtisan(artisanId: String): Flow<List<Service>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<Service>)

    @Query("SELECT * FROM inquiries WHERE userId = :userId")
    fun getInquiriesForUser(userId: String): Flow<List<Inquiry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInquiry(inquiry: Inquiry)

    @Query("SELECT * FROM inquiries WHERE artisanId = :artisanId")
    fun getInquiriesForArtisan(artisanId: String): Flow<List<Inquiry>>

    @Query("UPDATE inquiries SET status = :status WHERE id = :inquiryId")
    suspend fun updateInquiryStatus(inquiryId: String, status: String)

    @Query("SELECT * FROM inquiries WHERE id = :inquiryId")
    suspend fun getInquiryById(inquiryId: String): Inquiry?

    @Query("UPDATE artisans SET verified = :isVerified WHERE id = :artisanId")
    suspend fun verifyArtisan(artisanId: String, isVerified: Boolean)

    @Query("DELETE FROM artisans WHERE id = :artisanId")
    suspend fun deleteArtisanById(artisanId: String)

    @Query("DELETE FROM services WHERE artisanId = :artisanId")
    suspend fun deleteServicesForArtisan(artisanId: String)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getTotalUsers(): Int

    @Query("SELECT COUNT(*) FROM artisans WHERE verified = 0")
    suspend fun getPendingArtisansCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: Service)

    @Query("SELECT COUNT(*) FROM inquiries")
    suspend fun getTotalInquiriesCount(): Int

    @Query("SELECT COUNT(*) FROM services")
    suspend fun getTotalServicesCount(): Int

    @Query("DELETE FROM artisans")
    suspend fun deleteAllArtisans()
}
