package com.hvasoft.dailydose.presentation.screens.add

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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.databinding.FragmentAddBinding
import com.hvasoft.dailydose.presentation.screens.common.HostActivityListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddFragment : Fragment(), HostActivityListener {

    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddViewModel by viewModels()

    private lateinit var imageFile: File
    private var hostActivityListener: HostActivityListener? = null
    private var imageSelectedUri: Uri? = null

    private val selectImageResult =
        registerForActivityResult(StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                if (activityResult.data?.data != null) imageSelectedUri = activityResult.data?.data
                onImageSelected()
            }
        }

    private val pickImageResult = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            imageSelectedUri = uri
            onImageSelected()
        }
    }

    private fun onImageSelected() {
        with(binding) {
            imgPhoto.setImageURI(imageSelectedUri)
            tilTitle.visibility = View.VISIBLE
            etTitle.setText(getString(R.string.add_default_title, getCurrentTimeString()))
            tvMessage.text = getString(R.string.post_message_valid_title)
            btnSelect.visibility = View.GONE
        }
    }

    private fun getCurrentTimeString(): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis()))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTextField()
        setupButtons()
        observeUiState()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            hostActivityListener = context as HostActivityListener
        } catch (_: ClassCastException) {
            throw ClassCastException("$context must implement HostActivityListener")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupTextField() {
        binding.etTitle.addTextChangedListener { validateFields(binding.tilTitle) }
    }

    private fun setupButtons() {
        with(binding) {
            btnPost.setOnClickListener { if (validateFields(tilTitle)) postSnapshot() }
            btnSelect.setOnClickListener { selectImage(btnSelect.context) }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        AddPostUiState.Idle -> Unit

                        is AddPostUiState.Uploading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.progressBar.progress = state.percent
                            binding.tvMessage.text = String.format("%s%%", state.percent)
                        }

                        AddPostUiState.Success -> {
                            binding.progressBar.visibility = View.INVISIBLE
                            hideKeyboard()
                            hostActivityListener?.showPopUpMessage(R.string.post_message_post_success)
                            resetForm()
                            hostActivityListener?.onSnapshotPosted()
                            enableUI(true)
                            viewModel.acknowledgeTerminalState()
                        }

                        AddPostUiState.FailedImage -> {
                            binding.progressBar.visibility = View.INVISIBLE
                            hostActivityListener?.showPopUpMessage(R.string.post_message_post_image_fail)
                            enableUI(true)
                            viewModel.acknowledgeTerminalState()
                        }

                        AddPostUiState.FailedSave -> {
                            binding.progressBar.visibility = View.INVISIBLE
                            hostActivityListener?.showPopUpMessage(R.string.post_message_post_fail)
                            enableUI(true)
                            viewModel.acknowledgeTerminalState()
                        }
                    }
                }
            }
        }
    }

    private fun resetForm() {
        with(binding) {
            tilTitle.visibility = View.GONE
            etTitle.setText("")
            tilTitle.error = null
            tvMessage.text = getString(R.string.post_message_title)
            imgPhoto.setImageDrawable(null)
        }
        imageSelectedUri = null
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
            imageFile = getPhotoFile()
            val fileProvider = FileProvider.getUriForFile(
                requireActivity().baseContext,
                "com.hvasoft.fileprovider",
                imageFile,
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
            imageSelectedUri = fileProvider
            selectImageResult.launch(takePictureIntent)
        }
    }

    private fun getPhotoFile(): File {
        val storageDirectory = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("photo", ".jpg", storageDirectory)
    }

    private fun openGallery() {
        pickImageResult.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

    private fun postSnapshot() {
        val uri = imageSelectedUri ?: return
        if (!validateFields(binding.tilTitle)) return
        enableUI(false)
        viewModel.postSnapshot(
            title = binding.etTitle.text.toString().trim(),
            imageUri = uri,
            userId = Constants.currentUser.uid,
        )
    }

    private fun validateFields(vararg textFields: TextInputLayout): Boolean {
        var isValid = true
        for (textField in textFields) {
            if (textField.editText?.text.toString().trim().isEmpty()) {
                textField.error = getString(R.string.helper_required)
                isValid = false
            } else {
                textField.error = null
            }
        }
        return isValid
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun enableUI(enable: Boolean) {
        with(binding) {
            btnSelect.isEnabled = enable
            if (enable) btnSelect.visibility = View.VISIBLE
            btnPost.isEnabled = enable
            tilTitle.isEnabled = enable
        }
    }

    override fun showPopUpMessage(resId: Int, duration: Int) {
        hostActivityListener?.showPopUpMessage(resId, duration)
    }

    override fun onSnapshotPosted() {
        hostActivityListener?.onSnapshotPosted()
    }
}
