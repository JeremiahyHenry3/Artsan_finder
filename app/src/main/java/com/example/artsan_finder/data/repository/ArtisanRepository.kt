package com.example.artsan_finder.data.repository

import android.content.Context
import android.util.Log
import com.example.artsan_finder.data.model.*
import com.example.artsan_finder.data.remote.FirebaseModule
import com.example.artsan_finder.utils.LocationProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Cloud data layer backed by Firebase Firestore with real-time snapshot listeners.
 * All data lives in the cloud; Firestore's built-in offline cache keeps the app
 * usable on poor networks. Works across any device, anywhere in the world.
 *
 * Note: the Artisan model uses the property name `verified` so it round-trips
 * with the Firestore field of the same name.
 */
class ArtisanRepository(
    private val context: Context,
    private val locationProvider: LocationProvider
) {
    private val db get() = FirebaseModule.firestore
    private val artisansCol get() = db.collection("artisans")
    private val categoriesCol get() = db.collection("categories")
    private val servicesCol get() = db.collection("services")
    private val inquiriesCol get() = db.collection("inquiries")
    private val messagesCol get() = db.collection("chat_messages")
    private val usersCol get() = db.collection("users")

    // ---------- Generic real-time helpers ----------

    private fun <T> queryFlow(query: Query, mapper: (DocumentSnapshot) -> T?): Flow<List<T>> =
        callbackFlow {
            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull(mapper) ?: emptyList())
            }
            awaitClose { registration.remove() }
        }

    // ---------- Categories & Artisans ----------

    val categories: Flow<List<Category>> =
        queryFlow(categoriesCol) { it.toObject(Category::class.java) }.onEach {
            if (it.isEmpty()) seedInitialData()
        }

    val artisans: Flow<List<Artisan>> =
        queryFlow(artisansCol) { it.toObject(Artisan::class.java) }

    fun getArtisansByCategory(categoryId: String): Flow<List<Artisan>> =
        queryFlow(artisansCol.whereEqualTo("categoryId", categoryId)) { it.toObject(Artisan::class.java) }

    suspend fun getArtisanById(artisanId: String): Artisan? {
        return try {
            artisansCol.document(artisanId).get().await().toObject(Artisan::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun observeArtisanById(artisanId: String): Flow<Artisan?> = callbackFlow {
        val registration = artisansCol.document(artisanId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(Artisan::class.java))
            }
        awaitClose { registration.remove() }
    }

    suspend fun refreshCategories() {
        try {
            val snapshot = categoriesCol.limit(1).get().await()
            if (snapshot.isEmpty) seedInitialData()
        } catch (_: Exception) {
            // Offline — Firestore cache serves existing data
        }
    }

    suspend fun refreshArtisans(categoryId: String? = null) {
        // Firestore listeners are already real-time; nothing to refresh manually.
    }

    // ---------- Services ----------

    fun getServicesForArtisan(artisanId: String): Flow<List<Service>> =
        queryFlow(servicesCol.whereEqualTo("artisanId", artisanId)) { it.toObject(Service::class.java) }

    suspend fun refreshServices(artisanId: String) {
        // Firestore listeners are already real-time; nothing to refresh manually.
    }

    suspend fun addService(service: Service) {
        try {
            servicesCol.document(service.id).set(service).await()
        } catch (_: Exception) {
            // Queued by Firestore offline persistence
        }
    }

    /**
     * Overwrites a service in place (same doc id). Every screen showing services
     * observes a snapshot listener, so the edit propagates live to the artisan's
     * dashboard and every customer currently viewing the profile.
     */
    suspend fun updateService(service: Service) {
        try {
            servicesCol.document(service.id).set(service).await()
        } catch (_: Exception) {
            // Queued by Firestore offline persistence
        }
    }

    suspend fun deleteService(serviceId: String) {
        try {
            servicesCol.document(serviceId).delete().await()
        } catch (_: Exception) {
            // Queued by Firestore offline persistence
        }
    }

    // ---------- Inquiries ----------

    fun getInquiriesForUser(userId: String): Flow<List<Inquiry>> =
        queryFlow(inquiriesCol.whereEqualTo("userId", userId)) { it.toObject(Inquiry::class.java) }
            .map { list -> list.sortedByDescending { maxOf(it.lastActivityAt, it.createdAt) } }

    fun getInquiriesForArtisan(artisanId: String): Flow<List<Inquiry>> =
        queryFlow(inquiriesCol.whereEqualTo("artisanId", artisanId)) { it.toObject(Inquiry::class.java) }
            .map { list -> list.sortedByDescending { maxOf(it.lastActivityAt, it.createdAt) } }

    /**
     * Creates the inquiry and its first chat message.
     *
     * Two hard guarantees, both learned the painful way:
     * 1. The recipient must be REAL — the artisan doc is verified to exist
     *    before anything is written. A phantom recipient (stale sample id,
     *    artisan deleted/re-keyed by the admin) produced threads only the
     *    customer's local cache could see: the artisan never received them
     *    and every later send died with PERMISSION_DENIED.
     * 2. The sender identity is taken live from Firebase Auth, never from a
     *    UI-supplied id — security rules reject inquiries created in someone
     *    else's name, and a stale "guest" id used to fail silently here.
     *
     * The writes themselves are still issued without awaiting server acks:
     * latency compensation shows them instantly, the offline queue delivers.
     * (The inquiry write must be issued before the message write — the
     * published security rules verify a message against its parent inquiry.)
     */
    suspend fun sendInquiry(inquiry: Inquiry): Result<Unit> {
        val authUid = FirebaseModule.auth.currentUser?.uid
            ?: return Result.failure(Exception("You must be signed in to send an inquiry"))
        val artisanExists = try {
            artisansCol.document(inquiry.artisanId).get().await().exists()
        } catch (_: Exception) {
            false
        }
        if (!artisanExists) {
            return Result.failure(
                Exception("This artisan is no longer available. Please pick an artisan from Discovery.")
            )
        }

        val now = System.currentTimeMillis()
        val stamped = inquiry.copy(
            userId = authUid,
            createdAt = now,
            lastActivityAt = now,
            lastMessage = inquiry.message
        )
        inquiriesCol.document(stamped.id).set(stamped)
            .addOnFailureListener { e ->
                Log.e(TAG, "Inquiry rejected (artisan=${stamped.artisanId}, user=$authUid)", e)
            }
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            inquiryId = stamped.id,
            senderId = authUid,
            text = stamped.message,
            timestamp = now
        )
        messagesCol.document(message.id).set(message)
            .addOnFailureListener { e ->
                Log.e(TAG, "Inquiry first message rejected (inquiry=${stamped.id})", e)
            }
        return Result.success(Unit)
    }

    suspend fun refreshInquiries(userId: String) {
        // Firestore listeners are already real-time; nothing to refresh manually.
    }

    // ---------- Chat ----------

    suspend fun getInquiryById(inquiryId: String): Inquiry? {
        return try {
            inquiriesCol.document(inquiryId).get().await().toObject(Inquiry::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Live inquiry document — keeps chat headers and status chips current in
     * real time. Re-attaches itself if the listener errors, because sending is
     * blocked until the inquiry is known: a silently dead listener here would
     * make the chat unable to send forever.
     */
    fun observeInquiryById(inquiryId: String): Flow<Inquiry?> = callbackFlow {
        var registration: ListenerRegistration? = null
        fun attach() {
            registration = inquiriesCol.document(inquiryId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Inquiry listener failed for $inquiryId", error)
                        launch { delay(2_000); attach() }
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.toObject(Inquiry::class.java))
                }
        }
        attach()
        awaitClose { registration?.remove() }
    }

    /**
     * Real-time message stream for an inquiry thread, sorted oldest → newest.
     * Firestore permanently kills a listener that errors (e.g. a transient
     * permission/network failure during auth refresh) — this stream reports
     * the error via [onError] and re-attaches itself so the chat self-heals
     * instead of silently freezing.
     */
    fun getMessagesForInquiry(
        inquiryId: String,
        onError: (Exception) -> Unit = {}
    ): Flow<List<ChatMessage>> = callbackFlow {
        var registration: ListenerRegistration? = null
        fun attach() {
            registration = messagesCol.whereEqualTo("inquiryId", inquiryId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Message listener failed for inquiry $inquiryId", error)
                        onError(error)
                        launch { delay(2_000); attach() }
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    trySend(
                        snapshot.documents
                            .mapNotNull { it.toObject(ChatMessage::class.java) }
                            .sortedBy { it.timestamp }
                    )
                }
        }
        attach()
        awaitClose { registration?.remove() }
    }

    /**
     * Sends a chat message and syncs the inquiry metadata (status, last message,
     * last activity) as TWO INDEPENDENT writes — deliberately not a batch. In an
     * atomic batch a rejected metadata update takes the message down with it;
     * decoupled, the message delivers as long as the sender is a participant,
     * and a refused metadata update merely means the list preview lags (it
     * re-syncs on the next successful send).
     *
     * Neither write waits for the server: latency compensation shows the message
     * on the sender's screen instantly, the other participant receives it the
     * moment the server accepts it, and the offline queue guarantees delivery.
     *
     * [afterTimestamp] is the newest timestamp already visible in the thread:
     * the new message is stamped strictly after it, so a reply can never sort
     * above the message it answers even when the two devices' clocks disagree.
     *
     * [onFailure] fires only if the MESSAGE itself is rejected (e.g. security
     * rules) — a rejected message silently disappears from the sender's screen,
     * so the caller must tell the user instead of letting it vanish.
     */
    fun sendChatMessage(
        inquiry: Inquiry,
        senderId: String,
        text: String,
        afterTimestamp: Long = 0L,
        onFailure: (Exception) -> Unit = {}
    ) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            inquiryId = inquiry.id,
            senderId = senderId,
            text = text,
            timestamp = maxOf(System.currentTimeMillis(), afterTimestamp + 1)
        )
        messagesCol.document(message.id).set(message)
            .addOnFailureListener { e ->
                Log.e(TAG, "Chat message rejected (inquiry=${inquiry.id}, sender=$senderId)", e)
                onFailure(e)
            }

        val newStatus = if (senderId == inquiry.userId) "pending" else "replied"
        inquiriesCol.document(inquiry.id).update(
            mapOf(
                "status" to newStatus,
                "lastMessage" to text,
                "lastActivityAt" to message.timestamp
            )
        ).addOnFailureListener { e ->
            // Best-effort: never surfaced to the user, never blocks the message
            Log.w(TAG, "Inquiry metadata sync refused (inquiry=${inquiry.id}, sender=$senderId)", e)
        }
    }

    /**
     * Ensures an inquiry's chat thread contains at least the original inquiry
     * message. Only the CUSTOMER may run this: the seed message is written in
     * their name, and security rules reject messages written for someone else —
     * running it as the artisan would only produce a doomed queued write.
     */
    suspend fun seedChatThreadIfEmpty(inquiryId: String, currentUserId: String) {
        try {
            val inquiry = getInquiryById(inquiryId) ?: return
            if (inquiry.userId != currentUserId) return
            val existing = messagesCol.whereEqualTo("inquiryId", inquiryId).limit(1).get().await()
            if (existing.isEmpty) {
                val message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    inquiryId = inquiryId,
                    senderId = inquiry.userId,
                    text = inquiry.message,
                    timestamp = System.currentTimeMillis()
                )
                messagesCol.document(message.id).set(message).await()
            }
        } catch (_: Exception) {
            // Non-critical
        }
    }

    // ---------- Admin ----------

    /**
     * Live dashboard counters derived from snapshot listeners: always current,
     * update in real time on every change, and are served from the offline
     * cache on poor networks. (Server-side count() aggregations cannot use the
     * cache — they failed silently offline and left the dashboard at zero.)
     */
    val adminStats: Flow<Map<String, Int>> = combine(
        artisans,
        queryFlow(usersCol.whereEqualTo("role", "CUSTOMER")) { it.id },
        queryFlow(usersCol.whereEqualTo("role", "ADMIN")) { it.id },
        queryFlow(inquiriesCol) { it.id },
        queryFlow(servicesCol) { it.id }
    ) { allArtisans, customers, admins, inquiries, services ->
        mapOf(
            // Everyone present in the app: customers + admins + every artisan
            "total_users" to customers.size + admins.size + allArtisans.size,
            "pending_artisans" to allArtisans.count { !it.verified },
            "total_artisans" to allArtisans.size,
            "total_customers" to customers.size,
            "total_inquiries" to inquiries.size,
            "total_services" to services.size
        )
    }

    fun getAllCustomers(): Flow<List<User>> =
        queryFlow(usersCol.whereEqualTo("role", "CUSTOMER")) { it.toObject(User::class.java) }

    fun getAllArtisans(): Flow<List<Artisan>> = artisans

    fun getUserById(userId: String): Flow<User?> = callbackFlow {
        val registration = usersCol.document(userId)
            .addSnapshotListener { snapshot, _ ->
                // Doc key is the authoritative id (= auth uid)
                trySend(snapshot?.toObject(User::class.java)?.copy(id = snapshot.id))
            }
        awaitClose { registration.remove() }
    }

    suspend fun updateUserInfo(user: User): Result<Unit> {
        return try {
            // Credentials live only in Firebase Auth — profiles never contain passwords
            usersCol.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyArtisan(artisanId: String, isApproved: Boolean) {
        try {
            artisansCol.document(artisanId).update("verified", isApproved).await()
        } catch (_: Exception) {
            // Queued by Firestore offline persistence
        }
    }

    /**
     * Updates an artisan's profile and keeps their user profile name in sync.
     * If a new region/ward is provided, the address is geocoded and coordinates update automatically.
     * Login credentials live in Firebase Auth: password changes are done via reset email.
     */
    suspend fun updateArtisanProfile(
        artisan: Artisan,
        region: String? = null,
        ward: String? = null,
        street: String? = null
    ): Result<Unit> {
        return try {
            var updated = artisan

            if (!region.isNullOrBlank() && !ward.isNullOrBlank()) {
                val coords = geocodeAddress(region, ward, street)
                if (coords != null) {
                    updated = updated.copy(
                        latitude = coords.first,
                        longitude = coords.second,
                        region = region,
                        ward = ward,
                        street = street.orEmpty()
                    )
                }
            }

            artisansCol.document(updated.id).set(updated).await()

            // Keep the user profile display name in sync (login email is owned by Firebase Auth)
            try {
                usersCol.document(updated.id).update("name", updated.name).await()
            } catch (_: Exception) {
                // Seeded demo artisans have no login account — fine
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gives seeded demo artisans (legacy "art_*" doc ids) a real Firebase Auth
     * login with [DEMO_ARTISAN_PASSWORD], then re-keys their artisan doc, user
     * profile and services to the new auth uid. Idempotent: once migrated, no
     * artisan has a legacy id left, so later runs do nothing. Runs on admin
     * login; accounts are created on the secondary app so the admin stays
     * signed in.
     */
    suspend fun provisionDemoArtisanLogins(): Int {
        var created = 0
        try {
            val secondary = FirebaseModule.secondaryAuth(context)
            val legacy = artisansCol.get().await().documents
                .mapNotNull { it.toObject(Artisan::class.java) }
                .filter { it.id.startsWith("art_") }

            legacy.forEach { artisan ->
                try {
                    val uid = try {
                        secondary.createUserWithEmailAndPassword(artisan.email, DEMO_ARTISAN_PASSWORD)
                            .await().user?.uid
                    } catch (_: Exception) {
                        // Account already exists — recover its uid with the demo password
                        secondary.signInWithEmailAndPassword(artisan.email, DEMO_ARTISAN_PASSWORD)
                            .await().user?.uid
                    } ?: return@forEach

                    artisansCol.document(uid).set(artisan.copy(id = uid)).await()
                    usersCol.document(uid).set(
                        User(id = uid, email = artisan.email, name = artisan.name, role = UserRole.ARTISAN)
                    ).await()
                    servicesCol.whereEqualTo("artisanId", artisan.id).get().await()
                        .documents.forEach { it.reference.update("artisanId", uid).await() }
                    // Follow the conversations too — an inquiry left pointing at the
                    // legacy id becomes invisible to the artisan forever.
                    rekeyInquiries(oldArtisanId = artisan.id, newArtisanId = uid)
                    artisansCol.document(artisan.id).delete().await()
                    usersCol.document(artisan.id).delete().await()
                    created++
                } catch (_: Exception) {
                    // Email taken with a different password, or offline — skip this one
                } finally {
                    secondary.signOut()
                }
            }
        } catch (_: Exception) {
            // Offline — retried on the next admin login
        }
        return created
    }

    /**
     * Ensures every artisan also has a profile in the users collection.
     * Admin-registered artisans get one at creation, but seeded demo artisans
     * were written straight into artisans/ without one, so they never showed up
     * in the users list. Runs on admin login — security rules only let admins
     * create non-customer profiles.
     */
    suspend fun backfillArtisanUserProfiles() {
        try {
            val artisans = artisansCol.get().await()
                .documents.mapNotNull { it.toObject(Artisan::class.java) }
            artisans.forEach { artisan ->
                val userDoc = usersCol.document(artisan.id).get().await()
                if (!userDoc.exists()) {
                    usersCol.document(artisan.id).set(
                        User(
                            id = artisan.id,
                            email = artisan.email,
                            name = artisan.name,
                            role = UserRole.ARTISAN
                        )
                    ).await()
                }
            }
        } catch (_: Exception) {
            // Offline, or rules rejected the write — retried on the next admin login
        }
    }

    /**
     * Changes an artisan's login credentials on their behalf.
     *
     * Password-only change: signs in as the artisan on the secondary app (needs
     * their current password — demo accounts use [DEMO_ARTISAN_PASSWORD]) and
     * updates it in place.
     *
     * New email: the client SDK cannot swap another account's email instantly,
     * so a fresh Auth account is created with the new credentials, every
     * Firestore doc is re-keyed to the new uid, and the old login is deleted
     * (best effort). The artisan can sign in with the new email immediately.
     */
    suspend fun changeArtisanLogin(
        artisanId: String,
        currentPassword: String,
        newEmail: String,
        newPassword: String
    ): Result<String> {
        val secondary = FirebaseModule.secondaryAuth(context)
        return try {
            val artisan = artisansCol.document(artisanId).get().await().toObject(Artisan::class.java)
                ?: return Result.failure(Exception("Artisan not found"))
            val email = newEmail.trim()
            if (newPassword.isNotBlank() && newPassword.length < 6) {
                return Result.failure(Exception("Password must be at least 6 characters"))
            }

            if (email.isBlank() || email.equals(artisan.email, ignoreCase = true)) {
                // Password-only rotation on the existing account
                if (newPassword.isBlank()) {
                    return Result.failure(Exception("Enter a new email, a new password, or both"))
                }
                val user = secondary.signInWithEmailAndPassword(artisan.email, currentPassword).await().user
                    ?: return Result.failure(Exception("Could not sign in as ${artisan.email}"))
                user.updatePassword(newPassword).await()
                secondary.signOut()
                return Result.success("Password updated — ${artisan.email} can log in with the new password now")
            }

            // New login email: create the replacement account, move everything over
            if (newPassword.isBlank()) {
                return Result.failure(Exception("Set a password for the new login email"))
            }
            val newUid = secondary.createUserWithEmailAndPassword(email, newPassword).await().user?.uid
                ?: return Result.failure(Exception("Could not create the new login account"))

            artisansCol.document(newUid).set(artisan.copy(id = newUid, email = email)).await()
            usersCol.document(newUid).set(
                User(id = newUid, email = email, name = artisan.name, role = UserRole.ARTISAN)
            ).await()
            servicesCol.whereEqualTo("artisanId", artisan.id).get().await()
                .documents.forEach { it.reference.update("artisanId", newUid).await() }
            // Carry the artisan's conversations to the new identity: inquiries
            // keyed to the old uid would vanish from their dashboard, and their
            // old messages would render on the wrong side of the chat.
            rekeyInquiries(oldArtisanId = artisan.id, newArtisanId = newUid)
            messagesCol.whereEqualTo("senderId", artisan.id).get().await()
                .documents.forEach { it.reference.update("senderId", newUid).await() }
            artisansCol.document(artisan.id).delete().await()
            usersCol.document(artisan.id).delete().await()

            // Retire the old login so the stale credentials stop working
            secondary.signOut()
            try {
                secondary.signInWithEmailAndPassword(artisan.email, currentPassword).await()
                    .user?.delete()?.await()
            } catch (_: Exception) {
                // Old account missing or password unknown — it is orphaned and
                // cannot reach any data because its profile docs are gone
            }
            secondary.signOut()
            Result.success("Login changed — the artisan now signs in with $email")
        } catch (e: Exception) {
            secondary.signOut()
            Result.failure(Exception(e.message ?: "Could not change login credentials"))
        }
    }

    /**
     * Points every inquiry at an artisan's new document id after a re-key
     * (demo provisioning or login change). Requires the admin update rule on
     * inquiries — participants themselves may never touch the participant fields.
     */
    private suspend fun rekeyInquiries(oldArtisanId: String, newArtisanId: String) {
        inquiriesCol.whereEqualTo("artisanId", oldArtisanId).get().await()
            .documents.forEach { it.reference.update("artisanId", newArtisanId).await() }
    }

    /** Sends a Firebase password-reset email to the user's login email address. */
    suspend fun sendPasswordResetEmail(userId: String): Result<String> {
        return try {
            val user = usersCol.document(userId).get().await().toObject(User::class.java)
                ?: return Result.failure(Exception("User profile not found"))
            FirebaseModule.auth.sendPasswordResetEmail(user.email).await()
            Result.success(user.email)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes an artisan entirely: profile, their services and their user profile.
     * The Auth account (if any) is blocked from logging in because the profile is gone.
     */
    suspend fun deleteArtisan(artisanId: String): Result<Unit> {
        return try {
            val services = servicesCol.whereEqualTo("artisanId", artisanId).get().await()
            services.documents.forEach { it.reference.delete().await() }
            artisansCol.document(artisanId).delete().await()
            usersCol.document(artisanId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(userId: String): Result<Unit> {
        return try {
            usersCol.document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Location ----------

    /**
     * Geocodes a structured Tanzanian address to coordinates.
     * Tries the full address first, then falls back to the region center.
     */
    suspend fun geocodeAddress(region: String, ward: String, street: String?): Pair<Double, Double>? {
        val fullAddress = listOfNotNull(
            street?.takeIf { it.isNotBlank() },
            ward,
            region
        ).joinToString(", ")
        return locationProvider.getCoordinatesFromAddress(fullAddress)
            ?: locationProvider.getCoordinatesFromAddress(region)
    }

    /**
     * Pins the artisan's workshop to the device's current GPS position — the most
     * accurate location source available (the artisan stands at their workshop).
     */
    suspend fun updateArtisanLocationFromGps(artisanId: String): Result<Pair<Double, Double>> {
        return try {
            val location = locationProvider.getCurrentLocation()
                ?: return Result.failure(Exception("Could not get GPS position. Turn on Location and try again."))
            val artisan = getArtisanById(artisanId)
                ?: return Result.failure(Exception("Artisan profile not found."))
            artisansCol.document(artisan.id).update(
                mapOf("latitude" to location.latitude, "longitude" to location.longitude)
            ).await()
            Result.success(Pair(location.latitude, location.longitude))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Registration ----------

    /**
     * Registers a new artisan: creates their Firebase Auth login (via the secondary
     * instance so the admin stays signed in), their user profile and artisan profile.
     */
    suspend fun registerArtisan(
        name: String,
        categoryId: String,
        region: String,
        ward: String,
        street: String,
        phoneNumber: String,
        email: String,
        password: String,
        bio: String,
        imageUrl: String,
        isVerified: Boolean = false
    ): Result<Unit> {
        return try {
            val coords = geocodeAddress(region, ward, street)
            val latitude = coords?.first ?: -6.7924
            val longitude = coords?.second ?: 39.2330

            // 1. Create the login account without disturbing the admin session
            val secondary = FirebaseModule.secondaryAuth(context)
            val result = secondary.createUserWithEmailAndPassword(email.trim(), password).await()
            val artisanId = result.user?.uid
                ?: return Result.failure(Exception("Could not create artisan account"))

            // 2. Create the user profile
            usersCol.document(artisanId).set(
                User(id = artisanId, email = email.trim(), name = name, role = UserRole.ARTISAN)
            ).await()

            // 3. Create the artisan profile
            val newArtisan = Artisan(
                id = artisanId,
                name = name,
                categoryId = categoryId,
                rating = 0f,
                latitude = latitude,
                longitude = longitude,
                phoneNumber = phoneNumber,
                email = email.trim(),
                bio = bio,
                imageUrl = imageUrl,
                verified = isVerified,
                region = region,
                ward = ward,
                street = street
            )
            artisansCol.document(artisanId).set(newArtisan).await()
            secondary.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Seed data (written to the cloud once, when collections are empty) ----------

    private suspend fun seedInitialData() {
        try {
            val existing = categoriesCol.limit(1).get().await()
            if (!existing.isEmpty) return

            val mockCategories = listOf(
                Category("cat_1", "Plumbing", "https://img.icons8.com/ios-filled/100/00B4D8/plumbing.png"),
                Category("cat_2", "Electrical", "https://img.icons8.com/ios-filled/100/00B4D8/electricity.png"),
                Category("cat_3", "Carpentry", "https://img.icons8.com/ios-filled/100/00B4D8/hammer.png"),
                Category("cat_4", "Cleaning", "https://img.icons8.com/ios-filled/100/00B4D8/broom.png"),
                Category("cat_5", "Painting", "https://img.icons8.com/ios-filled/100/00B4D8/paint-brush.png")
            )
            mockCategories.forEach { categoriesCol.document(it.id).set(it).await() }

            val mockArtisans = listOf(
                Artisan(
                    id = "art_1",
                    name = "Juma Hassan",
                    categoryId = "cat_1",
                    rating = 4.97f,
                    latitude = -6.7924,
                    longitude = 39.2330,
                    phoneNumber = "+255 654 123 001",
                    email = "juma.hassan@plumbing.com",
                    bio = "Expert plumber with 15+ years of professional experience. Specialized in emergency plumbing repairs, bathroom installations, and modern piping systems. Available 24/7 for urgent repairs.",
                    imageUrl = "https://i.pinimg.com/736x/6d/66/af/6d66af4d10a9a7d19d1df880b0ce3b23.jpg",
                    verified = true,
                    region = "Dar es Salaam"
                ),
                Artisan(
                    id = "art_2",
                    name = "Mary M.",
                    categoryId = "cat_2",
                    rating = 4.93f,
                    latitude = -6.8161,
                    longitude = 39.2746,
                    phoneNumber = "+255 654 123 002",
                    email = "mary.electric@services.com",
                    bio = "Certified electrician with 12 years of industry experience. Expert in residential and commercial electrical installations, smart home wiring, solar panels, and safety inspections.",
                    imageUrl = "https://images.unsplash.com/photo-1544724569-5f546fd6f2b5?q=80&w=2070&auto=format&fit=crop",
                    verified = true,
                    region = "Dar es Salaam"
                ),
                Artisan(
                    id = "art_3",
                    name = "David L.",
                    categoryId = "cat_3",
                    rating = 4.87f,
                    latitude = -6.7695,
                    longitude = 39.2362,
                    phoneNumber = "+255 654 123 003",
                    email = "david.carpenter@crafts.com",
                    bio = "Modern carpenter specializing in custom furniture, kitchen cabinets, and home renovations. Uses sustainable materials and provides consultation for all carpentry projects.",
                    imageUrl = "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?q=80&w=2070&auto=format&fit=crop",
                    verified = true,
                    region = "Dar es Salaam"
                ),
                Artisan(
                    id = "art_4",
                    name = "Sarah K.",
                    categoryId = "cat_5",
                    rating = 4.69f,
                    latitude = -6.7482,
                    longitude = 39.2743,
                    phoneNumber = "+255 654 123 004",
                    email = "sarah.painting@pro.com",
                    bio = "Professional painter with 8 years of experience in interior and exterior painting. Specializes in wall art, textured finishes, and premium paint applications for residential and commercial spaces.",
                    imageUrl = "https://i.pinimg.com/1200x/01/78/a1/0178a10b5c4fba3512c66e4c950c4ffd.jpg",
                    verified = true,
                    region = "Dar es Salaam"
                ),
                Artisan(
                    id = "art_5",
                    name = "Amos P.",
                    categoryId = "cat_1",
                    rating = 4.98f,
                    latitude = -6.7800,
                    longitude = 39.2200,
                    phoneNumber = "+255 654 123 005",
                    email = "amos.emergency@24-7.com",
                    bio = "24/7 emergency plumber specializing in leak detection and emergency repairs. Quick response time with guaranteed professional service for all plumbing emergencies.",
                    imageUrl = "https://i.pinimg.com/736x/42/4d/66/424d66f9d433eb170b5e14badfce7950.jpg",
                    verified = true,
                    region = "Dar es Salaam"
                ),
                Artisan(
                    id = "art_6",
                    name = "Grace J.",
                    categoryId = "cat_2",
                    rating = 4.95f,
                    latitude = -6.8100,
                    longitude = 39.2800,
                    phoneNumber = "+255 654 123 006",
                    email = "grace.rapid@electric.com",
                    bio = "Rapid response electrical technician for emergency situations. Available immediately for urgent electrical issues, outages, and safety hazards. Licensed and fully insured.",
                    imageUrl = "https://i.pinimg.com/1200x/00/ce/ab/00ceabcf6b336b1a9e2a22eb971baa9b.jpg",
                    verified = true,
                    region = "Dar es Salaam"
                ),
                Artisan(
                    id = "art_7",
                    name = "Kelvin T.",
                    categoryId = "cat_1",
                    rating = 4.88f,
                    latitude = -6.7400,
                    longitude = 39.2700,
                    phoneNumber = "+255 654 123 007",
                    email = "kelvin.drain@urgent.com",
                    bio = "Specialized in urgent drain clearing and pipe maintenance. Uses advanced equipment for fast, efficient drain cleaning without disrupting your home. Emergency callouts available.",
                    imageUrl = "https://i.pinimg.com/1200x/b6/54/f3/b654f3336a2ccec52ed2d78346903541.jpg",
                    verified = true,
                    region = "Dar es Salaam"
                ),
                Artisan(
                    id = "art_8",
                    name = "Peter O.",
                    categoryId = "cat_3",
                    rating = 4.91f,
                    latitude = -6.7750,
                    longitude = 39.2400,
                    phoneNumber = "+255 654 123 008",
                    email = "peter.furniture@custom.com",
                    bio = "Expert in custom furniture design and fabrication. Provides free consultations for all projects. Known for unique designs, quality craftsmanship, and attention to detail in every piece.",
                    imageUrl = "https://i.pinimg.com/736x/dc/81/43/dc8143a0f8c5b060aa51a8b0119bdbe9.jpg",
                    verified = true,
                    region = "Dar es Salaam"
                ),
                Artisan(
                    id = "art_9",
                    name = "Aisha S.",
                    categoryId = "cat_5",
                    rating = 4.85f,
                    latitude = -6.1659,
                    longitude = 39.2026,
                    phoneNumber = "+255 654 123 009",
                    email = "aisha.interior@design.com",
                    bio = "Interior painting specialist with 10+ years delivering premium finishes. Transform your spaces with professional color consultation, decorative techniques, and flawless execution.",
                    imageUrl = "https://images.unsplash.com/photo-1562259949-e8e7689d7828?q=80&w=2070&auto=format&fit=crop",
                    verified = true,
                    region = "Zanzibar"
                ),
                Artisan(
                    id = "art_10",
                    name = "Bakari",
                    categoryId = "cat_3",
                    rating = 4.79f,
                    latitude = -3.3731,
                    longitude = 36.6830,
                    phoneNumber = "+255 654 123 010",
                    email = "bakari.kitchen@remodel.com",
                    bio = "Kitchen remodeling specialist with expertise in modern designs and functional layouts. Project-based pricing with transparent quotes. Delivers high-quality results on time and on budget.",
                    imageUrl = "https://images.unsplash.com/photo-1556911220-e15b29be8c8f?q=80&w=2070&auto=format&fit=crop",
                    verified = true,
                    region = "Arusha"
                )
            )
            mockArtisans.forEach { artisansCol.document(it.id).set(it).await() }

            val mockServices = listOf(
                Service("ser_1", "art_1", "Emergency Leak Repair", 25000.0),
                Service("ser_2", "art_1", "Bathroom Installation", 150000.0),
                Service("ser_3", "art_1", "Pipe Replacement", 45000.0),
                Service("ser_4", "art_1", "Drain Cleaning", 30000.0),
                Service("ser_5", "art_1", "Sink Installation", 40000.0),
                Service("ser_6", "art_2", "Home Wiring Inspection", 50000.0),
                Service("ser_7", "art_2", "Solar Panel Installation", 850000.0),
                Service("ser_8", "art_2", "Lighting Installation", 35000.0),
                Service("ser_9", "art_2", "Circuit Breaker Repair", 75000.0),
                Service("ser_10", "art_2", "Smart Home Setup", 120000.0),
                Service("ser_11", "art_3", "Custom Furniture Design", 250000.0),
                Service("ser_12", "art_3", "Kitchen Cabinet Installation", 450000.0),
                Service("ser_13", "art_3", "Door Installation", 80000.0),
                Service("ser_14", "art_3", "Furniture Repair", 60000.0),
                Service("ser_15", "art_3", "Wood Floor Installation", 300000.0),
                Service("ser_16", "art_4", "Interior Painting", 150000.0),
                Service("ser_17", "art_4", "Exterior Painting", 250000.0),
                Service("ser_18", "art_4", "Wall Art & Murals", 200000.0),
                Service("ser_19", "art_4", "Textured Finishes", 100000.0),
                Service("ser_20", "art_4", "Commercial Painting", 500000.0),
                Service("ser_21", "art_5", "24/7 Emergency Repair", 60000.0),
                Service("ser_22", "art_5", "Leak Detection", 40000.0),
                Service("ser_23", "art_5", "Water Heater Service", 55000.0),
                Service("ser_24", "art_5", "Septic Tank Cleaning", 120000.0),
                Service("ser_25", "art_6", "24/7 Emergency Electrical", 70000.0),
                Service("ser_26", "art_6", "Power Outage Service", 45000.0),
                Service("ser_27", "art_6", "Electrical Safety Audit", 40000.0),
                Service("ser_28", "art_6", "Generator Installation", 250000.0),
                Service("ser_29", "art_7", "Urgent Drain Clearing", 50000.0),
                Service("ser_30", "art_7", "Clogged Pipe Removal", 40000.0),
                Service("ser_31", "art_7", "Drain Maintenance", 25000.0),
                Service("ser_32", "art_7", "Sewer Line Inspection", 60000.0),
                Service("ser_33", "art_8", "Custom Wardrobe Design", 400000.0),
                Service("ser_34", "art_8", "Bedroom Furniture", 350000.0),
                Service("ser_35", "art_8", "Office Furniture", 300000.0),
                Service("ser_36", "art_8", "Free Consultation", 0.0),
                Service("ser_37", "art_9", "Premium Interior Painting", 250000.0),
                Service("ser_38", "art_9", "Decorative Finishes", 180000.0),
                Service("ser_39", "art_9", "Color Consultation", 30000.0),
                Service("ser_40", "art_9", "Accent Wall Painting", 80000.0),
                Service("ser_41", "art_10", "Full Kitchen Remodel", 2500000.0),
                Service("ser_42", "art_10", "Cabinet Refacing", 800000.0),
                Service("ser_43", "art_10", "Countertop Installation", 500000.0),
                Service("ser_44", "art_10", "Design Consultation", 50000.0)
            )
            mockServices.forEach { servicesCol.document(it.id).set(it).await() }
        } catch (_: Exception) {
            // Offline or race with another device seeding — safe to ignore (fixed doc ids)
        }
    }

    companion object {
        private const val TAG = "ArtisanRepository"

        /** Shared login password for the seeded demo artisan accounts. */
        const val DEMO_ARTISAN_PASSWORD = "Artisan@123"
    }
}
