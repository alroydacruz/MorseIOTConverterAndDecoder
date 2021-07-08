package com.alroy.morsecodedetector.util

import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.withTimeoutOrNull

inline fun <T> safeCall(action: () -> Resource<T>): Resource<T> {
    return try {
        action()
    } catch(e: Exception) {
        Log.i("boika",e.message?: "An unknown error occured" )
        Resource.Error("${e.message}  safe call"?: "An unknown error occured" )
    }
}