package com.hvasoft.dailydose.di

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseDataSource
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseDataSourceImpl
import com.hvasoft.dailydose.data.repository.HomeRepositoryImpl
import com.hvasoft.dailydose.data.utils.Constants
import com.hvasoft.dailydose.domain.repository.HomeRepository
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

@Module
@InstallIn(SingletonComponent::class)
object HomeRepositoryModule {

    @Provides
    @Singleton
    fun providesHomeRepository(
        remoteDatabaseDataSource: RemoteDatabaseDataSource
    ): HomeRepository =
        HomeRepositoryImpl(
            remoteDatabaseDataSource
        )

    @Provides
    @Singleton
    @SnapshotsDatabaseQualifier
    fun providesSnapshotsDatabase(): DatabaseReference =
        FirebaseDatabase.getInstance().reference
            .child(Constants.SNAPSHOTS_PATH)

    @Provides
    @Singleton
    @UsersDatabaseQualifier
    fun providesUsersDatabase(): DatabaseReference =
        FirebaseDatabase.getInstance().reference
            .child(Constants.USERS_PATH)

    @Provides
    @Singleton
    fun providesSnapshotsStorage(): StorageReference =
        FirebaseStorage.getInstance().reference
            .child(Constants.SNAPSHOTS_PATH).child(Constants.currentUser.uid)

    @Provides
    @Singleton
    fun providesRemoteDatabaseDataSource(
        @SnapshotsDatabaseQualifier snapshotsDatabase: DatabaseReference,
        @UsersDatabaseQualifier usersDatabase: DatabaseReference,
        snapshotsStorage: StorageReference
    ) : RemoteDatabaseDataSource = RemoteDatabaseDataSourceImpl(
        snapshotsDatabase,
        usersDatabase,
        snapshotsStorage
    )

}