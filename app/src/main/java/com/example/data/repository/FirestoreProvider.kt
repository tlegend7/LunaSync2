package com.example.data.repository

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

object FirestoreProvider {
  private var firestoreInstance: FirebaseFirestore? = null
  private var authInstance: FirebaseAuth? = null

  fun ensureInitialized(context: Context) {
    val appContext = context.applicationContext ?: context
    if (FirebaseApp.getApps(appContext).any { it.name == FirebaseApp.DEFAULT_APP_NAME }) return
    val configuredApp = FirebaseApp.initializeApp(appContext)
    if (configuredApp == null) {
      // Sin configuración externa solo se permite explorar la demo; no hay credenciales reales.
      val options = FirebaseOptions.Builder()
        .setApplicationId("1:123456789012:android:0000000000000000")
        .setProjectId("demo-no-project")
        .setApiKey("AIzaSyAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        .build()
      FirebaseApp.initializeApp(appContext, options)
    }
  }

  fun getDb(context: Context): FirebaseFirestore {
    ensureInitialized(context)
    if (firestoreInstance == null) {
      val dbId = try {
        context.applicationContext.getString(R.string.firestore_database_id)
      } catch (e: Exception) {
        ""
      }
      val db = if (dbId.isNotBlank()) {
        try {
          FirebaseFirestore.getInstance(dbId)
        } catch (_: Exception) {
          Firebase.firestore
        }
      } else {
        Firebase.firestore
      }
      val emulatorHost = System.getenv("FIRESTORE_EMULATOR_HOST")
      if (!emulatorHost.isNullOrBlank()) {
        val parts = emulatorHost.split(":")
        val host = parts[0]
        val port = if (parts.size > 1) parts[1].toInt() else 8085
        try {
          db.useEmulator(host, port)
          val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder(db.firestoreSettings)
            .setPersistenceEnabled(false)
            .build()
          db.firestoreSettings = settings
        } catch (_: Exception) {}
      }
      firestoreInstance = db
    }
    return firestoreInstance!!
  }

  fun getAuth(context: Context? = null): FirebaseAuth {
    if (context != null) {
      ensureInitialized(context)
    }
    if (authInstance == null) {
      val auth = Firebase.auth
      val emulatorHost = System.getenv("FIREBASE_AUTH_EMULATOR_HOST")
      if (!emulatorHost.isNullOrBlank()) {
        val parts = emulatorHost.split(":")
        val host = parts[0]
        val port = if (parts.size > 1) parts[1].toInt() else 9099
        try {
          auth.useEmulator(host, port)
        } catch (_: Exception) {}
      }
      authInstance = auth
    }
    return authInstance!!
  }

  fun setDbForTesting(db: FirebaseFirestore?) {
    firestoreInstance = db
  }

  fun setAuthForTesting(auth: FirebaseAuth?) {
    authInstance = auth
  }
}
