package com.alroy.morsecodedetector.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.alroy.morsecodedetector.R
import com.alroy.morsecodedetector.databinding.MorseCodeLoginFragmentBinding
import com.alroy.morsecodedetector.util.Event
import com.alroy.morsecodedetector.util.snackbar
import com.alroy.morsecodedetector.viewmodels.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Exception
import javax.inject.Inject

@AndroidEntryPoint
class AuthFragment : Fragment(R.layout.morse_code_login_fragment) {

    val REQUEST_CODE_SIGN_UP = 0

    var _binding: MorseCodeLoginFragmentBinding? = null
    val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel

    @Inject
    lateinit var googleSignInClient: GoogleSignInClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(FirebaseAuth.getInstance().currentUser != null) {
            findNavController().navigate(R.id.action_morseLoginFragment_to_morseDecodeFragment)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MorseCodeLoginFragmentBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        subscribeToObservers()

        binding.googleIcon.setOnClickListener {
            googleSignInClient.signInIntent.also {
                startActivityForResult(it, REQUEST_CODE_SIGN_UP)
            }
        }
    }

    private fun subscribeToObservers() {
        //google register status
        viewModel.googleRegisterLoginStatus.observe(viewLifecycleOwner, Event.EventObserver(
            onError = { error ->
//                loadingDialog?.let { if (it.isShowing) it.cancel() }
                snackbar(error)
            },
            onLoading = {
                //loadingDialog = CommonUtils.showLoadingDialog(requireContext())
            }
        ) {
            // loadingDialog?.let { if (it.isShowing) it.cancel() }
            snackbar("Welcome Mate")
            findNavController().navigate(R.id.action_morseLoginFragment_to_morseDecodeFragment)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
            if (requestCode == REQUEST_CODE_SIGN_UP) {
                account?.let {
                    viewModel.registerLoginGoogle(it)
                }
            }
//            else {
//                account?.let {
//                    viewModel.registerGoogle(it)
//                }
//            }
        } catch (e: Exception) {
            Log.e("nigger", e.message.toString())
        }
    }
}