package com.alroy.morsecodedetector.repository



import android.content.Context
import com.alroy.morsecodedetector.entities.Device
import com.alroy.morsecodedetector.util.Resource
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface MainRepository {

    suspend fun registerLoginGoogle(googleSignInAccount: GoogleSignInAccount): Resource<AuthResult>
    suspend fun transmitMessage(device: Device): Resource<Device>
    suspend fun updateBrightnessPer(percent: Int)
    suspend fun updateAccelerometerSwitch(state: Boolean)
    suspend fun updateErrorRange(range: List<Int>)
    suspend fun getSettings():Flow<SettingPreferences>

}