package com.headphonetracker.di

import android.content.Context
import androidx.room.Room
import com.headphonetracker.data.AppDatabase
import com.headphonetracker.data.HeadphoneUsageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [com.headphonetracker.di.DatabaseModule::class]
)
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideInMemoryDb(@ApplicationContext context: Context): AppDatabase {
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    @Singleton
    fun provideHeadphoneUsageDao(db: AppDatabase): HeadphoneUsageDao = db.headphoneUsageDao()
}
