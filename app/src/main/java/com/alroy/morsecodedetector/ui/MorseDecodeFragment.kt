package com.alroy.morsecodedetector.ui

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.alroy.morsecodedetector.R
import com.alroy.morsecodedetector.databinding.MorseDecodeFragmentBinding
import com.alroy.morsecodedetector.util.Event
import com.alroy.morsecodedetector.util.Morse
import com.alroy.morsecodedetector.util.snackbar
import com.alroy.morsecodedetector.viewmodels.MainViewModel
import com.github.nisrulz.sensey.MovementDetector.MovementListener
import com.github.nisrulz.sensey.Sensey
import com.github.nisrulz.sensey.TiltDirectionDetector.TiltDirectionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MorseDecodeFragment : Fragment(R.layout.morse_decode_fragment), SensorEventListener {

    var _binding: MorseDecodeFragmentBinding? = null
    val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel

    private lateinit var sensorManager: SensorManager
    private var brightness: Sensor? = null


    var brightnessIncPer = 60f
    var errorPerPositive = 50f
    var errorPerNegative = -50f


    var startingRoomBrightness = 0f
    var isStarted = false
    var morseCodeTemp = " "
    var morseCode = " "
    var firstTime = true
    var startTime = SystemClock.elapsedRealtime()
    var delay = SystemClock.elapsedRealtime()
    var doneWithLowCal = true
    var doneWithHighCal = false
    var textMessage = ""

    var isMotionSensingEnabled = false
    var isRotating = false
    var isMoving = false

    var movementListener: MovementListener? = null
    var tiltDirectionListener: TiltDirectionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Sensey.getInstance().init(context);
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MorseDecodeFragmentBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//
//        val typeface = Typeface.createFromAsset(activity?.applicationContext?.assets, "font/morse.ttf")
//        binding.morseCode.typeface = typeface


        resetEverything()

        binding.messageButton.setOnClickListener {
            findNavController().navigate(R.id.action_morseDecodeFragment_to_sendMorseToDeviceFragment)
        }
        binding.startBtn.setOnClickListener {
//            Toast.makeText(
//                context,
//                "${brightnessIncPer}   || $errorPerNegative  $errorPerPositive",
//                Toast.LENGTH_SHORT
//            ).show()
            isStarted = !isStarted
            if (isStarted) {
                if (isMotionSensingEnabled) {
                    motionSensor(true)
                }
                binding.startBtn.text = "PAUSE"
                binding.resetBtn.isEnabled = false
            } else {
                Sensey.getInstance().stopMovementDetection(movementListener)
                Sensey.getInstance().stopTiltDirectionDetection(tiltDirectionListener)
                startTime = SystemClock.elapsedRealtime()
                delay = SystemClock.elapsedRealtime()
                binding.resetBtn.isEnabled = true
                binding.startBtn.text = "RESUME"
            }
        }

        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_morseDecodeFragment_to_settingsBottomSheetFragment)
        }

        binding.resetBtn.setOnClickListener {
            if (!isStarted) {
                resetEverything()
            }
        }

        setUpSensorStuff()

    }

    private fun motionSensor(enabled: Boolean) {
        movementListener = object : MovementListener {
            override fun onMovement() {
                isMoving = true
                vibrate()
                Log.i("acctest", "moving")
            }

            override fun onStationary() {
                isMoving = false
                Log.i("acctest", "stationary")
            }
        }

        tiltDirectionListener = object : TiltDirectionListener {
            override fun onTiltInAxisX(direction: Int) {
                isRotating = true
                vibrate()
                Log.i("ooo", "x")
            }

            override fun onTiltInAxisY(direction: Int) {
                isRotating = true
                vibrate()
                Log.i("ooo", "y")
            }

            override fun onTiltInAxisZ(direction: Int) {
                isRotating = true
                vibrate()
                Log.i("ooo", "z")
            }
        }
        Sensey.getInstance().startTiltDirectionDetection(tiltDirectionListener)
        Sensey.getInstance().startMovementDetection(0.3F, 3000L, movementListener)
    }

    private fun resetEverything() {
        subscribeToObservers()
        Log.i("nigger", "reset ")

        binding.startBtn.text = "START"
        binding.decodedMessage.text = ""
        binding.morseCode.text = ""
        startingRoomBrightness = 0f
        isStarted = false
        morseCodeTemp = " "
        morseCode = " "
        firstTime = true
        startTime = SystemClock.elapsedRealtime()
        delay = SystemClock.elapsedRealtime()
        doneWithLowCal = true
        doneWithHighCal = false
        textMessage = ""
    }

    private fun setUpSensorStuff() {
        sensorManager =
            activity?.applicationContext?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        brightness = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)


        // Specify the sensor you want to listen to
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI,
                SensorManager.SENSOR_DELAY_UI
            )
        }

    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val light1 = event.values[0]

            binding.tvText.text = "LUX: ${light1.toInt()}"
            binding.lightStatus.text = "${brightness(light1)}"
            binding.circularProgressBar.setProgressWithAnimation(light1)

            if (!isStarted && firstTime) {
                startingRoomBrightness = light1
                Log.i("nigger", "startingRoomBrightness : $startingRoomBrightness")
            }
            if (isStarted) {
                if ((light1 > (startingRoomBrightness + (startingRoomBrightness * (brightnessIncPer / 100f)))) && !doneWithHighCal) {
                    if (firstTime) {
                        Log.i("nigger", "High")
                        startTime = SystemClock.elapsedRealtime()
                        firstTime = false
                    } else {
                        onLedToggle(1)
                    }
                    doneWithHighCal = true
                    doneWithLowCal = false
                }
                if (((light1 >= startingRoomBrightness - (startingRoomBrightness * (-errorPerNegative / 100f))) && (light1 <= startingRoomBrightness + (startingRoomBrightness * (errorPerPositive / 100f)))) && !doneWithLowCal) {
                    onLedToggle(0)
                    doneWithHighCal = false
                    doneWithLowCal = true
                }
            }
        }

//        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
//            //Log.d("Main", "onSensorChanged: sides ${event.values[0]} front/back ${event.values[1]} ")
//
//            // Sides = Tilting phone left(10) and right(-10)
//            val sides = event.values[0]
//
//            // Up/Down = Tilting phone up(10), flat (0), upside-down(-10)
//            val upDown = event.values[1]
//
////            square.apply {
////                rotationX = upDown * 3f
////                rotationY = sides * 3f
////                rotation = -sides
////                translationX = sides * -10
////                translationY = upDown * 10
////            }
//
//            Log.i("acctest",(upDown * 3f).toString()+"  "+(sides * 3f).toString()+"  "+ (-sides).toString()+"  "+(sides * -10).toString()+"  "+(upDown * 10).toString())
//
////            // Changes the colour of the square if it's completely flat
////            val color = if (upDown.toInt() == 0 && sides.toInt() == 0) Color.GREEN else Color.RED
////            square.setBackgroundColor(color)
////
////            square.text = "up/down ${upDown.toInt()}\nleft/right ${sides.toInt()}"
//        }
    }


    private fun onLedToggle(state: Int) {
        delay = SystemClock.elapsedRealtime() - startTime
        Log.i("nigger", delay.toString())
        val code = ((assignMorseCode(delay.toInt(), state = state == 1)) ?: "")
        if (code == "reset") {
            morseCodeTemp = ""
            morseCode += " "
        } else {
            morseCodeTemp += code
            morseCode += code
        }
        startTime = SystemClock.elapsedRealtime()
        Log.i("nigger", morseCodeTemp)

        binding.morseCode.text = morseCode
        Log.i("nigger", if (state == 1) "High" else "Low")


    }

    private fun assignMorseCode(delay: Int, state: Boolean) = when (delay) {
        in 600..2000 -> if (!state) "." else null
        in 2700..4500 -> if (!state) "-" else {
            textMessage += Morse().convertCharToMorse(morseCodeTemp.trim())
            Log.i("nigger", textMessage)
            binding.decodedMessage.text = textMessage
            "reset"
        }
        in 5600..10000 -> {
            textMessage += Morse().convertCharToMorse(morseCodeTemp.trim()) + "  "
            binding.decodedMessage.text = textMessage
            "reset"
        }
        else -> null
    }

    private fun brightness(brightness: Float): String {
        return when (brightness.toInt()) {
            0 -> "Dark"
            in 1..10 -> "Dim"
            in 11..800 -> "Normal"
            in 801..1000 -> "Bright"
            in 1001..25000 -> "Damn bright"
            else -> "Flashbang"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    override fun onResume() {
        super.onResume()
        // Register a listener for the sensor.
        sensorManager.registerListener(this, brightness, SensorManager.SENSOR_DELAY_FASTEST)

        if (isStarted) {
            if (isMotionSensingEnabled) {
                motionSensor(true)
            }
        }
    }


    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        Sensey.getInstance().stopTiltDirectionDetection(tiltDirectionListener)
        Sensey.getInstance().stopMovementDetection(movementListener)
    }

    private fun subscribeToObservers() {
        //google register status
        viewModel.settings.observe(viewLifecycleOwner, Event.EventObserver(
            onError = { error ->
//                loadingDialog?.let { if (it.isShowing) it.cancel() }
                snackbar(error)
            },
            onLoading = {
                //loadingDialog = CommonUtils.showLoadingDialog(requireContext())
            }
        ) { settings ->
            brightnessIncPer = settings.brightnessPer.toFloat()

            isMotionSensingEnabled = settings.accelerometerSwitch

            val range = settings.errorRange.split(":").map { it.toFloat() }
            errorPerNegative = range[0]
            errorPerPositive = range[1]
        })
    }

    fun vibrate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CoroutineScope(Dispatchers.Main).launch {
                val vibrator: Vibrator = context?.getSystemService(VIBRATOR_SERVICE) as Vibrator

                // this effect creates the vibration of default amplitude for 1000ms(1 sec)
                val vibrationEffect1: VibrationEffect =
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE);

                // it is safe to cancel other vibrations currently taking place
                vibrator.cancel()
                vibrator.vibrate(vibrationEffect1)
            }

        }
    }
}

