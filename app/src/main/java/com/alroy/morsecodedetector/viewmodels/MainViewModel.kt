package com.alroy.morsecodedetector.viewmodels


import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.alroy.morsecodedetector.repository.MainRepository
import com.alroy.morsecodedetector.entities.Device
import com.alroy.morsecodedetector.repository.SettingPreferences
import com.alroy.morsecodedetector.util.Event
import com.alroy.morsecodedetector.util.Morse
import com.alroy.morsecodedetector.util.Resource
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel @ViewModelInject constructor(
    private val mainRepository: MainRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {


    private val _googleRegisterLoginStatus = MutableLiveData<Event<Resource<AuthResult>>>()
    val googleRegisterLoginStatus: LiveData<Event<Resource<AuthResult>>> =
        _googleRegisterLoginStatus

    private val _sendMessageStatus = MutableLiveData<Event<Resource<Device>>>()
    val sendMessageStatus: LiveData<Event<Resource<Device>>> = _sendMessageStatus

    private val _settings = MutableLiveData<Event<Resource<SettingPreferences>>>()
    val settings: LiveData<Event<Resource<SettingPreferences>>> = _settings

    fun getSettings() {
        viewModelScope.launch(dispatcher) {
            val result = mainRepository.getSettings().first()
            _settings.postValue(Event(Resource.Success(result)))
        }
    }

    fun registerLoginGoogle(googleSignInAccount: GoogleSignInAccount) {
        _googleRegisterLoginStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher) {
            val result = mainRepository.registerLoginGoogle(googleSignInAccount)
            _googleRegisterLoginStatus.postValue(Event(result))
        }
    }

    fun transmitMessage(device: Device) {
        var error = ""
        if (device.message.isEmpty()) {
            error = "Enter something na"
            _sendMessageStatus.postValue(Event(Resource.Error(error)))

        } else if (Morse().checkValidString(device.message).isNotEmpty()) {
            Morse().checkValidString(device.message).toSet().forEach { letter ->
                error += "$letter  "
            }
            error += " not supported. Try tweaking your message"
            _sendMessageStatus.postValue(Event(Resource.Error(error)))

        } else {
            _sendMessageStatus.postValue(Event(Resource.Loading()))
            viewModelScope.launch(dispatcher) {
                val result = mainRepository.transmitMessage(device)
                _sendMessageStatus.postValue(Event(result))

            }
        }
    }

    fun updateBrightnessPer(percent: Int) = viewModelScope.launch(dispatcher) {
        mainRepository.updateBrightnessPer(percent)
    }

    fun updateAccelerometerSwitch(state: Boolean) = viewModelScope.launch(dispatcher) {
        mainRepository.updateAccelerometerSwitch(state)
    }

    fun updateErrorRange(range: List<Int>) = viewModelScope.launch(dispatcher) {
        mainRepository.updateErrorRange(range)
    }
}
