package com.alroy.morsecodedetector.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.alroy.morsecodedetector.R
import com.alroy.morsecodedetector.databinding.MorseDecodeFragmentBinding
import com.alroy.morsecodedetector.databinding.SettingsDialogBinding
import com.alroy.morsecodedetector.repository.SettingPreferences
import com.alroy.morsecodedetector.util.Event
import com.alroy.morsecodedetector.util.snackbar
import com.alroy.morsecodedetector.viewmodels.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.RangeSlider
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt


@AndroidEntryPoint
class SettingsBottomSheetFragment : BottomSheetDialogFragment() {

    var _binding: SettingsDialogBinding? = null
    val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val inflator = inflater.inflate(R.layout.settings_dialog, container, false)
        _binding = SettingsDialogBinding.bind(inflator)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        viewModel.getSettings()
        subscribeToObservers()



        binding.rangebar.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: RangeSlider) {
                // Responds to when slider's touch event is being started
            }

            override fun onStopTrackingTouch(slider: RangeSlider) {

            }
        })

        binding.rangebar.addOnChangeListener { rangeSlider, value, fromUser ->
            viewModel.updateErrorRange(rangeSlider.values.map { it.toInt() })
        }

        binding.switchButton.setOnCheckedChangeListener { view, isChecked ->
            viewModel.updateAccelerometerSwitch(isChecked)
        }

        binding.fluidSlider.positionListener = { pos ->
            binding.brightnessDesc.text =
                "The brightness must increase atleast by ${(pos * 100).roundToInt()}% from base brightness to record a Dash. The Lower the value the greater the sensitivity."
            viewModel.updateBrightnessPer((pos * 100).roundToInt())
        }

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
        ) {settings->
            //setting of fluidslider
            binding.fluidSlider.position = settings.brightnessPer*0.01.toFloat()
            //setting of accelerometer
            binding.switchButton.isChecked = settings.accelerometerSwitch
            val range = settings.errorRange.split(":").map { it.toFloat() }
            binding.rangebar.values =  range
        })
    }


}