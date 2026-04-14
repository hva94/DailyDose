package com.hvasoft.dailydose.domain.interactor.home

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.repository.HomeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetSnapshotsUseCaseImplTest {

    private lateinit var homeRepository: HomeRepository
    private lateinit var useCase: GetSnapshotsUseCaseImpl

    @Before
    fun setUp() {
        homeRepository = mockk()
        useCase = GetSnapshotsUseCaseImpl(homeRepository)
    }

    @OptIn(ExperimentalPagingApi::class)
    @Test
    fun `invoke forwards to getPagedSnapshots and emits result`() = runTest {
        val fakePagingData = PagingData.empty<Snapshot>()
        coEvery { homeRepository.getPagedSnapshots() } returns flowOf(fakePagingData)

        val result = useCase.invoke().first()

        assertNotNull(result)
        coVerify(exactly = 1) { homeRepository.getPagedSnapshots() }
    }
}
