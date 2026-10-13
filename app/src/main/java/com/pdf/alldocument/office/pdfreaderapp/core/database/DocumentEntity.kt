package com.pdf.alldocument.office.pdfreaderapp.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val path: String,
    val name: String,
    val size: Long,
    val dateModified: Long,
    val extension: String,
    val isFavorite: Boolean = false,
    val isRecent: Boolean = false,
    val lastReadTime: Long = 0L,
    val lastReadPage: Int = 0,
    val totalPages: Int = 0
)
