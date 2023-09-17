package com.hvasoft.dailydose.di

import com.hvasoft.dailydose.domain.HomeRepository
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCaseImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InteractionModule {

    @Provides
    @Singleton
    fun provideGetSnapshotsUseCase(
        homeRepository: HomeRepository
    ): GetSnapshotsUseCase = GetSnapshotsUseCaseImpl(homeRepository)

//    @Provides
//    fun provideIsLikeChangedUseCase(
//        homeRepository: HomeRepository
//    ): IsLikeChangedUseCase = Isl(tvShowRepository)
//
//    @Provides
//    fun provideDeleteSnapshotUseCase(
//        homeRepository: HomeRepository
//    ): GetPagedTvShowsUseCase = GetPagedTvShowsUseCaseImpl(tvShowRepository)
}
