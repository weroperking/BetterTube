package com.bettertube.app.di

import android.content.Context
import com.bettertube.app.data.engine.YtDlpEngine
import com.bettertube.app.data.repository.Aria2RepositoryImpl
import com.bettertube.app.data.repository.DownloadRepositoryImpl
import com.bettertube.app.data.repository.VaultRepositoryImpl
import com.bettertube.app.domain.repository.Aria2Repository
import com.bettertube.app.domain.repository.DownloadRepository
import com.bettertube.app.domain.repository.VaultRepository
import com.bettertube.app.utils.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAria2Repository(impl: Aria2RepositoryImpl): Aria2Repository

    @Binds
    @Singleton
    abstract fun bindVaultRepository(impl: VaultRepositoryImpl): VaultRepository

    companion object {
        @Provides
        @Singleton
        fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
            return NetworkMonitor(context)
        }

        @Provides
        @Singleton
        fun provideDownloadRepository(
            engine: YtDlpEngine,
            networkMonitor: NetworkMonitor,
            @ApplicationContext context: Context
        ): DownloadRepository {
            return DownloadRepositoryImpl(engine, networkMonitor, context)
        }
    }
}
