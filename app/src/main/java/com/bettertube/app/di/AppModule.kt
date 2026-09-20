package com.bettertube.app.di

import android.content.Context
import com.bettertube.app.data.engine.YtDlpEngine
import com.bettertube.app.data.schedule.ScheduleConfigStore
import com.bettertube.app.data.schedule.ScheduleScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideYtDlpEngine(@ApplicationContext context: Context): YtDlpEngine = YtDlpEngine(context)

    @Provides
    @Singleton
    fun provideScheduleConfigStore(@ApplicationContext context: Context): ScheduleConfigStore = ScheduleConfigStore(context)

    @Provides
    @Singleton
    fun provideScheduleScheduler(@ApplicationContext context: Context, configStore: ScheduleConfigStore): ScheduleScheduler = ScheduleScheduler(context, configStore)
}
