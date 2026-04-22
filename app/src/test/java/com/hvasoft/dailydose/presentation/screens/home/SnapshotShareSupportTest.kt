package com.hvasoft.dailydose.presentation.screens.home

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotVisibilityMode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalCoroutinesApi::class)
class SnapshotShareSupportTest {

    @Test
    fun `prepareShareableImageUri copies retained local image into share cache`() = runTest {
        val tempDir = createTempDirectory("share-local").toFile()
        val sourceFile = File(tempDir, "source.jpg").apply {
            writeText("local-image")
        }
        val context = mockContext(cacheDir = tempDir)
        val expectedUri = mockk<Uri>()
        var sharedFile: File? = null

        val result = prepareShareableImageUri(
            context = context,
            snapshot = Snapshot(
                snapshotKey = "snapshot-local",
                localPhotoPath = sourceFile.absolutePath,
            ),
            allowRemoteFallback = false,
            remoteImageWriter = { _, _, _ -> error("Remote writer should not be used") },
            fileUriProvider = { _, file ->
                sharedFile = file
                expectedUri
            },
        )

        assertThat(result).isEqualTo(expectedUri)
        assertThat(sharedFile).isNotNull()
        assertThat(sharedFile!!.parentFile?.name).isEqualTo("shared_images")
        assertThat(sharedFile!!.absolutePath).isNotEqualTo(sourceFile.absolutePath)
        assertThat(sharedFile!!.readText()).isEqualTo("local-image")
    }

    @Test
    fun `prepareShareableImageUri writes remote image into share cache when fallback is allowed`() = runTest {
        val tempDir = createTempDirectory("share-remote").toFile()
        val context = mockContext(cacheDir = tempDir)
        val expectedUri = mockk<Uri>()
        var sharedFile: File? = null

        val result = prepareShareableImageUri(
            context = context,
            snapshot = Snapshot(
                snapshotKey = "snapshot-remote",
                photoUrl = "https://example.com/image.jpg",
            ),
            allowRemoteFallback = true,
            remoteImageWriter = { _, _, file ->
                file.writeText("remote-image")
            },
            fileUriProvider = { _, file ->
                sharedFile = file
                expectedUri
            },
        )

        assertThat(result).isEqualTo(expectedUri)
        assertThat(sharedFile).isNotNull()
        assertThat(sharedFile!!.parentFile?.name).isEqualTo("shared_images")
        assertThat(sharedFile!!.readText()).isEqualTo("remote-image")
    }

    @Test
    fun `prepareShareableImageUri fails when image is unavailable offline`() = runTest {
        val tempDir = createTempDirectory("share-missing").toFile()
        val context = mockContext(cacheDir = tempDir)

        val failure = runCatching {
            prepareShareableImageUri(
                context = context,
                snapshot = Snapshot(snapshotKey = "snapshot-missing"),
                allowRemoteFallback = false,
            )
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `canShareImage only returns true when a real local or remote source exists`() {
        val localFile = createTempDirectory("share-available").toFile().resolve("local.jpg").apply {
            writeText("local")
        }
        val localSnapshot = Snapshot(localPhotoPath = localFile.absolutePath)
        val remoteSnapshot = Snapshot(photoUrl = "https://example.com/image.jpg")
        val missingSnapshot = Snapshot()

        assertThat(localSnapshot.canShareImage(allowRemoteFallback = false)).isTrue()
        assertThat(remoteSnapshot.canShareImage(allowRemoteFallback = true)).isTrue()
        assertThat(remoteSnapshot.canShareImage(allowRemoteFallback = false)).isFalse()
        assertThat(missingSnapshot.canShareImage(allowRemoteFallback = true)).isFalse()
    }

    @Test
    fun `canShareImage blocks hidden posts for the current viewer`() {
        val localFile = createTempDirectory("share-hidden").toFile().resolve("local.jpg").apply {
            writeText("local")
        }
        val hiddenSnapshot = Snapshot(
            snapshotKey = "snapshot-hidden",
            localPhotoPath = localFile.absolutePath,
            idUserOwner = "owner-2",
            visibilityMode = SnapshotVisibilityMode.HIDDEN_UNREVEALED,
        )
        val revealedSnapshot = hiddenSnapshot.copy(
            visibilityMode = SnapshotVisibilityMode.VISIBLE_REVEALED,
            isRevealedForViewer = true,
        )

        assertThat(hiddenSnapshot.canShareImage(allowRemoteFallback = false, currentUserId = "viewer-1")).isFalse()
        assertThat(revealedSnapshot.canShareImage(allowRemoteFallback = false, currentUserId = "viewer-1")).isTrue()
    }

    private fun mockContext(cacheDir: File): Context {
        val context = mockk<Context>(relaxed = true)
        val contentResolver = mockk<ContentResolver>(relaxed = true)
        every { context.cacheDir } returns cacheDir
        every { context.contentResolver } returns contentResolver
        return context
    }
}
