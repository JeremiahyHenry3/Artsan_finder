package com.example.artsan_finder

import android.app.Application
import android.content.Context
import com.example.artsan_finder.data.local.LocalCacheManager
import com.example.artsan_finder.data.local.LocalStorageManager
import com.example.artsan_finder.data.repository.ArtisanRepository
import com.example.artsan_finder.data.repository.AuthRepository
import com.example.artsan_finder.utils.LocationProvider

class ArtisanApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}

interface AppContainer {
    val artisanRepository: ArtisanRepository
    val authRepository: AuthRepository
    val localStorageManager: LocalStorageManager
    val localCacheManager: LocalCacheManager
    val locationProvider: LocationProvider
    val nidaService: com.example.artsan_finder.data.remote.NidaVerificationService
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val localStorageManager: LocalStorageManager by lazy {
        LocalStorageManager(context)
    }

    override val localCacheManager: LocalCacheManager by lazy {
        LocalCacheManager(context)
    }

    override val locationProvider: LocationProvider by lazy {
        LocationProvider(context)
    }

    override val nidaService: com.example.artsan_finder.data.remote.NidaVerificationService by lazy {
        com.example.artsan_finder.data.remote.NidaVerificationService()
    }

    override val artisanRepository: ArtisanRepository by lazy {
        ArtisanRepository(context, locationProvider)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepository(context, localStorageManager)
    }
}
