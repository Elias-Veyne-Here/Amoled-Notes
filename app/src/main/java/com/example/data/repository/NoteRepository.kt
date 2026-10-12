package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteVersion
import com.example.data.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NoteRepository(private val db: AppDatabase) {
    private val noteDao = db.noteDao()
    private val versionDao = db.noteVersionDao()
    private val folderDao = db.folderDao()
    private val tagDao = db.tagDao()

    fun getActiveNotes(): Flow<List<Note>> = noteDao.getActiveNotes()

    fun getArchivedNotes(): Flow<List<Note>> = noteDao.getArchivedNotes()

    fun getNotesByFolder(folder: String): Flow<List<Note>> = noteDao.getNotesByFolder(folder)

    fun getNoteById(id: Long): Flow<Note?> = noteDao.getNoteById(id)

    suspend fun getNoteByIdImmediate(id: Long): Note? = withContext(Dispatchers.IO) {
        noteDao.getNoteByIdImmediate(id)
    }

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun insertNote(note: Note): Long = withContext(Dispatchers.IO) {
        val id = noteDao.insertNote(note)
        // Also save initial version snapshot
        if (note.content.isNotBlank() || note.title.isNotBlank()) {
            versionDao.insertVersion(
                NoteVersion(
                    noteId = id,
                    title = note.title,
                    content = note.content,
                    timestamp = System.currentTimeMillis(),
                    wordCount = note.wordCount
                )
            )
        }
        id
    }

    suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun togglePin(note: Note): Boolean = withContext(Dispatchers.IO) {
        if (!note.isPinned) {
            val count = noteDao.getPinnedCount()
            if (count >= 3) return@withContext false
            noteDao.updateNote(note.copy(isPinned = true, updatedAt = System.currentTimeMillis()))
            true
        } else {
            noteDao.updateNote(note.copy(isPinned = false, updatedAt = System.currentTimeMillis()))
            true
        }
    }

    suspend fun toggleArchive(note: Note) = withContext(Dispatchers.IO) {
        noteDao.updateNote(
            note.copy(
                isArchived = !note.isArchived,
                isPinned = if (!note.isArchived) false else note.isPinned, // Unpin when archiving
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleLock(note: Note) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note.copy(isLocked = !note.isLocked, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: Long) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }

    // Version Snapshots
    fun getVersionsForNote(noteId: Long): Flow<List<NoteVersion>> = versionDao.getVersionsForNote(noteId)

    suspend fun saveVersionSnapshot(note: Note) = withContext(Dispatchers.IO) {
        if (note.id <= 0) return@withContext
        val latest = versionDao.getLatestVersion(note.id)
        // Only snapshot if text changed and at least 60 seconds elapsed, or first snapshot
        if (latest == null || latest.content != note.content || latest.title != note.title) {
            versionDao.insertVersion(
                NoteVersion(
                    noteId = note.id,
                    title = note.title,
                    content = note.content,
                    timestamp = System.currentTimeMillis(),
                    wordCount = note.wordCount
                )
            )
        }
    }

    suspend fun restoreVersion(noteId: Long, version: NoteVersion) = withContext(Dispatchers.IO) {
        val current = noteDao.getNoteByIdImmediate(noteId) ?: return@withContext
        val updated = current.copy(
            title = version.title,
            content = version.content,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updated)
    }

    // Folders & Tags
    fun getAllFolders(): Flow<List<Folder>> = folderDao.getAllFolders()

    suspend fun insertFolder(name: String): Long = withContext(Dispatchers.IO) {
        folderDao.insertFolder(Folder(name = name.trim()))
    }

    suspend fun deleteFolder(folder: Folder) = withContext(Dispatchers.IO) {
        folderDao.deleteFolder(folder)
    }

    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun insertTag(name: String): Long = withContext(Dispatchers.IO) {
        tagDao.insertTag(Tag(name = name.trim().removePrefix("#")))
    }

    suspend fun deleteTag(tag: Tag) = withContext(Dispatchers.IO) {
        tagDao.deleteTag(tag)
    }

    // Backup & Restore
    suspend fun getAllNotesImmediate(): List<Note> = withContext(Dispatchers.IO) {
        noteDao.getAllNotesImmediate()
    }

    suspend fun getAllVersionsImmediate(): List<NoteVersion> = withContext(Dispatchers.IO) {
        versionDao.getAllVersionsImmediate()
    }

    suspend fun getAllFoldersImmediate(): List<Folder> = withContext(Dispatchers.IO) {
        folderDao.getAllFoldersImmediate()
    }

    suspend fun getAllTagsImmediate(): List<Tag> = withContext(Dispatchers.IO) {
        tagDao.getAllTagsImmediate()
    }

    suspend fun restoreBackup(
        notes: List<Note>,
        versions: List<NoteVersion>,
        folders: List<Folder>,
        tags: List<Tag>
    ) = withContext(Dispatchers.IO) {
        noteDao.clearAllNotes()
        versionDao.clearAllVersions()
        folderDao.clearAllFolders()
        tagDao.clearAllTags()

        noteDao.insertNotes(notes)
        versionDao.insertVersions(versions)
        folderDao.insertFolders(folders)
        tagDao.insertTags(tags)
    }
}
