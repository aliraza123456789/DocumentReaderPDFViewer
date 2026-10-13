package com.pdf.alldocument.office.pdfreaderapp.data.pdftools.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pdf_files")
data class PdfFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val uriString: String,
    val sizeBytes: Long,
    val folderId: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L,
    val lastOpenedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface PdfDao {

    @Query("SELECT * FROM pdf_files WHERE isDeleted = 0 ORDER BY lastOpenedAt DESC")
    fun observeRecentFiles(): Flow<List<PdfFileEntity>>

    @Query("SELECT * FROM pdf_files WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeRecycleBin(): Flow<List<PdfFileEntity>>

    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(entity: PdfFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(entity: FolderEntity): Long

    @Query("UPDATE pdf_files SET isDeleted = 1, deletedAt = :timestamp WHERE id = :id")
    suspend fun moveToRecycleBin(id: Long, timestamp: Long)

    @Query("UPDATE pdf_files SET isDeleted = 0 WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM pdf_files WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM pdf_files WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()
}

@Database(entities = [PdfFileEntity::class, FolderEntity::class], version = 1, exportSchema = false)
abstract class PdfDatabase : androidx.room.RoomDatabase() {
    abstract fun pdfDao(): PdfDao
}
