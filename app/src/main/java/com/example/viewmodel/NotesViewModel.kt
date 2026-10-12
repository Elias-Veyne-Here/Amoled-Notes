package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteVersion
import com.example.data.model.Tag
import com.example.data.preferences.EditorFontSize
import com.example.data.preferences.UserPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.theme.ThemeMode
import com.example.util.BackupManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = NoteRepository(db)
    val preferences = UserPreferences(application)

    // User Preferences
    val themeMode: StateFlow<ThemeMode> = preferences.themeModeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ThemeMode.AMOLED
    )

    val fontSize: StateFlow<EditorFontSize> = preferences.fontSizeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        EditorFontSize.MEDIUM
    )

    val defaultFolder: StateFlow<String> = preferences.defaultFolderFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "Default"
    )

    val pinHash: StateFlow<String?> = preferences.pinHashFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val biometricsEnabled: StateFlow<Boolean> = preferences.biometricsEnabledFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    // Selection filters
    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    // Unlocked notes in the current session
    private val _unlockedNotes = MutableStateFlow<Set<Long>>(emptySet())
    val unlockedNotes: StateFlow<Set<Long>> = _unlockedNotes.asStateFlow()

    // Notes List with Folder/Tag Filter
    val allActiveNotes: StateFlow<List<Note>> = combine(
        repository.getActiveNotes(),
        _selectedFolder,
        _selectedTag
    ) { notes, folder, tag ->
        var filtered = notes
        if (folder != null) {
            filtered = filtered.filter { it.folder.equals(folder, ignoreCase = true) }
        }
        if (tag != null) {
            filtered = filtered.filter { note ->
                note.tagList.any { it.equals(tag, ignoreCase = true) }
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<Note>> = repository.getArchivedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search Query & Results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Note>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getActiveNotes()
        } else {
            repository.searchNotes(query.trim())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFolder(folder: String?) {
        _selectedFolder.value = folder
    }

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun unlockNote(noteId: Long) {
        _unlockedNotes.value = _unlockedNotes.value + noteId
    }

    fun isNoteUnlocked(note: Note): Boolean {
        if (!note.isLocked) return true
        return _unlockedNotes.value.contains(note.id)
    }

    fun verifyPin(enteredPin: String): Boolean {
        val storedHash = pinHash.value ?: return true
        return UserPreferences.hashPin(enteredPin) == storedHash
    }

    // Note actions
    suspend fun createNote(
        initialTitle: String = "",
        initialContent: String = "",
        folder: String? = null
    ): Long {
        val assignedFolder = folder ?: defaultFolder.value
        val note = Note(
            title = initialTitle,
            content = initialContent,
            folder = assignedFolder,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return repository.insertNote(note)
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun saveVersionSnapshot(note: Note) {
        viewModelScope.launch {
            repository.saveVersionSnapshot(note)
        }
    }

    fun togglePin(note: Note, onMaxPinnedReached: () -> Unit) {
        viewModelScope.launch {
            val success = repository.togglePin(note)
            if (!success) {
                onMaxPinnedReached()
            }
        }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch {
            repository.toggleArchive(note)
        }
    }

    fun toggleLock(note: Note) {
        viewModelScope.launch {
            repository.toggleLock(note)
            if (!note.isLocked) {
                // If it was just locked, unlock for current session so creator isn't blocked immediately
                unlockNote(note.id)
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun restoreDeletedNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    fun getVersionsForNote(noteId: Long): Flow<List<NoteVersion>> {
        return repository.getVersionsForNote(noteId)
    }

    fun restoreVersion(noteId: Long, version: NoteVersion) {
        viewModelScope.launch {
            repository.restoreVersion(noteId, version)
        }
    }

    // Folders & Tags
    fun addFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertFolder(name.trim())
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            if (_selectedFolder.value == folder.name) {
                _selectedFolder.value = null
            }
        }
    }

    fun addTag(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertTag(name.trim().removePrefix("#"))
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            repository.deleteTag(tag)
            if (_selectedTag.value == tag.name) {
                _selectedTag.value = null
            }
        }
    }

    // Preferences
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setFontSize(size: EditorFontSize) {
        viewModelScope.launch {
            preferences.setFontSize(size)
        }
    }

    fun setDefaultFolder(folder: String) {
        viewModelScope.launch {
            preferences.setDefaultFolder(folder)
        }
    }

    fun setPin(pin: String?) {
        viewModelScope.launch {
            preferences.setPin(pin)
        }
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setBiometricsEnabled(enabled)
        }
    }

    // Backup & Restore
    fun restoreBackup(jsonString: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = BackupManager.restoreFromJson(repository, jsonString)
            onResult(result)
        }
    }
}
