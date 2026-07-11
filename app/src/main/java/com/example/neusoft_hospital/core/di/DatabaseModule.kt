package com.example.neusoft_hospital.core.di

import android.content.Context
import androidx.room.Room
import com.example.neusoft_hospital.core.data.local.AppDatabase
import com.example.neusoft_hospital.core.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "neusoft_hospital.db").build()

    @Provides fun provideAppointmentDao(db: AppDatabase) = db.appointmentDao()
    @Provides fun provideFamilyMemberDao(db: AppDatabase) = db.familyMemberDao()
    @Provides fun provideFollowUpDao(db: AppDatabase) = db.followUpDao()
    @Provides fun provideRehabLogDao(db: AppDatabase) = db.rehabLogDao()
    @Provides fun provideChronicDao(db: AppDatabase) = db.chronicDao()
    @Provides fun provideChatSessionDao(db: AppDatabase) = db.chatSessionDao()
}
