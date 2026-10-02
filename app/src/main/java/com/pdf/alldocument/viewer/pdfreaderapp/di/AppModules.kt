package com.pdf.alldocument.viewer.pdfreaderapp.di

import android.content.Context
import androidx.room.Room
import com.pdf.alldocument.viewer.pdfreaderapp.core.database.AppDatabase
import com.pdf.alldocument.viewer.pdfreaderapp.core.database.DocumentDao
import com.pdf.alldocument.viewer.pdfreaderapp.data.repository.DocumentRepositoryImpl
import com.pdf.alldocument.viewer.pdfreaderapp.data.repository.PdfToolsRepositoryImpl
import com.pdf.alldocument.viewer.pdfreaderapp.data.repository.UserPrefsRepositoryImpl
import com.pdf.alldocument.viewer.pdfreaderapp.domain.repository.DocumentRepository
import com.pdf.alldocument.viewer.pdfreaderapp.domain.repository.PdfToolsRepository
import com.pdf.alldocument.viewer.pdfreaderapp.domain.repository.UserPrefsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides
    @Singleton
    fun provideDocumentDao(database: AppDatabase): DocumentDao {
        return database.documentDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        impl: DocumentRepositoryImpl
    ): DocumentRepository

    @Binds
    @Singleton
    abstract fun bindPdfToolsRepository(
        impl: PdfToolsRepositoryImpl
    ): PdfToolsRepository

    @Binds
    @Singleton
    abstract fun bindUserPrefsRepository(
        impl: UserPrefsRepositoryImpl
    ): UserPrefsRepository
}
