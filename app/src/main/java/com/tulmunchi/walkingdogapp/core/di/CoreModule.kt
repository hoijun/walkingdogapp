package com.tulmunchi.walkingdogapp.core.di

import com.tulmunchi.walkingdogapp.core.datastore.UserPreferencesDataStore
import com.tulmunchi.walkingdogapp.core.datastore.UserPreferencesDataStoreImpl
import com.tulmunchi.walkingdogapp.core.location.LocationProvider
import com.tulmunchi.walkingdogapp.core.location.LocationProviderImpl
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.core.network.NetworkCheckerImpl
import com.tulmunchi.walkingdogapp.core.permission.PermissionHandler
import com.tulmunchi.walkingdogapp.core.permission.PermissionHandlerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Core 레이어의 Hilt 의존성 주입 모듈
 *
 * Clean Architecture의 Core 레이어에 있는 유틸리티들을
 * Hilt 컨테이너에 등록하여 의존성 주입이 가능하도록 함
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    /**
     * NetworkChecker 바인딩
     * NetworkCheckerImpl을 NetworkChecker 인터페이스에 바인딩
     */
    @Binds
    @Singleton
    abstract fun bindNetworkChecker(impl: NetworkCheckerImpl): NetworkChecker

    /**
     * PermissionHandler 바인딩
     * PermissionHandlerImpl을 PermissionHandler 인터페이스에 바인딩
     */
    @Binds
    @Singleton
    abstract fun bindPermissionHandler(impl: PermissionHandlerImpl): PermissionHandler

    /**
     * LocationProvider 바인딩
     * LocationProviderImpl을 LocationProvider 인터페이스에 바인딩
     */
    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: LocationProviderImpl): LocationProvider

    /**
     * UserPreferencesDataStore 바인딩
     * UserPreferencesDataStoreImpl을 UserPreferencesDataStore 인터페이스에 바인딩
     */
    @Binds
    @Singleton
    abstract fun bindUserPreferencesDataStore(impl: UserPreferencesDataStoreImpl): UserPreferencesDataStore
}
