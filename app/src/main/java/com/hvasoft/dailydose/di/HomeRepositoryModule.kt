package com.hvasoft.dailydose.di

import com.google.firebase.database.DatabaseReference
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.data.auth.AuthSessionProvider
import com.hvasoft.dailydose.data.auth.FirebaseAuthSessionProvider
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.local.DailyDoseDatabase
import com.hvasoft.dailydose.data.local.FeedAssetStorage
import com.hvasoft.dailydose.data.local.FeedSyncStateDao
import com.hvasoft.dailydose.data.local.HomeFeedTransactionRunner
import com.hvasoft.dailydose.data.local.OfflineFeedItemDao
import com.hvasoft.dailydose.data.local.OfflineFeedMapper
import com.hvasoft.dailydose.data.local.OfflineMediaAssetDao
import com.hvasoft.dailydose.data.local.ProfileLocalCache
import com.hvasoft.dailydose.data.local.RoomHomeFeedTransactionRunner
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseServiceImpl
import com.hvasoft.dailydose.data.repository.HomeFeedRefreshCoordinator
import com.hvasoft.dailydose.data.repository.AddSnapshotRepositoryImpl
import com.hvasoft.dailydose.data.repository.HomeRepositoryImpl
import com.hvasoft.dailydose.data.repository.ProfileRepositoryImpl
import com.hvasoft.dailydose.domain.repository.AddSnapshotRepository
import com.hvasoft.dailydose.domain.repository.HomeRepository
import com.hvasoft.dailydose.domain.repository.ProfileRepository
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun providesFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun providesAuthSessionProvider(firebaseAuth: FirebaseAuth): AuthSessionProvider =
        FirebaseAuthSessionProvider(firebaseAuth)

    @Provides
    @Singleton
    fun providesHomeRepository(
        remoteDatabaseService: RemoteDatabaseService,
        offlineFeedItemDao: OfflineFeedItemDao,
        offlineMediaAssetDao: OfflineMediaAssetDao,
        feedSyncStateDao: FeedSyncStateDao,
        offlineFeedMapper: OfflineFeedMapper,
        refreshCoordinator: HomeFeedRefreshCoordinator,
        authSessionProvider: AuthSessionProvider,
        feedAssetStorage: FeedAssetStorage,
    ): HomeRepository = HomeRepositoryImpl(
        remoteDatabaseService = remoteDatabaseService,
        offlineFeedItemDao = offlineFeedItemDao,
        offlineMediaAssetDao = offlineMediaAssetDao,
        feedSyncStateDao = feedSyncStateDao,
        offlineFeedMapper = offlineFeedMapper,
        refreshCoordinator = refreshCoordinator,
        authSessionProvider = authSessionProvider,
        feedAssetStorage = feedAssetStorage,
    )

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
    @SnapshotsRootStorageQualifier
    fun providesSnapshotsRootStorage(): StorageReference =
        FirebaseStorage.getInstance().reference.child(Constants.SNAPSHOTS_PATH)

    @Provides
    @Singleton
    fun providesRemoteDatabaseDataSource(
        @SnapshotsDatabaseQualifier snapshotsDatabase: DatabaseReference,
        @UsersDatabaseQualifier usersDatabase: DatabaseReference,
        @SnapshotsRootStorageQualifier snapshotsRootStorage: StorageReference,
        authSessionProvider: AuthSessionProvider,
    ): RemoteDatabaseService = RemoteDatabaseServiceImpl(
        snapshotsDatabase = snapshotsDatabase,
        usersDatabase = usersDatabase,
        snapshotsRootStorage = snapshotsRootStorage,
        authSessionProvider = authSessionProvider,
    )

    @Provides
    @Singleton
    fun providesDailyDoseDatabase(
        @ApplicationContext context: Context,
    ): DailyDoseDatabase = Room.databaseBuilder(
        context,
        DailyDoseDatabase::class.java,
        "daily-dose-offline.db",
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun providesOfflineFeedItemDao(database: DailyDoseDatabase): OfflineFeedItemDao =
        database.offlineFeedItemDao()

    @Provides
    fun providesOfflineMediaAssetDao(database: DailyDoseDatabase): OfflineMediaAssetDao =
        database.offlineMediaAssetDao()

    @Provides
    fun providesFeedSyncStateDao(database: DailyDoseDatabase): FeedSyncStateDao =
        database.feedSyncStateDao()

    @Provides
    @Singleton
    fun providesOfflineFeedMapper(): OfflineFeedMapper = OfflineFeedMapper()

    @Provides
    @Singleton
    fun providesFeedAssetStorage(
        @ApplicationContext context: Context,
        @DispatcherIO dispatcherIO: kotlinx.coroutines.CoroutineDispatcher,
    ): FeedAssetStorage = FeedAssetStorage(context, dispatcherIO)

    @Provides
    @Singleton
    fun providesHomeFeedTransactionRunner(
        database: DailyDoseDatabase,
    ): HomeFeedTransactionRunner = RoomHomeFeedTransactionRunner(database)

    @Provides
    @Singleton
    fun providesHomeFeedRefreshCoordinator(
        remoteDatabaseService: RemoteDatabaseService,
        transactionRunner: HomeFeedTransactionRunner,
        offlineFeedItemDao: OfflineFeedItemDao,
        offlineMediaAssetDao: OfflineMediaAssetDao,
        feedSyncStateDao: FeedSyncStateDao,
        feedAssetStorage: FeedAssetStorage,
        profileLocalCache: ProfileLocalCache,
        @DispatcherIO dispatcherIO: kotlinx.coroutines.CoroutineDispatcher,
    ): HomeFeedRefreshCoordinator = HomeFeedRefreshCoordinator(
        remoteDatabaseService = remoteDatabaseService,
        transactionRunner = transactionRunner,
        offlineFeedItemDao = offlineFeedItemDao,
        offlineMediaAssetDao = offlineMediaAssetDao,
        feedSyncStateDao = feedSyncStateDao,
        feedAssetStorage = feedAssetStorage,
        profileLocalCache = profileLocalCache,
        dispatcherIO = dispatcherIO,
    )
}
