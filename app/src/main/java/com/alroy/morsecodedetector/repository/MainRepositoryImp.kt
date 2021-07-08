package com.alroy.morsecodedetector.repository


import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.createDataStore
import com.alroy.morsecodedetector.entities.Device
import com.alroy.morsecodedetector.util.Resource
import com.alroy.morsecodedetector.util.safeCall
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject


data class SettingPreferences(val brightnessPer: Int=20, val accelerometerSwitch: Boolean=false, val errorRange: String="-40:40")


class MainRepositoryImp @Inject constructor(@ApplicationContext val context: Context) : MainRepository {

    private val auth = FirebaseAuth.getInstance()
    private val users = FirebaseFirestore.getInstance().collection("users")
    private val firestore = FirebaseFirestore.getInstance()

    private val dataStore= context.createDataStore("settings")

    private object PreferenceKeys {
        val BRIGHTNESS_PER = preferencesKey<Int>("brightness_per")
        val ACCELEROMETER_SWITCH = preferencesKey<Boolean>("accelerometer_switch")
        val ERROR_RANGE = preferencesKey<String>("error_range")
    }




    override suspend fun updateBrightnessPer(percent: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.BRIGHTNESS_PER] = percent
        }
    }

    override suspend fun updateAccelerometerSwitch(state: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ACCELEROMETER_SWITCH] = state
        }
    }



    override suspend fun updateErrorRange(range: List<Int>) {
        dataStore.edit { preferences ->
                preferences[PreferenceKeys.ERROR_RANGE] = "${range[0]}:${range[1]}"
        }
    }

    override suspend fun getSettings(): Flow<SettingPreferences> {
        return dataStore.data
            .catch { exception->
                if(exception is IOException){
                    Log.e("nigger", "ex : $exception")
                    emit(emptyPreferences())
                }else{
                    throw exception
                }
            }
            .map { preferences ->
                val brightnessPer = preferences[PreferenceKeys.BRIGHTNESS_PER] ?: 50
                val accelerometerSwitch = preferences[PreferenceKeys.ACCELEROMETER_SWITCH] ?: false
                val errorRange = preferences[PreferenceKeys.ERROR_RANGE] ?: "-40:40"
                SettingPreferences(brightnessPer, accelerometerSwitch,errorRange)
            }
    }


    override suspend fun registerLoginGoogle(googleSignInAccount: GoogleSignInAccount): Resource<AuthResult> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val credentials =
                    GoogleAuthProvider.getCredential(googleSignInAccount.idToken, null)
                val result = auth.signInWithCredential(credentials).await()

                if (result.additionalUserInfo!!.isNewUser) {
                    //sign in successful, create new user
                    val uid = result.user?.uid!!
                    val localUser = Device(deviceId = uid)
                    users.document(uid).set(localUser).await()
                    Resource.Success(result)
                } else {
                    //sign in successful, user already exists
                    Resource.Success(result)
                }
            }
        }
    }

    override suspend fun transmitMessage(device: Device): Resource<Device> =
        withContext(Dispatchers.IO) {
            safeCall {
                val uid = FirebaseAuth.getInstance().currentUser!!.uid
                val map = mutableMapOf(
                    "message" to device.message,
                    "messageState" to device.messageState
                )
                users.document(uid).update(map.toMap()).await()
                Resource.Success(device)
            }
        }


}