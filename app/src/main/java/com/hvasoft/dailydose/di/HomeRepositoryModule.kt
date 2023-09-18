package com.hvasoft.dailydose.di

import com.hvasoft.dailydose.data.network.RemoteDatabaseApi
import com.hvasoft.dailydose.data.repository.HomeRepositoryImpl
import com.hvasoft.dailydose.domain.repository.HomeRepository
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
        remoteDatabaseApi: RemoteDatabaseApi
    ): HomeRepository =
        HomeRepositoryImpl(
            remoteDatabaseApi
        )

    @Provides
    @Singleton
    fun providesRemoteDatabase() = RemoteDatabaseApi()

}