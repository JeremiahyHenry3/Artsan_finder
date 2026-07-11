package com.example.artsan_finder.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseModule {
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /**
     * A secondary Firebase app used to CREATE accounts (customer registration,
     * admin registering artisans) without signing out whoever is currently logged in.
     */
    private fun secondaryApp(context: Context): FirebaseApp {
        val appName = "account-creator"
        return try {
            FirebaseApp.getInstance(appName)
        } catch (_: IllegalStateException) {
            FirebaseApp.initializeApp(
                context.applicationContext,
                FirebaseApp.getInstance().options,
                appName
            )
        }
    }

    fun secondaryAuth(context: Context): FirebaseAuth =
        FirebaseAuth.getInstance(secondaryApp(context))

    /**
     * Firestore bound to the secondary app: writes are authenticated as the account
     * that was just created, which security rules require for self-registration.
     */
    fun secondaryFirestore(context: Context): FirebaseFirestore =
        FirebaseFirestore.getInstance(secondaryApp(context))
}
