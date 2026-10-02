package com.pdf.alldocument.viewer.pdfreaderapp.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY dateModified DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents")
    suspend fun getAllDocumentsSnapshot(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY dateModified DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isRecent = 1 ORDER BY lastReadTime DESC")
    fun getRecentDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE path = :path LIMIT 1")
    suspend fun getDocumentByPath(path: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE path = :path LIMIT 1")
    fun getDocumentFlowByPath(path: String): Flow<DocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<DocumentEntity>)

    @Query("UPDATE documents SET isFavorite = :isFavorite WHERE path = :path")
    suspend fun updateFavorite(path: String, isFavorite: Boolean)

    @Query("UPDATE documents SET isRecent = 1, lastReadTime = :time, lastReadPage = :page WHERE path = :path")
    suspend fun updateRecentAndPage(path: String, time: Long, page: Int)

    @Query("UPDATE documents SET lastReadPage = :page WHERE path = :path")
    suspend fun updateLastReadPage(path: String, page: Int)

    @Query("UPDATE documents SET totalPages = :totalPages WHERE path = :path")
    suspend fun updateTotalPages(path: String, totalPages: Int)

    @Query("DELETE FROM documents WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM documents WHERE isRecent = 1")
    suspend fun clearAllRecent()
}
