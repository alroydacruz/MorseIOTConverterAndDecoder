package com.alroy.morsecodedetector.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.alroy.morsecodedetector.R
import com.alroy.morsecodedetector.viewmodels.MainViewModel
import com.alroy.morsecodedetector.databinding.SendMorseToDeviceFragmentBinding
import com.alroy.morsecodedetector.entities.Device
import com.alroy.morsecodedetector.util.Event
import com.alroy.morsecodedetector.util.snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class SendMorseToDeviceFragment : Fragment(R.layout.send_morse_to_device_fragment) {

    private lateinit var viewModel: MainViewModel
    var _binding: SendMorseToDeviceFragmentBinding? = null
    val binding get() = _binding!!
    private val users = FirebaseFirestore.getInstance().collection("users")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SendMorseToDeviceFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        getUpdatedDeviceInRealTime()

        binding.transmitMessageBtn.setOnClickListener {
            val message = binding.messageEt.editText!!.text.trim()
            Log.i("nigger", message.toString())
            subscribeToObservers()
            viewModel.transmitMessage(
                Device(message = message.toString().toLowerCase(), messageState = 0)
            )
        }


        binding.deleteMessageButton.setOnClickListener {
            viewModel.transmitMessage(
                Device(message = "reset", messageState = -1)
            )
            binding.messageEt.editText?.apply {
                setText("")
                isEnabled = true
            }
            binding.transmitMessageBtn.text = "SEND"
            binding.transmitMessageBtn.isEnabled = true
        }
    }

    private fun getUpdatedDeviceInRealTime() {
        users.addSnapshotListener { quesrySnapshot, firebaseSirestoreException ->
            var device: Device? = null
            quesrySnapshot?.let {
                for (doc in it) {
                    device = doc.toObject()

                }
                if (device?.messageState == -1) {
                    binding.messageEt.editText?.apply {
                        setText("")
                        binding.messageEt.editText?.isEnabled = true
                    }
                } else {
                    binding.messageEt.editText?.setText(device?.message ?: "")
                    binding.messageEt.editText?.isEnabled = false
                    binding.transmitMessageBtn.isEnabled = false

                    if (device?.messageState == 0) {
                        binding.transmitMessageBtn.text = "SENT"
                    } else if (device?.messageState == 1) {
                        binding.transmitMessageBtn.text = "RECEIVED"
                    } else if (device?.messageState == 2) {
                        binding.transmitMessageBtn.text = "COMPLETED"
                    }

                }
            }
        }
    }

    private fun subscribeToObservers() {

        viewModel.sendMessageStatus.observe(viewLifecycleOwner, Event.EventObserver(
            onError = { error ->
//                loadingDialog?.let { if (it.isShowing) it.cancel() }
                snackbar(error)
            },
            onLoading = {
                //loadingDialog = CommonUtils.showLoadingDialog(requireContext())
            }
        ) { device ->
            if (device.messageState == -1) {
                binding.messageEt.editText?.apply {
                    binding.messageEt.isEnabled = true
                    setText("")
                }
            } else {
                // loadingDialog?.let { if (it.isShowing) it.cancel() }
                //single tick cus message got send from your side
            }

        })

    }
}