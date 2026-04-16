package com.hvasoft.dailydose.presentation

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.hvasoft.dailydose.BuildConfig
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.local.ProfileLocalCache
import com.hvasoft.dailydose.data.network.model.User
import com.hvasoft.dailydose.presentation.screens.add.AddRoute
import com.hvasoft.dailydose.presentation.screens.add.AddViewModel
import com.hvasoft.dailydose.presentation.screens.home.HomeRoute
import com.hvasoft.dailydose.presentation.screens.home.HomeViewModel
import com.hvasoft.dailydose.presentation.screens.profile.ProfileRoute
import com.hvasoft.dailydose.presentation.screens.profile.ProfileViewModel
import com.hvasoft.dailydose.presentation.theme.DailyDoseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HostActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val addViewModel: AddViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    private lateinit var authListener: FirebaseAuth.AuthStateListener
    private lateinit var updaterPrefs: SharedPreferences

    private var firebaseAuth: FirebaseAuth? = null
    private val remoteConfig = Firebase.remoteConfig
    private var hasShownUpdateDialog = false
    private var isAuthFlowInProgress = false
    private var lastAuthenticatedUserId: String? = null

    private var selectedDestination by mutableStateOf(MainDestination.HOME)
    private var homeScrollSignal by mutableIntStateOf(0)
    private var profileRefreshSignal by mutableIntStateOf(0)
    private var pendingUpdateApkUrl by mutableStateOf<String?>(null)
    private var snackbarRequest by mutableStateOf<SnackbarRequest?>(null)

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
            isAuthFlowInProgress = false
            if (it.resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.main_auth_welcome, Toast.LENGTH_SHORT).show()
            } else if (IdpResponse.fromResultIntent(it.data) == null) {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updaterPrefs = getSharedPreferences(UPDATER_PREFS_NAME, Context.MODE_PRIVATE)
        setContent {
            DailyDoseTheme {
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(snackbarRequest) {
                    snackbarRequest?.let { request ->
                        snackbarHostState.showSnackbar(
                            message = getString(request.messageRes),
                            duration = request.duration,
                        )
                        snackbarRequest = null
                    }
                }

                HostContent(
                    selectedDestination = selectedDestination,
                    snackbarHostState = snackbarHostState,
                    onDestinationSelected = ::onDestinationSelected,
                    homeContent = {
                        HomeRoute(
                            viewModel = homeViewModel,
                            scrollSignal = homeScrollSignal,
                            modifier = Modifier.fillMaxSize(),
                            onShowMessage = ::showPopUpMessage,
                        )
                    },
                    addContent = {
                        AddRoute(
                            viewModel = addViewModel,
                            modifier = Modifier.fillMaxSize(),
                            onShowMessage = ::showPopUpMessage,
                            onSnapshotPosted = { snapshot ->
                                homeViewModel.cachePostedSnapshot(snapshot)
                                selectedDestination = MainDestination.HOME
                            },
                        )
                    },
                    profileContent = {
                        ProfileRoute(
                            viewModel = profileViewModel,
                            refreshSignal = profileRefreshSignal,
                            modifier = Modifier.fillMaxSize(),
                            onShowMessage = ::showPopUpMessage,
                            onSignedOut = {
                                selectedDestination = MainDestination.HOME
                            },
                        )
                    },
                )

                pendingUpdateApkUrl?.let { apkUrl ->
                    UpdateDialog(
                        onConfirm = {
                            pendingUpdateApkUrl = null
                            downloadUpdateApk(apkUrl)
                        },
                        onDismiss = { pendingUpdateApkUrl = null },
                    )
                }
            }
        }
        setupRemoteConfig()
        registerUpdateDownloadReceiver()
        setupAuth()
        checkForAppUpdate()
    }

    override fun onResume() {
        super.onResume()
        maybeRefreshHomeOnResume()
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
            ContextCompat.RECEIVER_NOT_EXPORTED,
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
                    pendingUpdateApkUrl = apkUrl
                }
            }
    }

    private fun downloadUpdateApk(apkUrl: String) {
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setVisibleInDownloadsUi(true)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                LATEST_APK_FILE_NAME,
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

    private fun setupAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                lastAuthenticatedUserId?.let(homeViewModel::clearOfflineSnapshots)
                lastAuthenticatedUserId = null
                profileViewModel.clearProfileState()
                selectedDestination = MainDestination.HOME
                if (!isAuthFlowInProgress) {
                    isAuthFlowInProgress = true
                    authResult.launch(
                        AuthUI.getInstance().createSignInIntentBuilder()
                            .setIsSmartLockEnabled(false)
                            .setLogo(R.mipmap.ic_banner)
                            .setTheme(R.style.LoginTheme)
                            .setLockOrientation(true)
                            .setAvailableProviders(
                                listOf(
                                    AuthUI.IdpConfig.EmailBuilder().build(),
                                    AuthUI.IdpConfig.GoogleBuilder().build(),
                                )
                            )
                            .build()
                    )
                }
            } else {
                val currentUser = auth.currentUser ?: return@AuthStateListener
                val currentUserId = currentUser.uid
                val previousUserId = lastAuthenticatedUserId
                if (previousUserId != null && previousUserId != currentUserId) {
                    homeViewModel.clearOfflineSnapshots(previousUserId)
                }
                lastAuthenticatedUserId = currentUserId
                profileViewModel.seedSignedInUser(
                    userId = currentUserId,
                    displayName = currentUser.displayName.orEmpty(),
                    email = currentUser.email.orEmpty(),
                    photoUrl = currentUser.photoUrl?.toString().orEmpty(),
                )
                ensureCurrentUserRecord(currentUser)
                profileViewModel.loadCurrentProfile()
                if (previousUserId != currentUserId) {
                    homeViewModel.fetchSnapshots()
                    profileRefreshSignal += 1
                }
            }
        }
    }

    private fun ensureCurrentUserRecord(currentUser: com.google.firebase.auth.FirebaseUser) {
        val authFallbackDisplayName = currentUser.displayName
            ?.takeIf(String::isNotBlank)
            ?: currentUser.email
                ?.substringBefore('@')
                .orEmpty()
        val authFallbackPhotoUrl = currentUser.photoUrl?.toString().orEmpty()
        val authFallbackEmail = currentUser.email.orEmpty()
        getSharedPreferences(ProfileLocalCache.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(ProfileLocalCache.displayNameKey(currentUser.uid), authFallbackDisplayName)
            .putString(ProfileLocalCache.photoUrlKey(currentUser.uid), authFallbackPhotoUrl)
            .putString(ProfileLocalCache.emailKey(currentUser.uid), authFallbackEmail)
            .apply()

        val userReference = FirebaseDatabase.getInstance()
            .getReference(Constants.USERS_PATH)
            .child(currentUser.uid)

        userReference.get().addOnSuccessListener { snapshot ->
            val currentUserRecord = snapshot.getValue(User::class.java)
            val resolvedDisplayName = currentUserRecord?.userName
                ?.takeIf(String::isNotBlank)
                ?: currentUser.displayName.orEmpty()
            val resolvedPhotoUrl = currentUserRecord?.photoUrl
                ?.takeIf(String::isNotBlank)
                ?: currentUser.photoUrl?.toString().orEmpty()

            getSharedPreferences(ProfileLocalCache.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(ProfileLocalCache.displayNameKey(currentUser.uid), resolvedDisplayName)
                .putString(ProfileLocalCache.photoUrlKey(currentUser.uid), resolvedPhotoUrl)
                .putString(ProfileLocalCache.emailKey(currentUser.uid), authFallbackEmail)
                .apply()

            if (!snapshot.exists()) {
                userReference.setValue(
                    User(
                        userName = resolvedDisplayName,
                        photoUrl = resolvedPhotoUrl,
                    )
                )
            }
        }
    }

    private fun onDestinationSelected(destination: MainDestination) {
        val wasSelected = selectedDestination == destination
        selectedDestination = destination

        when (destination) {
            MainDestination.HOME -> {
                if (wasSelected) {
                    homeScrollSignal += 1
                }
            }

            MainDestination.ADD -> Unit
            MainDestination.PROFILE -> {
                if (!wasSelected) {
                    profileRefreshSignal += 1
                }
            }
        }
    }

    private fun showPopUpMessage(@StringRes resId: Int) {
        snackbarRequest = SnackbarRequest(
            messageRes = resId,
            duration = SnackbarDuration.Short,
        )
    }

    private fun maybeRefreshHomeOnResume() {
        val currentUserId = firebaseAuth?.currentUser?.uid ?: return
        if (currentUserId != lastAuthenticatedUserId) return
        if (selectedDestination != MainDestination.HOME) return
        if (homeViewModel.shouldAutoRefreshOnResume(System.currentTimeMillis())) {
            homeViewModel.fetchSnapshots(reloadSource = false)
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

    companion object {
        private const val REMOTE_CONFIG_LATEST_VERSION_CODE_KEY = "latest_version_code"
        private const val REMOTE_CONFIG_APK_URL_KEY = "apk_url"
        private const val LATEST_APK_FILE_NAME = "dailydose-latest.apk"
        private const val UPDATER_PREFS_NAME = "updater_prefs"
        private const val PREF_PENDING_DOWNLOAD_ID = "pending_download_id"
        private const val PREF_READY_DOWNLOAD_ID = "ready_download_id"
    }
}

@Composable
private fun HostContent(
    selectedDestination: MainDestination,
    snackbarHostState: SnackbarHostState,
    onDestinationSelected: (MainDestination) -> Unit,
    homeContent: @Composable () -> Unit,
    addContent: @Composable () -> Unit,
    profileContent: @Composable () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedDestination == destination,
                        onClick = { onDestinationSelected(destination) },
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        label = { Text(text = stringResource(destination.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        Box(modifier = contentModifier.fillMaxSize()) {
            when (selectedDestination) {
                MainDestination.HOME -> {
                    homeContent()
                }

                MainDestination.ADD -> {
                    addContent()
                }

                MainDestination.PROFILE -> {
                    profileContent()
                }
            }
        }
    }
}

@Composable
private fun UpdateDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.update_dialog_title)) },
        text = { Text(text = stringResource(R.string.update_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.update_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_logout_cancel))
            }
        },
    )
}

private data class SnackbarRequest(
    @StringRes val messageRes: Int,
    val duration: SnackbarDuration,
)
