package com.example.data.repository

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

object FirestoreProvider {
  private var firestoreInstance: FirebaseFirestore? = null

  fun getDb(context: Context): FirebaseFirestore {
    if (firestoreInstance == null) {
      val dbId = try {
        context.applicationContext.getString(R.string.firestore_database_id)
      } catch (e: Exception) {
        ""
      }
      firestoreInstance = if (dbId.isNotBlank()) {
        FirebaseFirestore.getInstance(dbId)
      } else {
        Firebase.firestore
      }
    }
    return firestoreInstance!!
  }

  fun getAuth(): FirebaseAuth = Firebase.auth
}
