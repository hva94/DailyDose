package com.hvasoft.dailydose.presentation.screens.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.auth.AuthUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.databinding.FragmentProfileBinding
import com.hvasoft.dailydose.presentation.screens.common.HomeFragmentListener
import com.hvasoft.dailydose.presentation.screens.common.HostActivityListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ProfileFragment : Fragment(), HomeFragmentListener {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    private var hostActivityListener: HostActivityListener? = null
    private var imageSelectedUri: Uri? = null

    private lateinit var fragmentContext: Context
    private lateinit var photoFile: File

    private val selectImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                if (it.data?.data != null) imageSelectedUri = it.data?.data
                binding.imgPhoto.setImageURI(imageSelectedUri)
                postUserImageProfile()
            }
        }

    private val pickImageResult =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                imageSelectedUri = uri
                binding.imgPhoto.setImageURI(imageSelectedUri)
                postUserImageProfile()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        fragmentContext = requireContext()
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onRefresh()
        setupButtons()
        observeViewModel()
        viewModel.loadProfile(
            userId = Constants.currentUser.uid,
            authDisplayNameFallback = Constants.currentUser.displayName.orEmpty(),
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        hostActivityListener = activity as HostActivityListener
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is ProfileViewModel.Event.ProfileLoaded -> {
                                binding.tvName.text = event.displayName
                                binding.etName.setText(event.displayName)
                                showNameEditor(event.displayName.isBlank())
                                showUserImageProfile(event.photoUrl)
                            }

                            is ProfileViewModel.Event.NameUpdated -> {
                                binding.progressBar.visibility = View.INVISIBLE
                                val newName = binding.etName.text.toString().trim()
                                binding.tvName.text = newName
                                showNameEditor(false)
                                hideKeyboard()
                                FirebaseAuth.getInstance().currentUser?.let {
                                    Constants.currentUser = it
                                }
                                hostActivityListener?.showPopUpMessage(R.string.profile_name_updated)
                            }

                            is ProfileViewModel.Event.PhotoUpdated -> {
                                binding.progressBar.visibility = View.INVISIBLE
                                showUserImageProfile(event.photoUrl)
                                hostActivityListener?.showPopUpMessage(R.string.profile_user_image_updated)
                            }

                            is ProfileViewModel.Event.Failure -> {
                                binding.progressBar.visibility = View.INVISIBLE
                                hostActivityListener?.showPopUpMessage(event.messageRes)
                            }
                        }
                    }
                }
                launch {
                    viewModel.uploadProgress.collect { progress ->
                        if (progress == null) return@collect
                        binding.progressBar.visibility = View.VISIBLE
                        binding.progressBar.progress = progress
                    }
                }
            }
        }
    }

    private fun setupButtons() {
        with(binding) {
            btnSelect.setOnClickListener { selectImage(btnSelect.context) }
            btnEditName.setOnClickListener { showNameEditor(true) }
            btnCancelName.setOnClickListener { cancelNameEditing() }
            btnSaveName.setOnClickListener { updateUserName() }
            btnLogout.setOnClickListener {
                MaterialAlertDialogBuilder(fragmentContext)
                    .setTitle(R.string.dialog_logout_title)
                    .setPositiveButton(R.string.dialog_logout_confirm) { _, _ -> signOut() }
                    .setNegativeButton(R.string.dialog_logout_cancel, null)
                    .show()
            }
        }
    }

    private fun signOut() {
        AuthUI.getInstance().signOut(fragmentContext).addOnCompleteListener {
            Toast.makeText(fragmentContext, R.string.profile_logout_success, Toast.LENGTH_SHORT).show()
            binding.tvName.text = ""
            binding.etName.setText("")
            showNameEditor(true)
            binding.tvEmail.text = ""
            binding.imgPhoto.setImageResource(0)
            (activity?.findViewById(R.id.bottomNav) as? BottomNavigationView)?.selectedItemId =
                R.id.action_home
        }
    }

    private fun selectImage(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_options_title)
            .setItems(resources.getStringArray(R.array.array_options_item)) { _, item ->
                when (item) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        viewLifecycleOwner.lifecycleScope.launch {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photoFile = getPhotoFile()
            val fileProvider = FileProvider.getUriForFile(
                requireActivity().baseContext,
                "com.hvasoft.fileprovider",
                photoFile,
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
            imageSelectedUri = fileProvider
            selectImageResult.launch(takePictureIntent)
        }
    }

    private fun getPhotoFile(): File {
        val storageDirectory = fragmentContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("photo", ".jpg", storageDirectory)
    }

    private fun openGallery() {
        pickImageResult.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun postUserImageProfile() {
        if (imageSelectedUri == null) return
        val currentName = binding.etName.text.toString().trim()
            .ifEmpty { binding.tvName.text.toString() }
        viewModel.uploadProfilePhoto(
            userId = Constants.currentUser.uid,
            imageUri = imageSelectedUri!!,
            currentUserName = currentName,
        )
    }

    private fun updateUserName() {
        val newName = binding.etName.text.toString().trim()
        if (newName.isEmpty()) {
            binding.tilName.error = getString(R.string.profile_name_empty_error)
            return
        }
        binding.tilName.error = null
        binding.progressBar.visibility = View.VISIBLE
        viewModel.updateDisplayName(
            userId = Constants.currentUser.uid,
            newName = newName,
            authDisplayNameFallback = Constants.currentUser.displayName.orEmpty(),
        )
    }

    private fun showNameEditor(show: Boolean) {
        with(binding) {
            tilName.isVisible = show
            btnCancelName.isVisible = show
            btnSaveName.isVisible = show
            btnEditName.isVisible = !show
            if (show) etName.requestFocus() else tilName.error = null
        }
    }

    private fun cancelNameEditing() {
        val currentName = binding.tvName.text?.toString().orEmpty()
        binding.etName.setText(currentName)
        showNameEditor(currentName.isBlank())
        hideKeyboard()
    }

    private fun showUserImageProfile(photoUrl: String) {
        if (photoUrl.isBlank()) return
        Glide.with(fragmentContext)
            .load(photoUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .circleCrop()
            .into(binding.imgPhoto)
    }

    private fun hideKeyboard() {
        val imm = fragmentContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    override fun onRefresh() {
        val displayName = Constants.currentUser.displayName.orEmpty()
        with(binding) {
            tvName.text = displayName
            etName.setText(displayName)
            showNameEditor(displayName.isBlank())
            tvEmail.text = Constants.currentUser.email
        }
    }
}
