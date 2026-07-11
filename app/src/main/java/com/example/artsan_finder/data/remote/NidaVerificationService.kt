package com.example.artsan_finder.data.remote

import com.example.artsan_finder.data.model.NidaProfile
import kotlinx.coroutines.delay

/**
 * Mock NIDA Verification Service.
 * In a real application, this would use Retrofit to call the official NIDA Gateway or a 3rd party provider.
 */
class NidaVerificationService {

    suspend fun verifyNida(nidaNumber: String): Result<NidaProfile> {
        // Simulate network latency for a "Real" feel
        delay(2000)

        // Basic validation for NIDA format (Tanzanian NIDA is 20 digits)
        if (nidaNumber.length != 20 || !nidaNumber.all { it.isDigit() }) {
            return Result.failure(Exception("Invalid NIDA number format. Must be 20 digits."))
        }

        // Mock data based on different ID suffixes for testing
        return when {
            nidaNumber.endsWith("1") -> Result.success(
                NidaProfile(
                    nidaNumber = nidaNumber,
                    firstName = "JUMA",
                    middleName = "HASSAN",
                    lastName = "KHAMIS",
                    gender = "MALE",
                    dateOfBirth = "1990-05-12",
                    photoUrl = "https://i.pinimg.com/736x/6d/66/af/6d66af4d10a9a7d19d1df880b0ce3b23.jpg"
                )
            )
            nidaNumber.endsWith("2") -> Result.success(
                NidaProfile(
                    nidaNumber = nidaNumber,
                    firstName = "MARIAM",
                    middleName = "SAID",
                    lastName = "ABDALLAH",
                    gender = "FEMALE",
                    dateOfBirth = "1995-08-22",
                    photoUrl = "https://images.unsplash.com/photo-1544724569-5f546fd6f2b5?q=80&w=2070&auto=format&fit=crop"
                )
            )
            else -> Result.failure(Exception("No profile found for this NIDA number. Please check the number and try again."))
        }
    }
}
