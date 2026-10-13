package com.pdf.alldocument.office.pdfreaderapp.di.pdftools

import android.content.Context
import androidx.room.Room
import com.pdf.alldocument.office.pdfreaderapp.data.pdftools.local.PdfDao
import com.pdf.alldocument.office.pdfreaderapp.data.pdftools.local.PdfDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): PdfDatabase =
        Room.databaseBuilder(context, PdfDatabase::class.java, "pdf_tools.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun providePdfDao(db: PdfDatabase): PdfDao = db.pdfDao()
}
