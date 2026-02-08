package com.eyuphanaydin.discbase

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class DiscBaseApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Realtime Database Offline Modu (Zaten eklemiştik)
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) { }

        // 2. YENİ: Firestore Offline Modu (Bunu eklemelisin!)
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (e: Exception) { }
    }
}