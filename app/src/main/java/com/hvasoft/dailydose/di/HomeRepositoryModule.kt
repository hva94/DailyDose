package com.hvasoft.dailydose.di

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseServiceImpl
import com.hvasoft.dailydose.data.repository.AddSnapshotRepositoryImpl
import com.hvasoft.dailydose.data.repository.HomeRepositoryImpl
import com.hvasoft.dailydose.data.repository.ProfileRepositoryImpl
import com.hvasoft.dailydose.domain.repository.AddSnapshotRepository
import com.hvasoft.dailydose.domain.repository.HomeRepository
import com.hvasoft.dailydose.domain.repository.ProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SnapshotsDatabaseQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UsersDatabaseQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SnapshotsRootStorageQualifier

@Module
@InstallIn(SingletonComponent::class)
object HomeRepositoryModule {

    @Provides
    @Singleton
    fun providesHomeRepository(
        remoteDatabaseService: RemoteDatabaseService,
    ): HomeRepository = HomeRepositoryImpl(remoteDatabaseService)

    @Provides
    @Singleton
    fun providesAddSnapshotRepository(
        remoteDatabaseService: RemoteDatabaseService,
    ): AddSnapshotRepository = AddSnapshotRepositoryImpl(remoteDatabaseService)

    @Provides
    @Singleton
    fun providesProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository = impl

    @Provides
    @Singleton
    @SnapshotsDatabaseQualifier
    fun providesSnapshotsDatabase(): DatabaseReference =
        FirebaseDatabase.getInstance().reference.child(Constants.SNAPSHOTS_PATH)

    @Provides
    @Singleton
    @UsersDatabaseQualifier
    fun providesUsersDatabase(): DatabaseReference =
        FirebaseDatabase.getInstance().reference.child(Constants.USERS_PATH)

    @Provides
    @Singleton
    fun providesSnapshotsStorage(): StorageReference =
        FirebaseStorage.getInstance().reference
            .child(Constants.SNAPSHOTS_PATH).child(Constants.currentUser.uid)

    @Provides
    @Singleton
    @SnapshotsRootStorageQualifier
    fun providesSnapshotsRootStorage(): StorageReference =
        FirebaseStorage.getInstance().reference.child(Constants.SNAPSHOTS_PATH)

    @Provides
    @Singleton
    fun providesRemoteDatabaseDataSource(
        @SnapshotsDatabaseQualifier snapshotsDatabase: DatabaseReference,
        @UsersDatabaseQualifier usersDatabase: DatabaseReference,
        snapshotsStorage: StorageReference,
    ): RemoteDatabaseService = RemoteDatabaseServiceImpl(
        snapshotsDatabase,
        usersDatabase,
        snapshotsStorage,
    )
}
