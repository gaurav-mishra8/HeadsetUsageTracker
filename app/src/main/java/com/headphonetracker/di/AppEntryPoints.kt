package com.headphonetracker.di

import com.headphonetracker.data.HeadphoneUsageDao
import com.headphonetracker.data.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoints {
    fun getSettingsRepository(): SettingsRepository
    fun getHeadphoneUsageDao(): HeadphoneUsageDao
}
