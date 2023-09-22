package com.hvasoft.dailydose.di

import com.hvasoft.dailydose.domain.interactor.home.DeleteSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.DeleteSnapshotUseCaseImpl
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCaseImpl
import com.hvasoft.dailydose.domain.interactor.home.ToggleUserLikeUseCase
import com.hvasoft.dailydose.domain.interactor.home.ToggleUserLikeUseCaseImpl
import com.hvasoft.dailydose.domain.repository.HomeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object InteractionModule {

    @Provides
    fun provideGetSnapshotsUseCase(
        homeRepository: HomeRepository
    ): GetSnapshotsUseCase = GetSnapshotsUseCaseImpl(homeRepository)

    @Provides
    fun provideToggleUserLikeUseCase(
        homeRepository: HomeRepository
    ): ToggleUserLikeUseCase = ToggleUserLikeUseCaseImpl(homeRepository)

    @Provides
    fun provideDeleteSnapshotUseCase(
        homeRepository: HomeRepository
    ): DeleteSnapshotUseCase = DeleteSnapshotUseCaseImpl(homeRepository)
}
