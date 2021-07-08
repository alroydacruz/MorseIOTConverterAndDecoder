package com.alroy.morsecodedetector.entities


import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Device(
    val deviceId: String = "",
    val message: String = "",
    var messageState: Int = -1, // -1 = no message ,  0 = message sent  , 1 = message received  , 2 = completed job successfully
//    @Exclude
//    var isFollowing: Boolean = false
)