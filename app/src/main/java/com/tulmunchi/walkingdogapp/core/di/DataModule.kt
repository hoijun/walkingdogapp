package com.tulmunchi.walkingdogapp.core.di

import com.tulmunchi.walkingdogapp.data.repository.AlarmRepositoryImpl
import com.tulmunchi.walkingdogapp.data.repository.CollectionRepositoryImpl
import com.tulmunchi.walkingdogapp.data.repository.DogRepositoryImpl
import com.tulmunchi.walkingdogapp.data.repository.UserRepositoryImpl
import com.tulmunchi.walkingdogapp.data.repository.WalkRepositoryImpl
import com.tulmunchi.walkingdogapp.data.source.local.AlarmLocalDataSource
import com.tulmunchi.walkingdogapp.data.source.local.AlarmLocalDataSourceImpl
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseCollectionDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseCollectionDataSourceImpl
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseDogDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseDogDataSourceImpl
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseStorageDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseStorageDataSourceImpl
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseUserDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseUserDataSourceImpl
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseWalkDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseWalkDataSourceImpl
import com.tulmunchi.walkingdogapp.domain.repository.AlarmRepository
import com.tulmunchi.walkingdogapp.domain.repository.CollectionRepository
import com.tulmunchi.walkingdogapp.domain.repository.DogRepository
import com.tulmunchi.walkingdogapp.domain.repository.UserRepository
import com.tulmunchi.walkingdogapp.domain.repository.WalkRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Data Layer dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    // ========== Data Sources ==========

    @Binds
    @Singleton
    abstract fun bindFirebaseUserDataSource(
        impl: FirebaseUserDataSourceImpl
    ): FirebaseUserDataSource

    @Binds
    @Singleton
    abstract fun bindFirebaseDogDataSource(
        impl: FirebaseDogDataSourceImpl
    ): FirebaseDogDataSource

    @Binds
    @Singleton
    abstract fun bindFirebaseStorageDataSource(
        impl: FirebaseStorageDataSourceImpl
    ): FirebaseStorageDataSource

    @Binds
    @Singleton
    abstract fun bindFirebaseWalkDataSource(
        impl: FirebaseWalkDataSourceImpl
    ): FirebaseWalkDataSource

    @Binds
    @Singleton
    abstract fun bindFirebaseCollectionDataSource(
        impl: FirebaseCollectionDataSourceImpl
    ): FirebaseCollectionDataSource

    @Binds
    @Singleton
    abstract fun bindAlarmLocalDataSource(
        impl: AlarmLocalDataSourceImpl
    ): AlarmLocalDataSource

    // ========== Repositories ==========

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindDogRepository(
        impl: DogRepositoryImpl
    ): DogRepository

    @Binds
    @Singleton
    abstract fun bindWalkRepository(
        impl: WalkRepositoryImpl
    ): WalkRepository

    @Binds
    @Singleton
    abstract fun bindAlarmRepository(
        impl: AlarmRepositoryImpl
    ): AlarmRepository

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(
        impl: CollectionRepositoryImpl
    ): CollectionRepository
}
