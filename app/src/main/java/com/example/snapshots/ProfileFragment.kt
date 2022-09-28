package com.example.snapshots

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.snapshots.databinding.FragmentProfileBinding
import com.firebase.ui.auth.AuthUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileFragment : Fragment(), FragmentAux {

    private lateinit var mBinding: FragmentProfileBinding

    //INICIO FOTO DE PERFIL
    private lateinit var mSnapshotsStorageRef: StorageReference
    private lateinit var mUsersDatabaseRef: DatabaseReference

    private var mainAux: MainAux? = null
    private var mPhotoSelectedUri: Uri? = null

    private lateinit var mContext: Context
    //FIN FOTO DE PERFIL

    //Appcompat Actualización
    private val selectImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                mPhotoSelectedUri = it.data?.data
                with(mBinding) {
                    imgPhoto.setImageURI(mPhotoSelectedUri)
                    postUserImageProfile()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        mContext = requireContext()
        mBinding = FragmentProfileBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refresh()
        setupButtons()
        setupFirebase()
        showUserImageProfile()
    }

    private fun setupButtons() {
        with(mBinding) {
            btnSelect.setOnClickListener { selectImage(btnSelect.context) }
            btnLogout.setOnClickListener {
                context?.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle(R.string.dialog_logout_title)
                        .setPositiveButton(R.string.dialog_logout_confirm) { _, _ ->
                            signOut()
                            requireActivity().finish()
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
        context?.let {
            AuthUI.getInstance().signOut(it)
                .addOnCompleteListener {
                    Toast.makeText(context, R.string.profile_logout_success, Toast.LENGTH_SHORT).show()
                    mBinding.tvName.text = ""
                    mBinding.tvEmail.text = ""
                    mBinding.imgPhoto.setImageResource(0)
                    (activity?.findViewById(R.id.bottomNav) as?
                            BottomNavigationView)?.selectedItemId = R.id.action_home
                }
        }
    }

    //INICIA FOTO DE PERFIL USUARIO
    //Seleccionar Imagen, ya sea con la cámara o con galeria
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
        mSnapshotsStorageRef =
            FirebaseStorage.getInstance().reference.child(SnapshotsApplication.PATH_SNAPSHOTS)
        mUsersDatabaseRef =
            FirebaseDatabase.getInstance().reference.child(SnapshotsApplication.PATH_SNAPSHOTS_USERS)
    }

    private fun openCamera() {
        mainAux?.showMessage(R.string.snackbar_message_developer)
    }

    private fun openGallery() {
        val openGalleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectImageResult.launch(openGalleryIntent)
        showUserImageProfile()
    }

    private fun postUserImageProfile() {
        if (mPhotoSelectedUri != null) {
            mBinding.progressBar.visibility = View.VISIBLE

            val fileName = "userImageProfile"
            val myStorageRef = mSnapshotsStorageRef.child(SnapshotsApplication.currentUser.uid)
                .child(fileName)

            myStorageRef.putFile(mPhotoSelectedUri!!)
                .addOnProgressListener {
                    val progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                    with(mBinding) {
                        progressBar.progress = progress
                    }
                }
                .addOnCompleteListener {
                    mBinding.progressBar.visibility = View.INVISIBLE
                }
                .addOnSuccessListener {
                    it.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveUserProfileData(SnapshotsApplication.currentUser.uid,
                            SnapshotsApplication.currentUser.displayName.toString().trim(),
                            downloadUri.toString())
                    }
                }
                .addOnFailureListener {
                    mainAux?.showMessage(R.string.post_message_post_image_fail)
                }
        }
    }

    private fun saveUserProfileData(key: String, userName:String, url: String) {
        val snapshotUser = SnapshotUser(userName = userName, photoUrl = url)
        mUsersDatabaseRef.child(key).setValue(snapshotUser)
            .addOnSuccessListener {
                mainAux?.showMessage(R.string.profile_user_image_updated)
            }
            .addOnFailureListener { mainAux?.showMessage(R.string.profile_user_image_failed) }
    }

    private fun showUserImageProfile() {
        mUsersDatabaseRef
            .child(SnapshotsApplication.currentUser.uid)
            .get().addOnSuccessListener {
                if (it.exists()) {
                    val photoUrl = it.child("photoUrl").value.toString()
                    Glide.with(mContext)
                        .load(photoUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .circleCrop()
                        .into(mBinding.imgPhoto)
                }/* else {
                    mainAux?.showMessage(R.string.home_not_found_user_error)
                }*/
            }.addOnFailureListener {
                mainAux?.showMessage(R.string.home_database_access_error)
            }
    }

/*    private fun saveUserName() {
        mainAux?.showMessage(R.string.snackbar_message_developer)
        val snapshotUser = SnapshotUser(userName = SnapshotsApplication.currentUser.displayName.toString())
        mUsersDatabaseRef.child(SnapshotsApplication.currentUser.uid).setValue(snapshotUser)
            .addOnSuccessListener {
                mainAux?.showMessage(R.string.profile_user_image_updated)
            }
            .addOnFailureListener { mainAux?.showMessage(R.string.profile_user_image_failed) }
    }*/

    //FINALIZA FOTO DE PERFIL USUARIO

    /*
    *   FragmentAux
    * */
    override fun refresh() {
        with(mBinding) {
            tvName.text = SnapshotsApplication.currentUser.displayName
            tvEmail.text = SnapshotsApplication.currentUser.email
        }
        // saveUserName()
    }
}