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
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.network.model.SnapshotDTO
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

    private lateinit var snapshotsDatabaseRef: DatabaseReference
    private lateinit var snapshotsStorageRef: StorageReference
    private lateinit var imageFile: File

    private var hostActivityListener: HostActivityListener? = null
    private var imageSelectedUri: Uri? = null

    private val selectImageResult =
        registerForActivityResult(StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                if (activityResult.data?.data != null) imageSelectedUri = activityResult.data?.data
                with(binding) {
                    imgPhoto.setImageURI(imageSelectedUri)
                    tilTitle.visibility = View.VISIBLE
                    val etTitleString =
                        getString(R.string.add_default_title, getCurrentTimeString())
                    etTitle.setText(etTitleString)
                    tvMessage.text = getString(R.string.post_message_valid_title)
                    btnSelect.visibility = View.GONE
                }
            }
        }

    private fun getCurrentTimeString(): String {
        val timeInMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        return dateFormat.format(Date(timeInMillis))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTextField()
        setupButtons()
        setupFirebase()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            hostActivityListener = context as HostActivityListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnSnapshotPostedListener")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setupTextField() {
        with(binding) {
            etTitle.addTextChangedListener { validateFields(tilTitle) }
        }
    }

    private fun setupButtons() {
        with(binding) {
            btnPost.setOnClickListener { if (validateFields(tilTitle)) postSnapshot() }
            btnSelect.setOnClickListener { selectImage(btnSelect.context) }
        }
    }

    private fun setupFirebase() {
        snapshotsStorageRef =
            FirebaseStorage.getInstance().reference.child(Constants.SNAPSHOTS_PATH)
        snapshotsDatabaseRef =
            FirebaseDatabase.getInstance().reference.child(Constants.SNAPSHOTS_PATH)
    }

    private fun selectImage(context: Context) {
        FirebaseDatabase.getInstance()
            .getReference(Constants.USERS_PATH)
            .child(Constants.currentUser.uid)
            .get().addOnSuccessListener {
                if (it.exists()) {
                    val items = resources.getStringArray(R.array.array_options_item)
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.dialog_options_title)
                        .setItems(items) { _, item ->
                            when (item) {
                                0 -> openCamera()

                                1 -> openGallery()
                            }
                        }
                        .show()
                } else {
                    hostActivityListener?.showPopUpMessage(R.string.home_not_found_user_data)
                }
            }.addOnFailureListener {
                hostActivityListener?.showPopUpMessage(R.string.home_database_access_error)
            }
    }

    private fun openCamera() {
        lifecycleScope.launch {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            imageFile = getPhotoFile()
            val fileProvider = FileProvider.getUriForFile(
                requireActivity().baseContext,
                "com.hvasoft.fileprovider",
                imageFile
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
        val openGalleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        selectImageResult.launch(openGalleryIntent)
    }

    private fun postSnapshot() {
        if (imageSelectedUri != null) {
            enableUI(false)
            binding.progressBar.visibility = View.VISIBLE

            val key = snapshotsDatabaseRef.push().key!!
            val myStorageRef = snapshotsStorageRef.child(Constants.currentUser.uid)
                .child(key)

            myStorageRef.putFile(imageSelectedUri!!)
                .addOnProgressListener {
                    val progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                    with(binding) {
                        progressBar.progress = progress
                        tvMessage.text = String.format("%s%%", progress)
                    }
                }
                .addOnCompleteListener {
                    binding.progressBar.visibility = View.INVISIBLE
                }
                .addOnSuccessListener {
                    it.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveSnapshot(
                            key,
                            downloadUri.toString(),
                            binding.etTitle.text.toString().trim(),
                            System.currentTimeMillis(),
                            Constants.currentUser.uid
                        )
                    }
                }
                .addOnFailureListener {
                    hostActivityListener?.showPopUpMessage(R.string.post_message_post_image_fail)
                }
        }
    }

    private fun saveSnapshot(
        key: String,
        url: String,
        title: String,
        dateTime: Long,
        userId: String
    ) {
        val snapshot = SnapshotDTO(
            idUserOwner = userId,
            title = title,
            dateTime = dateTime,
            photoUrl = url
        )
        snapshotsDatabaseRef.child(key).setValue(snapshot)
            .addOnSuccessListener {
                hideKeyboard()
                hostActivityListener?.showPopUpMessage(R.string.post_message_post_success)
                with(binding) {
                    tilTitle.visibility = View.GONE
                    etTitle.setText("")
                    tilTitle.error = null
                    tvMessage.text = getString(R.string.post_message_title)
                    imgPhoto.setImageDrawable(null)
                }
                hostActivityListener?.onSnapshotPosted()
            }
            .addOnCompleteListener { enableUI(true) }
            .addOnFailureListener { hostActivityListener?.showPopUpMessage(R.string.post_message_post_fail) }

    }

    private fun validateFields(vararg textFields: TextInputLayout): Boolean {
        var isValid = true

        for (textField in textFields) {
            if (textField.editText?.text.toString().trim().isEmpty()) {
                textField.error = getString(R.string.helper_required)
                isValid = false
            } else textField.error = null
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
