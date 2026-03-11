package com.hvasoft.dailydose.presentation

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.hvasoft.dailydose.BuildConfig
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.network.model.User
import com.hvasoft.dailydose.databinding.ActivityHostBinding
import com.hvasoft.dailydose.presentation.screens.add.AddFragment
import com.hvasoft.dailydose.presentation.screens.common.HomeFragmentListener
import com.hvasoft.dailydose.presentation.screens.common.HostActivityListener
import com.hvasoft.dailydose.presentation.screens.home.HomeFragment
import com.hvasoft.dailydose.presentation.screens.profile.ProfileFragment
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class HostActivity : AppCompatActivity(), HostActivityListener {

    private var _binding: ActivityHostBinding? = null
    private val binding get() = _binding!!

    private lateinit var activeFragment: Fragment
    private lateinit var authListener: FirebaseAuth.AuthStateListener
    private lateinit var updaterPrefs: SharedPreferences

    private var firebaseAuth: FirebaseAuth? = null
    private var mFragmentManager: FragmentManager? = null
    private val remoteConfig = Firebase.remoteConfig
    private var hasShownUpdateDialog = false

    private val installPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (canInstallUnknownApps()) {
                launchDownloadedApkInstaller()
            } else {
                showPopUpMessage(R.string.update_permission_denied)
            }
        }

    private val updateDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId == getPendingDownloadId()) {
                markDownloadReady(downloadId)
            }
        }
    }

    private val authResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.main_auth_welcome, Toast.LENGTH_SHORT).show()
            } else if (IdpResponse.fromResultIntent(it.data) == null) {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityHostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updaterPrefs = getSharedPreferences(UPDATER_PREFS_NAME, Context.MODE_PRIVATE)
        setupRemoteConfig()
        registerUpdateDownloadReceiver()
        setupAuth()
        checkForAppUpdate()
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        setupAuth()
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth?.addAuthStateListener(authListener)
        promoteSuccessfulPendingDownloadIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth?.removeAuthStateListener(authListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(updateDownloadReceiver)
        _binding = null
    }

    private fun setupRemoteConfig() {
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
            }
        )
    }

    private fun registerUpdateDownloadReceiver() {
        ContextCompat.registerReceiver(
            this,
            updateDownloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun checkForAppUpdate() {
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener {
                val latestVersionCode = remoteConfig.getLong(REMOTE_CONFIG_LATEST_VERSION_CODE_KEY)
                val apkUrl = remoteConfig.getString(REMOTE_CONFIG_APK_URL_KEY)

                if (!hasShownUpdateDialog &&
                    latestVersionCode > BuildConfig.VERSION_CODE &&
                    apkUrl.isNotBlank()
                ) {
                    hasShownUpdateDialog = true
                    showUpdateDialog(apkUrl)
                }
            }
    }

    private fun showUpdateDialog(apkUrl: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.update_dialog_title)
            .setMessage(R.string.update_dialog_message)
            .setPositiveButton(R.string.update_dialog_confirm) { _, _ ->
                downloadUpdateApk(apkUrl)
            }
            .setNegativeButton(R.string.dialog_logout_cancel, null)
            .show()
    }

    private fun downloadUpdateApk(apkUrl: String) {
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setVisibleInDownloadsUi(true)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                LATEST_APK_FILE_NAME
            )
            .setMimeType("application/vnd.android.package-archive")
            .setTitle(getString(R.string.app_name))

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)
        updaterPrefs.edit()
            .putLong(PREF_PENDING_DOWNLOAD_ID, downloadId)
            .remove(PREF_READY_DOWNLOAD_ID)
            .apply()
        showPopUpMessage(R.string.update_download_started)
    }

    private fun launchDownloadedApkInstaller() {
        val apkUri = getReadyDownloadedApkUri() ?: run {
            showPopUpMessage(R.string.update_download_failed)
            return
        }

        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = apkUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (installIntent.resolveActivity(packageManager) != null) {
                startActivity(installIntent)
            } else {
                startActivity(fallbackIntent)
            }
        } catch (_: Exception) {
            showPopUpMessage(R.string.update_install_failed)
            return
        }

        clearReadyDownloadId()
    }

    private fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    private fun setupAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                authResult.launch(
                    AuthUI.getInstance().createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setLogo(R.mipmap.ic_banner)
                        .setTheme(R.style.LoginTheme)
                        .setLockOrientation(true)
                        .setAvailableProviders(
                            listOf(
                                AuthUI.IdpConfig.EmailBuilder().build(),
                                AuthUI.IdpConfig.GoogleBuilder().build()
                            )
                        )
                        .build()
                )
                mFragmentManager = null
            } else {
                Constants.currentUser = auth.currentUser!!
                ensureCurrentUserRecord()

                val fragmentProfile =
                    mFragmentManager?.findFragmentByTag(ProfileFragment::class.java.name)
                fragmentProfile?.let {
                    (it as HomeFragmentListener).onRefresh()
                }

                if (mFragmentManager == null) {
                    mFragmentManager = supportFragmentManager
                    setupBottomNav(mFragmentManager!!)
                }
            }
        }
    }

    private fun ensureCurrentUserRecord() {
        val currentUser = Constants.currentUser
        val userReference = FirebaseDatabase.getInstance()
            .getReference(Constants.USERS_PATH)
            .child(currentUser.uid)

        userReference.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                userReference.setValue(
                    User(
                        userName = currentUser.displayName.orEmpty(),
                        photoUrl = currentUser.photoUrl?.toString().orEmpty()
                    )
                )
            }
        }
    }

    private fun setupBottomNav(fragmentManager: FragmentManager) {
        mFragmentManager?.let {
            for (fragment in it.fragments) {
                it.beginTransaction().remove(fragment!!).commit()
            }
        }

        val homeFragment = HomeFragment()
        val addFragment = AddFragment()
        val profileFragment = ProfileFragment()

        activeFragment = homeFragment

        fragmentManager.beginTransaction()
            .add(R.id.hostFragment, profileFragment, profileFragment::class.java.name)
            .hide(profileFragment)
            .commit()

        fragmentManager.beginTransaction()
            .add(R.id.hostFragment, addFragment, addFragment::class.java.name)
            .hide(addFragment)
            .commit()

        fragmentManager.beginTransaction()
            .add(R.id.hostFragment, homeFragment, HomeFragment::class.java.name)
            .commit()

        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.action_home -> {
                    fragmentManager.beginTransaction().hide(activeFragment).show(homeFragment)
                        .commit()
                    activeFragment = homeFragment
                    true
                }
                R.id.action_add -> {
                    fragmentManager.beginTransaction().hide(activeFragment).show(addFragment)
                        .commit()
                    activeFragment = addFragment
                    true
                }
                R.id.action_profile -> {
                    fragmentManager.beginTransaction().hide(activeFragment).show(profileFragment)
                        .commit()
                    activeFragment = profileFragment
                    true
                }
                else -> false
            }
        }

        binding.bottomNav.setOnItemReselectedListener {
            when (it.itemId) {
                R.id.action_home -> (homeFragment as HomeFragmentListener).onRefresh()
            }
        }
    }

    override fun showPopUpMessage(resId: Int, duration: Int) {
        Snackbar.make(binding.root, resId, duration)
            .setAnchorView(binding.bottomNav)
            .show()
    }

    override fun onSnapshotPosted() {
        val fragmentManager = supportFragmentManager
        val homeFragment =
            fragmentManager.findFragmentByTag(HomeFragment::class.java.name) as HomeFragment?
        if (homeFragment != null) {
            fragmentManager.beginTransaction().hide(activeFragment).show(homeFragment).commit()
            activeFragment = homeFragment
            binding.bottomNav.selectedItemId = R.id.action_home
            homeFragment.onRefresh()
        }
    }

    private fun getPendingDownloadId(): Long? {
        val downloadId = updaterPrefs.getLong(PREF_PENDING_DOWNLOAD_ID, -1L)
        return if (downloadId == -1L) null else downloadId
    }

    private fun markDownloadReady(downloadId: Long) {
        updaterPrefs.edit()
            .remove(PREF_PENDING_DOWNLOAD_ID)
            .putLong(PREF_READY_DOWNLOAD_ID, downloadId)
            .apply()
    }

    private fun clearPendingDownloadId() {
        updaterPrefs.edit().remove(PREF_PENDING_DOWNLOAD_ID).apply()
    }

    private fun promoteSuccessfulPendingDownloadIfNeeded() {
        val pendingDownloadId = getPendingDownloadId() ?: return
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(pendingDownloadId)

        downloadManager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) return
            val status =
                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> markDownloadReady(pendingDownloadId)
                DownloadManager.STATUS_FAILED -> clearPendingDownloadId()
            }
        }
    }

    private fun getReadyDownloadedApkUri(): Uri? {
        val downloadId = updaterPrefs.getLong(PREF_READY_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return null

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)

        downloadManager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) {
                clearReadyDownloadId()
                return null
            }

            val status =
                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                if (status == DownloadManager.STATUS_FAILED) {
                    clearReadyDownloadId()
                }
                return null
            }
        }

        return downloadManager.getUriForDownloadedFile(downloadId)?.also { return it }.run {
            clearReadyDownloadId()
            null
        }
    }

    private fun clearReadyDownloadId() {
        updaterPrefs.edit().remove(PREF_READY_DOWNLOAD_ID).apply()
    }

    companion object {
        private const val REMOTE_CONFIG_LATEST_VERSION_CODE_KEY = "latest_version_code"
        private const val REMOTE_CONFIG_APK_URL_KEY = "apk_url"
        private const val LATEST_APK_FILE_NAME = "dailydose-latest.apk"
        private const val UPDATER_PREFS_NAME = "updater_prefs"
        private const val PREF_PENDING_DOWNLOAD_ID = "pending_download_id"
        private const val PREF_READY_DOWNLOAD_ID = "ready_download_id"
    }
}
