package com.hvasoft.dailydose.presentation.screens.home

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.imageLoader
import coil.request.ImageRequest
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.domain.model.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val FileProviderAuthority = "com.hvasoft.fileprovider"
private const val SharedImagesDirectory = "shared_images"

internal fun shareSnapshotIfAvailable(
    context: Context,
    snapshot: Snapshot,
    hasFullAccess: Boolean,
    onShowMessage: (Int) -> Unit,
    launch: (suspend () -> Unit) -> Unit,
) {
    if (snapshot.canShareImage(hasFullAccess).not()) {
        onShowMessage(R.string.home_share_image_unavailable_offline)
        return
    }

    launch {
        runCatching {
            shareSnapshot(context, snapshot, hasFullAccess)
        }.onFailure {
            onShowMessage(R.string.home_share_image_error)
        }
    }
}

private suspend fun shareSnapshot(
    context: Context,
    snapshot: Snapshot,
    allowRemoteFallback: Boolean,
) {
    val shareText = context.getString(R.string.home_description_button_share, snapshot.title)
    val imageUri = prepareShareableImageUri(
        context = context,
        snapshot = snapshot,
        allowRemoteFallback = allowRemoteFallback,
    )
    val intent = buildShareSnapshotIntent(context, imageUri, shareText)

    context.startActivity(
        Intent.createChooser(
            intent,
            context.getString(R.string.home_description_title_share),
        ),
    )
}

internal suspend fun prepareShareableImageUri(
    context: Context,
    snapshot: Snapshot,
    allowRemoteFallback: Boolean,
): Uri = prepareShareableImageUri(
    context = context,
    snapshot = snapshot,
    allowRemoteFallback = allowRemoteFallback,
    remoteImageWriter = { localContext, imageUrl, targetFile ->
        writeRemoteImageToShareFile(localContext, imageUrl, targetFile)
    },
    fileUriProvider = ::buildShareContentUri,
)

internal suspend fun prepareShareableImageUri(
    context: Context,
    snapshot: Snapshot,
    allowRemoteFallback: Boolean,
    remoteImageWriter: suspend (Context, String, File) -> Unit,
    fileUriProvider: (Context, File) -> Uri,
): Uri = withContext(Dispatchers.IO) {
    val localImageFile = snapshot.localPhotoPath
        ?.let(::File)
        ?.takeIf(File::exists)
    val shareFile = buildShareCacheFile(context, snapshot.snapshotKey)

    when {
        localImageFile != null -> localImageFile.copyTo(shareFile, overwrite = true)
        allowRemoteFallback && snapshot.photoUrl.isNullOrBlank().not() -> {
            remoteImageWriter(context, snapshot.photoUrl.orEmpty(), shareFile)
        }

        else -> throw IllegalStateException("No image available to share")
    }

    fileUriProvider(context, shareFile)
}

private fun buildShareSnapshotIntent(
    context: Context,
    imageUri: Uri,
    shareText: String,
): Intent = Intent(Intent.ACTION_SEND).apply {
    type = "image/*"
    putExtra(Intent.EXTRA_STREAM, imageUri)
    putExtra(Intent.EXTRA_TEXT, shareText)
    clipData = ClipData.newUri(context.contentResolver, "snapshot_image", imageUri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

internal fun Snapshot.canShareImage(allowRemoteFallback: Boolean): Boolean =
    hasRetainedMainImage() || (allowRemoteFallback && photoUrl.isNullOrBlank().not())

private fun buildShareCacheFile(context: Context, snapshotKey: String): File {
    val shareDirectory = File(context.cacheDir, SharedImagesDirectory).apply {
        mkdirs()
    }
    val safeSnapshotKey = snapshotKey
        .ifBlank { "snapshot" }
        .replace("[^a-zA-Z0-9-_]".toRegex(), "_")
    return File(shareDirectory, "$safeSnapshotKey.jpg")
}

private suspend fun writeRemoteImageToShareFile(
    context: Context,
    imageUrl: String,
    targetFile: File,
) {
    val request = ImageRequest.Builder(context)
        .data(imageUrl.toUri())
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request)
    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
        ?: error("Unable to decode image")

    withContext(Dispatchers.IO) {
        FileOutputStream(targetFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }
    }
}

private fun buildShareContentUri(context: Context, shareFile: File): Uri =
    FileProvider.getUriForFile(context, FileProviderAuthority, shareFile)
