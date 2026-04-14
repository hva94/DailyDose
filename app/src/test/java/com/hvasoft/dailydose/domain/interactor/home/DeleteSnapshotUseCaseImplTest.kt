package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.repository.HomeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteSnapshotUseCaseImplTest {

    private lateinit var homeRepository: HomeRepository
    private lateinit var useCase: DeleteSnapshotUseCaseImpl

    @Before
    fun setUp() {
        homeRepository = mockk()
        useCase = DeleteSnapshotUseCaseImpl(homeRepository)
    }

    @Test
    fun `invoke calls deleteSnapshot on HomeRepository`() = runTest {
        val snapshot = Snapshot(snapshotKey = "key1")
        coEvery { homeRepository.deleteSnapshot(snapshot) } returns 1

        useCase.invoke(snapshot)

        coVerify(exactly = 1) { homeRepository.deleteSnapshot(snapshot) }
    }

    @Test(expected = Exception::class)
    fun `invoke throws when repository returns 0`() = runTest {
        val snapshot = Snapshot(snapshotKey = "key1")
        coEvery { homeRepository.deleteSnapshot(snapshot) } returns 0

        useCase.invoke(snapshot)
    }
}
