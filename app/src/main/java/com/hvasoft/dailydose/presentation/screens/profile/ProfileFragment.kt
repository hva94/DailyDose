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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.auth.AuthUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.network.model.User
import com.hvasoft.dailydose.data.utils.Constants
import com.hvasoft.dailydose.databinding.FragmentProfileBinding
import com.hvasoft.dailydose.presentation.screens.utils.FragmentAux
import com.hvasoft.dailydose.presentation.screens.utils.MainAux
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ProfileFragment : Fragment(), FragmentAux {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var snapshotsStorageRef: StorageReference
    private lateinit var usersDatabaseRef: DatabaseReference

    private var mainAux: MainAux? = null
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

        refresh()
        setupButtons()
        setupFirebase()
        showUserImageProfile()
    }

    private fun setupButtons() {
        with(binding) {
            btnSelect.setOnClickListener { selectImage(btnSelect.context) }
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
        mainAux = activity as MainAux
    }

    private fun signOut() {
        context.let {
            AuthUI.getInstance().signOut(it)
                .addOnCompleteListener {
                    Toast.makeText(context, R.string.profile_logout_success, Toast.LENGTH_SHORT)
                        .show()
                    binding.tvName.text = ""
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
        val openGalleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        selectImageResult.launch(openGalleryIntent)
        showUserImageProfile()
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
                            Constants.currentUser.uid,
                            Constants.currentUser.displayName.toString().trim(),
                            downloadUri.toString()
                        )
                    }
                }
                .addOnFailureListener {
                    mainAux?.showMessage(R.string.post_message_post_image_fail)
                }
        }
    }

    private fun saveUserProfileData(key: String, userName: String, url: String) {
        val user = User(userName = userName, photoUrl = url)
        usersDatabaseRef.child(key).setValue(user)
            .addOnSuccessListener {
                mainAux?.showMessage(R.string.profile_user_image_updated)
            }
            .addOnFailureListener { mainAux?.showMessage(R.string.profile_user_image_failed) }
    }

    private fun showUserImageProfile() {
        usersDatabaseRef
            .child(Constants.currentUser.uid)
            .get().addOnSuccessListener {
                if (it.exists()) {
                    val photoUrl = it.child("photoUrl").value.toString()
                    Glide.with(context)
                        .load(photoUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .circleCrop()
                        .into(binding.imgPhoto)
                }
            }.addOnFailureListener {
                mainAux?.showMessage(R.string.home_database_access_error)
            }
    }

    /**
     *   FragmentAux
     * */
    override fun refresh() {
        with(binding) {
            tvName.text = Constants.currentUser.displayName
            tvEmail.text = Constants.currentUser.email
        }
    }
}