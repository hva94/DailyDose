package com.hvasoft.dailydose.di

import com.hvasoft.dailydose.data.repository.HomeRepositoryImpl
import com.hvasoft.dailydose.data.network.RemoteDatabaseService
import com.hvasoft.dailydose.domain.HomeRepository
import com.hvasoft.dailydose.domain.use_case.home.DeleteSnapshotUC
import com.hvasoft.dailydose.domain.use_case.home.GetSnapshotsUC
import com.hvasoft.dailydose.domain.use_case.home.HomeUseCases
import com.hvasoft.dailydose.domain.use_case.home.IsLikeChangedUC
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeRepositoryModule {

    @Provides
    @Singleton
    fun providesHomeRepository(
        remoteDatabaseService: RemoteDatabaseService
    ): HomeRepository =
        HomeRepositoryImpl(
            remoteDatabaseService
        )

    @Provides
    @Singleton
    fun providesRemoteDatabase() = RemoteDatabaseService()

    @Provides
    @Singleton
    fun providesCounterUseCases(
        homeRepository: HomeRepository
    ): HomeUseCases = HomeUseCases(
        getSnapshots = GetSnapshotsUC(homeRepository),
        isLikeChanged = IsLikeChangedUC(homeRepository),
        deleteSnapshot = DeleteSnapshotUC(homeRepository)
    )

}