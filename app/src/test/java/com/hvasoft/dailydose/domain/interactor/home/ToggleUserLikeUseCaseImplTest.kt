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
class ToggleUserLikeUseCaseImplTest {

    private lateinit var homeRepository: HomeRepository
    private lateinit var useCase: ToggleUserLikeUseCaseImpl

    @Before
    fun setUp() {
        homeRepository = mockk()
        useCase = ToggleUserLikeUseCaseImpl(homeRepository)
    }

    @Test
    fun `invoke delegates like to HomeRepository`() = runTest {
        val snapshot = Snapshot(snapshotKey = "key1")
        coEvery { homeRepository.toggleUserLike(snapshot, true) } returns 1

        useCase.invoke(snapshot, true)

        coVerify(exactly = 1) { homeRepository.toggleUserLike(snapshot, true) }
    }

    @Test
    fun `invoke delegates unlike to HomeRepository`() = runTest {
        val snapshot = Snapshot(snapshotKey = "key1")
        coEvery { homeRepository.toggleUserLike(snapshot, false) } returns 1

        useCase.invoke(snapshot, false)

        coVerify(exactly = 1) { homeRepository.toggleUserLike(snapshot, false) }
    }
}
