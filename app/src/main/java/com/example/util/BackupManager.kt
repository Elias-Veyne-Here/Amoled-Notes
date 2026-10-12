package com.example.util

import android.content.Context
import androidx.core.content.FileProvider
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteVersion
import com.example.data.model.Tag
import com.example.data.repository.NoteRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object BackupManager {

    suspend fun createBackupJson(repository: NoteRepository): String {
        val notes = repository.getAllNotesImmediate()
        val versions = repository.getAllVersionsImmediate()
        val folders = repository.getAllFoldersImmediate()
        val tags = repository.getAllTagsImmediate()

        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "AMOLED Notes")
        root.put("timestamp", System.currentTimeMillis())

        val notesArray = JSONArray()
        for (note in notes) {
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("content", note.content)
            obj.put("folder", note.folder)
            obj.put("tags", note.tags)
            obj.put("isPinned", note.isPinned)
            obj.put("isArchived", note.isArchived)
            obj.put("isLocked", note.isLocked)
            obj.put("createdAt", note.createdAt)
            obj.put("updatedAt", note.updatedAt)
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        val versionsArray = JSONArray()
        for (v in versions) {
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("noteId", v.noteId)
            obj.put("title", v.title)
            obj.put("content", v.content)
            obj.put("timestamp", v.timestamp)
            obj.put("wordCount", v.wordCount)
            versionsArray.put(obj)
        }
        root.put("versions", versionsArray)

        val foldersArray = JSONArray()
        for (f in folders) {
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("name", f.name)
            obj.put("createdAt", f.createdAt)
            foldersArray.put(obj)
        }
        root.put("folders", foldersArray)

        val tagsArray = JSONArray()
        for (t in tags) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            tagsArray.put(obj)
        }
        root.put("tags", tagsArray)

        return root.toString(2)
    }

    suspend fun exportBackupFile(context: Context, repository: NoteRepository): File {
        val json = createBackupJson(repository)
        val exportDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val backupFile = File(exportDir, "amoled_notes_backup_${System.currentTimeMillis()}.json")
        FileOutputStream(backupFile).use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        }
        return backupFile
    }

    suspend fun restoreFromJson(repository: NoteRepository, jsonString: String): Result<Int> {
        return try {
            val root = JSONObject(jsonString)
            val notesArray = root.optJSONArray("notes") ?: JSONArray()
            val versionsArray = root.optJSONArray("versions") ?: JSONArray()
            val foldersArray = root.optJSONArray("folders") ?: JSONArray()
            val tagsArray = root.optJSONArray("tags") ?: JSONArray()

            val notes = mutableListOf<Note>()
            for (i in 0 until notesArray.length()) {
                val obj = notesArray.getJSONObject(i)
                notes.add(
                    Note(
                        id = obj.optLong("id", 0),
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        folder = obj.optString("folder", "Default"),
                        tags = obj.optString("tags", ""),
                        isPinned = obj.optBoolean("isPinned", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        isLocked = obj.optBoolean("isLocked", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val versions = mutableListOf<NoteVersion>()
            for (i in 0 until versionsArray.length()) {
                val obj = versionsArray.getJSONObject(i)
                versions.add(
                    NoteVersion(
                        id = obj.optLong("id", 0),
                        noteId = obj.optLong("noteId", 0),
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        wordCount = obj.optInt("wordCount", 0)
                    )
                )
            }

            val folders = mutableListOf<Folder>()
            for (i in 0 until foldersArray.length()) {
                val obj = foldersArray.getJSONObject(i)
                folders.add(
                    Folder(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name", "Default"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val tags = mutableListOf<Tag>()
            for (i in 0 until tagsArray.length()) {
                val obj = tagsArray.getJSONObject(i)
                tags.add(
                    Tag(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name", "")
                    )
                )
            }

            repository.restoreBackup(notes, versions, folders, tags)
            Result.success(notes.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFileUri(context: Context, file: File): android.net.Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
