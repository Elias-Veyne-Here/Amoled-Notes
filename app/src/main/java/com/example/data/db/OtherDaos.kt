package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Folder
import com.example.data.model.NoteVersion
import com.example.data.model.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteVersionDao {
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY timestamp DESC")
    fun getVersionsForNote(noteId: Long): Flow<List<NoteVersion>>

    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestVersion(noteId: Long): NoteVersion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: NoteVersion): Long

    @Query("DELETE FROM note_versions WHERE noteId = :noteId")
    suspend fun deleteVersionsForNote(noteId: Long)

    @Query("SELECT * FROM note_versions")
    suspend fun getAllVersionsImmediate(): List<NoteVersion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersions(versions: List<NoteVersion>)

    @Query("DELETE FROM note_versions")
    suspend fun clearAllVersions()
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersImmediate(): List<Folder>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: Folder): Long

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<Folder>)

    @Query("DELETE FROM folders")
    suspend fun clearAllFolders()
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags")
    suspend fun getAllTagsImmediate(): List<Tag>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<Tag>)

    @Query("DELETE FROM tags")
    suspend fun clearAllTags()
}
