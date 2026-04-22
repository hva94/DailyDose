package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotVisibilityMode
import com.hvasoft.dailydose.domain.repository.HomeRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RevealSnapshotUseCaseImplTest {

    private lateinit var homeRepository: HomeRepository
    private lateinit var useCase: RevealSnapshotUseCaseImpl

    @Before
    fun setUp() {
        homeRepository = mockk(relaxed = true)
        useCase = RevealSnapshotUseCaseImpl(homeRepository)
    }

    @Test
    fun `invoke delegates reveal when the snapshot is hidden`() = runTest {
        val snapshot = Snapshot(
            snapshotKey = "snapshot-1",
            idUserOwner = "owner-1",
            visibilityMode = SnapshotVisibilityMode.HIDDEN_UNREVEALED,
        )

        useCase.invoke(snapshot)

        coVerify(exactly = 1) { homeRepository.revealSnapshot(snapshot) }
    }

    @Test
    fun `invoke ignores already visible snapshots`() = runTest {
        val snapshot = Snapshot(
            snapshotKey = "snapshot-1",
            idUserOwner = "owner-1",
            visibilityMode = SnapshotVisibilityMode.VISIBLE_REVEALED,
            isRevealedForViewer = true,
        )

        useCase.invoke(snapshot)

        coVerify(exactly = 0) { homeRepository.revealSnapshot(any()) }
    }
}
