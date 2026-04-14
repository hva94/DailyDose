package com.hvasoft.dailydose.di

import com.hvasoft.dailydose.domain.interactor.add.CreateSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.add.CreateSnapshotUseCaseImpl
import com.hvasoft.dailydose.domain.interactor.home.DeleteSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.DeleteSnapshotUseCaseImpl
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCaseImpl
import com.hvasoft.dailydose.domain.interactor.home.ToggleUserLikeUseCase
import com.hvasoft.dailydose.domain.interactor.home.ToggleUserLikeUseCaseImpl
import com.hvasoft.dailydose.domain.interactor.profile.GetUserProfileUseCase
import com.hvasoft.dailydose.domain.interactor.profile.GetUserProfileUseCaseImpl
import com.hvasoft.dailydose.domain.interactor.profile.UpdateProfileNameUseCase
import com.hvasoft.dailydose.domain.interactor.profile.UpdateProfileNameUseCaseImpl
import com.hvasoft.dailydose.domain.interactor.profile.UploadProfilePhotoUseCase
import com.hvasoft.dailydose.domain.interactor.profile.UploadProfilePhotoUseCaseImpl
import com.hvasoft.dailydose.domain.repository.AddSnapshotRepository
import com.hvasoft.dailydose.domain.repository.HomeRepository
import com.hvasoft.dailydose.domain.repository.ProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object InteractionModule {

    @Provides
    fun provideGetSnapshotsUseCase(
        homeRepository: HomeRepository,
    ): GetSnapshotsUseCase = GetSnapshotsUseCaseImpl(homeRepository)

    @Provides
    fun provideToggleUserLikeUseCase(
        homeRepository: HomeRepository,
    ): ToggleUserLikeUseCase = ToggleUserLikeUseCaseImpl(homeRepository)

    @Provides
    fun provideDeleteSnapshotUseCase(
        homeRepository: HomeRepository,
    ): DeleteSnapshotUseCase = DeleteSnapshotUseCaseImpl(homeRepository)

    @Provides
    fun provideCreateSnapshotUseCase(
        addSnapshotRepository: AddSnapshotRepository,
    ): CreateSnapshotUseCase = CreateSnapshotUseCaseImpl(addSnapshotRepository)

    @Provides
    fun provideGetUserProfileUseCase(
        profileRepository: ProfileRepository,
    ): GetUserProfileUseCase = GetUserProfileUseCaseImpl(profileRepository)

    @Provides
    fun provideUpdateProfileNameUseCase(
        profileRepository: ProfileRepository,
    ): UpdateProfileNameUseCase = UpdateProfileNameUseCaseImpl(profileRepository)

    @Provides
    fun provideUploadProfilePhotoUseCase(
        profileRepository: ProfileRepository,
    ): UploadProfilePhotoUseCase = UploadProfilePhotoUseCaseImpl(profileRepository)
}
