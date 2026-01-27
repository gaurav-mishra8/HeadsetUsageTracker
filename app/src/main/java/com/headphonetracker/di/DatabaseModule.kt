package com.headphonetracker.di

import android.content.Context
import com.headphonetracker.data.AppDatabase
import com.headphonetracker.data.HeadphoneUsageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideHeadphoneUsageDao(db: AppDatabase): HeadphoneUsageDao = db.headphoneUsageDao()

}
