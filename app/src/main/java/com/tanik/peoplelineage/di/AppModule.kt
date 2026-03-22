package com.tanik.peoplelineage.di

import android.content.Context
import com.tanik.peoplelineage.data.AppDatabase
import com.tanik.peoplelineage.data.AppDatabaseProvider
import com.tanik.peoplelineage.data.AppPreferences
import com.tanik.peoplelineage.data.PeopleRepository
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabaseProvider.get(context)
    }

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun providePeopleRepository(@ApplicationContext context: Context): PeopleRepository {
        return PeopleRepository.getInstance(context)
    }
}
