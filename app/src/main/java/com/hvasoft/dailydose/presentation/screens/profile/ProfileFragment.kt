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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.auth.AuthUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.network.model.User
import com.hvasoft.dailydose.databinding.FragmentProfileBinding
import com.hvasoft.dailydose.presentation.screens.common.HomeFragmentListener
import com.hvasoft.dailydose.presentation.screens.common.HostActivityListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ProfileFragment : Fragment(), HomeFragmentListener {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var snapshotsStorageRef: StorageReference
    private lateinit var usersDatabaseRef: DatabaseReference

    private var hostActivityListener: HostActivityListener? = null
    private var imageSelectedUri: Uri? = null

    private lateinit var context: Context
    private lateinit var photoFile: File

    private val selectImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                if (it.data?.data != null) imageSelectedUri = it.data?.data
                with(binding) {
                    imgPhoto.setImageURI(imageSelectedUri)
                    postUserImageProfile()
                }
            }
        }

    private val pickImageResult =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                imageSelectedUri = uri
                with(binding) {
                    imgPhoto.setImageURI(imageSelectedUri)
                    postUserImageProfile()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        context = requireContext()
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onRefresh()
        setupButtons()
        setupFirebase()
        loadUserProfile()
    }

    private fun setupButtons() {
        with(binding) {
            btnSelect.setOnClickListener { selectImage(btnSelect.context) }
            btnEditName.setOnClickListener { showNameEditor(true) }
            btnCancelName.setOnClickListener { cancelNameEditing() }
            btnSaveName.setOnClickListener { updateUserName() }
            btnLogout.setOnClickListener {
                context.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle(R.string.dialog_logout_title)
                        .setPositiveButton(R.string.dialog_logout_confirm) { _, _ ->
                            signOut()
                        }

                        .setNegativeButton(R.string.dialog_logout_cancel, null)
                        .show()
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        hostActivityListener = activity as HostActivityListener
    }

    private fun signOut() {
        context.let {
            AuthUI.getInstance().signOut(it)
                .addOnCompleteListener {
                    Toast.makeText(context, R.string.profile_logout_success, Toast.LENGTH_SHORT)
                        .show()
                    binding.tvName.text = ""
                    binding.etName.setText("")
                    showNameEditor(true)
                    binding.tvEmail.text = ""
                    binding.imgPhoto.setImageResource(0)
                    (activity?.findViewById(R.id.bottomNav) as?
                            BottomNavigationView)?.selectedItemId = R.id.action_home
                }
        }
    }

    private fun selectImage(context: Context) {
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
    }

    private fun setupFirebase() {
        snapshotsStorageRef =
            FirebaseStorage.getInstance().reference.child(Constants.SNAPSHOTS_PATH)
        usersDatabaseRef =
            FirebaseDatabase.getInstance().reference.child(Constants.USERS_PATH)
    }

    private fun openCamera() {
        lifecycleScope.launch {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photoFile = getPhotoFile()
            val fileProvider = FileProvider.getUriForFile(
                requireActivity().baseContext,
                "com.hvasoft.fileprovider",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
            imageSelectedUri = fileProvider
            selectImageResult.launch(takePictureIntent)
        }
    }

    private fun getPhotoFile(): File {
        val storageDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("photo", ".jpg", storageDirectory)
    }

    private fun openGallery() {
        pickImageResult.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun postUserImageProfile() {
        if (imageSelectedUri != null) {
            binding.progressBar.visibility = View.VISIBLE

            val fileName = "userImageProfile"
            val myStorageRef = snapshotsStorageRef.child(Constants.currentUser.uid)
                .child(fileName)

            myStorageRef.putFile(imageSelectedUri!!)
                .addOnProgressListener {
                    val progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                    with(binding) {
                        progressBar.progress = progress
                    }
                }
                .addOnCompleteListener {
                    binding.progressBar.visibility = View.INVISIBLE
                }
                .addOnSuccessListener {
                    it.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveUserProfileData(
                            userName = binding.etName.text.toString().trim(),
                            photoUrl = downloadUri.toString(),
                            successMessageRes = R.string.profile_user_image_updated,
                            failureMessageRes = R.string.profile_user_image_failed
                        )
                    }
                }
                .addOnFailureListener {
                    hostActivityListener?.showPopUpMessage(R.string.post_message_post_image_fail)
                }
        }
    }

    private fun updateUserName() {
        val newName = binding.etName.text.toString().trim()
        if (newName.isEmpty()) {
            binding.tilName.error = getString(R.string.profile_name_empty_error)
            return
        }

        binding.tilName.error = null
        binding.progressBar.visibility = View.VISIBLE

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        currentUser.updateProfile(profileUpdates)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: IllegalStateException("Failed to update user profile")
                }
                saveUserProfileDataTask(
                    userName = newName,
                    photoUrl = null
                )
            }
            .addOnSuccessListener {
                Constants.currentUser = currentUser
                binding.progressBar.visibility = View.INVISIBLE
                binding.tvName.text = newName
                showNameEditor(false)
                hideKeyboard()
                hostActivityListener?.showPopUpMessage(R.string.profile_name_updated)
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.INVISIBLE
                hostActivityListener?.showPopUpMessage(R.string.profile_database_write_error)
            }
    }

    private fun saveUserProfileData(
        userName: String? = null,
        photoUrl: String? = null,
        successMessageRes: Int,
        failureMessageRes: Int
    ) {
        saveUserProfileDataTask(userName, photoUrl)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.INVISIBLE
                if (!userName.isNullOrEmpty()) {
                    binding.tvName.text = userName
                }
                if (photoUrl != null) {
                    showUserImageProfile(photoUrl)
                }
                hostActivityListener?.showPopUpMessage(successMessageRes)
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.INVISIBLE
                hostActivityListener?.showPopUpMessage(failureMessageRes)
            }
    }

    private fun saveUserProfileDataTask(
        userName: String? = null,
        photoUrl: String? = null
    ) = usersDatabaseRef.child(Constants.currentUser.uid)
        .get()
        .continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: IllegalStateException("Failed to load user data")
            }

            val currentUserData = task.result?.getValue(User::class.java) ?: User()
            val updatedUser = currentUserData.copy(
                userName = userName ?: currentUserData.userName.ifEmpty {
                    Constants.currentUser.displayName.orEmpty()
                },
                photoUrl = photoUrl ?: currentUserData.photoUrl
            )

            usersDatabaseRef.child(Constants.currentUser.uid).setValue(updatedUser)
        }

    private fun loadUserProfile() {
        usersDatabaseRef
            .child(Constants.currentUser.uid)
            .get().addOnSuccessListener {
                val user = it.getValue(User::class.java)
                val userName = user?.userName?.ifBlank {
                    Constants.currentUser.displayName.orEmpty()
                }.orEmpty()
                binding.tvName.text = userName
                binding.etName.setText(userName)
                showNameEditor(userName.isBlank())
                showUserImageProfile(user?.photoUrl.orEmpty())
            }.addOnFailureListener {
                hostActivityListener?.showPopUpMessage(R.string.home_database_access_error)
            }
    }

    private fun showNameEditor(show: Boolean) {
        with(binding) {
            tilName.isVisible = show
            btnCancelName.isVisible = show
            btnSaveName.isVisible = show
            btnEditName.isVisible = !show
            if (show) {
                etName.requestFocus()
            } else {
                tilName.error = null
            }
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

        Glide.with(context)
            .load(photoUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .circleCrop()
            .into(binding.imgPhoto)
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    /**
     *   FragmentAux
     * */
    override fun onRefresh() {
        with(binding) {
            val displayName = binding.etName.text?.toString()?.ifBlank {
                Constants.currentUser.displayName.orEmpty()
            } ?: Constants.currentUser.displayName.orEmpty()
            tvName.text = displayName
            etName.setText(displayName)
            showNameEditor(displayName.isBlank())
            tvEmail.text = Constants.currentUser.email
        }
    }
}
