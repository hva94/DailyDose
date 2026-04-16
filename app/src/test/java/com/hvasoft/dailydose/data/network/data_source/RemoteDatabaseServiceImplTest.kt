package com.hvasoft.dailydose.data.network.data_source

import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.data.auth.FakeAuthSessionProvider
import com.hvasoft.dailydose.domain.model.CreateSnapshotResult
import com.hvasoft.dailydose.domain.model.Snapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteDatabaseServiceImplTest {

    private lateinit var snapshotsDatabase: DatabaseReference
    private lateinit var usersDatabase: DatabaseReference
    private lateinit var snapshotsRootStorage: StorageReference
    private lateinit var authSessionProvider: FakeAuthSessionProvider
    private lateinit var service: RemoteDatabaseServiceImpl

    @Before
    fun setUp() {
        snapshotsDatabase = mockk(relaxed = true)
        usersDatabase = mockk(relaxed = true)
        snapshotsRootStorage = mockk(relaxed = true)
        authSessionProvider = FakeAuthSessionProvider()
        service = RemoteDatabaseServiceImpl(
            snapshotsDatabase = snapshotsDatabase,
            usersDatabase = usersDatabase,
            snapshotsRootStorage = snapshotsRootStorage,
            authSessionProvider = authSessionProvider,
        )
    }

    @Test
    fun `construction does not require a signed in user`() {
        assertThat(service).isNotNull()
    }

    @Test
    fun `publishSnapshot returns save failed when no signed in user exists`() = runTest {
        val result = service.publishSnapshot(
            localImageContentUri = "content://images/test",
            title = "Test",
            onProgress = {},
        )

        assertThat(result).isEqualTo(CreateSnapshotResult.SaveFailed)
    }

    @Test
    fun `toggleUserLike throws a controlled error when no signed in user exists`() = runTest {
        val failure = runCatching {
            service.toggleUserLike(
                snapshot = Snapshot(snapshotKey = "snapshot-1"),
                isChecked = true,
            )
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `deleteSnapshot uses the snapshot owner storage path`() = runTest {
        val ownerStorage = mockk<StorageReference>()
        val snapshotStorage = mockk<StorageReference>()
        val snapshotDatabase = mockk<DatabaseReference>()
        every { snapshotsRootStorage.child("owner-1") } returns ownerStorage
        every { ownerStorage.child("snapshot-1") } returns snapshotStorage
        every { snapshotsDatabase.child("snapshot-1") } returns snapshotDatabase
        every { snapshotStorage.delete() } returns Tasks.forResult<Void?>(null)
        every { snapshotDatabase.removeValue() } returns Tasks.forResult<Void?>(null)

        val result = service.deleteSnapshot(
            Snapshot(
                snapshotKey = "snapshot-1",
                idUserOwner = "owner-1",
            ),
        )

        assertThat(result).isEqualTo(1)
        verify(exactly = 1) { snapshotsRootStorage.child("owner-1") }
        verify(exactly = 1) { ownerStorage.child("snapshot-1") }
        verify(exactly = 1) { snapshotStorage.delete() }
        verify(exactly = 1) { snapshotDatabase.removeValue() }
    }
}
