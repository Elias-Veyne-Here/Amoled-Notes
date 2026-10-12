package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Note
import com.example.ui.components.RichFormattingToolbar
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.FormatAction
import com.example.util.RichTextParser
import com.example.viewmodel.NotesViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (Long) -> Unit,
    onOpenExport: (Note) -> Unit
) {
    val fontSize by viewModel.fontSize.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentNote by remember { mutableStateOf<Note?>(null) }
    var titleState by remember { mutableStateOf("") }
    var contentState by remember { mutableStateOf(TextFieldValue("")) }

    // Undo / Redo history
    val undoStack = remember { mutableListOf<TextFieldValue>() }
    val redoStack = remember { mutableListOf<TextFieldValue>() }
    var isUndoRedoAction by remember { mutableStateOf(false) }

    // Read Mode toggle
    var isReadMode by remember { mutableStateOf(false) }

    // Menu and Dialogs
    var showMenu by remember { mutableStateOf(false) }
    var showFolderTagDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Auto-save debounce state
    var isSaving by remember { mutableStateOf(false) }
    var lastSavedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var autoSaveJob by remember { mutableStateOf<Job?>(null) }

    // Load initial note data
    LaunchedEffect(noteId) {
        val note = viewModel.repository.getNoteByIdImmediate(noteId)
        if (note != null) {
            currentNote = note
            titleState = note.title
            contentState = TextFieldValue(note.content, TextRange(note.content.length))
            // Save initial version snapshot
            viewModel.saveVersionSnapshot(note)
        }
    }

    // Function to persist current note
    fun performSave(immediate: Boolean = false) {
        val note = currentNote ?: return
        val updatedNote = note.copy(
            title = titleState,
            content = contentState.text,
            updatedAt = System.currentTimeMillis()
        )
        currentNote = updatedNote
        viewModel.updateNote(updatedNote)
        lastSavedTimestamp = System.currentTimeMillis()
        isSaving = false

        if (immediate) {
            viewModel.saveVersionSnapshot(updatedNote)
        }
    }

    // Debounced auto-save triggers whenever text changes
    fun onContentChanged(newValue: TextFieldValue) {
        if (!isUndoRedoAction) {
            undoStack.add(contentState)
            redoStack.clear()
            if (undoStack.size > 50) undoStack.removeAt(0)
        }
        isUndoRedoAction = false
        contentState = newValue
        isSaving = true

        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(2000)
            performSave(immediate = false)
        }
    }

    // Periodic 5-minute snapshot during active editing
    LaunchedEffect(noteId) {
        while (true) {
            delay(300000) // 5 minutes
            currentNote?.let { viewModel.saveVersionSnapshot(it) }
        }
    }

    // Save on exit / back
    BackHandler {
        performSave(immediate = true)
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            performSave(immediate = true)
        }
    }

    // Word and character count live calculation
    val wordCount = remember(contentState.text) {
        if (contentState.text.isBlank()) 0
        else contentState.text.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
    }
    val charCount = remember(contentState.text) { contentState.text.length }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            performSave(immediate = true)
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Live word count pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$wordCount words",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Undo button
                        IconButton(
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    val previous = undoStack.removeAt(undoStack.lastIndex)
                                    redoStack.add(contentState)
                                    isUndoRedoAction = true
                                    contentState = previous
                                }
                            },
                            enabled = undoStack.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.Undo,
                                contentDescription = "Undo",
                                tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline
                            )
                        }

                        // Redo button
                        IconButton(
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    val next = redoStack.removeAt(redoStack.lastIndex)
                                    undoStack.add(contentState)
                                    isUndoRedoAction = true
                                    contentState = next
                                }
                            },
                            enabled = redoStack.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.Redo,
                                contentDescription = "Redo",
                                tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline
                            )
                        }

                        // Read Mode Toggle
                        IconButton(
                            onClick = { isReadMode = !isReadMode },
                            modifier = Modifier.testTag("toggle_read_mode")
                        ) {
                            Icon(
                                imageVector = if (isReadMode) Icons.Default.Edit else Icons.Default.AutoStories,
                                contentDescription = if (isReadMode) "Edit Mode" else "Read Mode",
                                tint = if (isReadMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Overflow Menu
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.testTag("editor_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Version History", color = MaterialTheme.colorScheme.onSurface) },
                                    leadingIcon = { Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        showMenu = false
                                        performSave(immediate = true)
                                        currentNote?.let { onNavigateToHistory(it.id) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export & Share", color = MaterialTheme.colorScheme.onSurface) },
                                    leadingIcon = { Icon(Icons.Default.IosShare, null, tint = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        showMenu = false
                                        performSave(immediate = true)
                                        currentNote?.let { onOpenExport(it) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Folder & Tags", color = MaterialTheme.colorScheme.onSurface) },
                                    leadingIcon = { Icon(Icons.Default.Label, null, tint = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        showMenu = false
                                        showFolderTagDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        val isLocked = currentNote?.isLocked == true
                                        Text(if (isLocked) "Unlock Note" else "Lock Note", color = MaterialTheme.colorScheme.onSurface)
                                    },
                                    leadingIcon = {
                                        val isLocked = currentNote?.isLocked == true
                                        Icon(if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurface)
                                    },
                                    onClick = {
                                        showMenu = false
                                        currentNote?.let {
                                            viewModel.toggleLock(it)
                                            currentNote = it.copy(isLocked = !it.isLocked)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        val isPinned = currentNote?.isPinned == true
                                        Text(if (isPinned) "Unpin Note" else "Pin to Top", color = MaterialTheme.colorScheme.onSurface)
                                    },
                                    leadingIcon = { Icon(Icons.Default.PushPin, null, tint = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        showMenu = false
                                        currentNote?.let {
                                            viewModel.togglePin(it) {
                                                scope.launch { snackbarHostState.showSnackbar("Maximum 3 pinned notes") }
                                            }
                                            currentNote = it.copy(isPinned = !it.isPinned)
                                        }
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                DropdownMenuItem(
                                    text = { Text("Delete Note", color = Color(0xFFFF5252)) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252)) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        },
        bottomBar = {
            if (!isReadMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    RichFormattingToolbar(
                        onFormatClick = { action ->
                            val updated = RichTextParser.applyFormat(contentState, action)
                            onContentChanged(updated)
                        }
                    )

                    // Footer with auto-save & character status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isSaving) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSaving) "Saving..." else "Auto-saved",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "$charCount chars • unlimited capacity",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isReadMode) {
                // Distraction-free lazy reader with 60fps performance for 100,000+ words
                val onBackgroundColor = MaterialTheme.colorScheme.onBackground
                val blocks = remember(contentState.text, onBackgroundColor) {
                    RichTextParser.parseToBlocks(contentState.text, onBackgroundColor)
                }

                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("read_mode_container"),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (titleState.isNotBlank()) {
                        item {
                            Text(
                                text = titleState,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }

                    items(blocks, key = { it.id }) { block ->
                        when {
                            block.isDivider -> {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                            block.isCheckbox -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val toggledText = RichTextParser.toggleCheckboxAtLine(contentState.text, block.id)
                                            onContentChanged(TextFieldValue(toggledText, TextRange(toggledText.length)))
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (block.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (block.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = block.annotated,
                                        fontSize = fontSize.sizeSp.sp,
                                        lineHeight = fontSize.lineHeightSp.sp,
                                        color = if (block.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = block.annotated,
                                    fontSize = fontSize.sizeSp.sp,
                                    lineHeight = fontSize.lineHeightSp.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            } else {
                // Unlimited capacity editable note area
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Title Input Field
                    BasicTextField(
                        value = titleState,
                        onValueChange = {
                            titleState = it
                            isSaving = true
                            autoSaveJob?.cancel()
                            autoSaveJob = scope.launch {
                                delay(2000)
                                performSave(immediate = false)
                            }
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_title_input"),
                        decorationBox = { innerTextField ->
                            if (titleState.isEmpty()) {
                                Text(
                                    text = "Title",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Main Content Editor Field
                    BasicTextField(
                        value = contentState,
                        onValueChange = { onContentChanged(it) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = fontSize.sizeSp.sp,
                            lineHeight = fontSize.lineHeightSp.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .testTag("note_content_input"),
                        decorationBox = { innerTextField ->
                            if (contentState.text.isEmpty()) {
                                Text(
                                    text = "Start writing with unlimited capacity...",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.outline,
                                        fontSize = fontSize.sizeSp.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Extra space at bottom to keep text comfortably above keyboard
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }

    // Folder and Tags assignment dialog
    if (showFolderTagDialog) {
        val folders by viewModel.folders.collectAsState()
        var tempFolder by remember { mutableStateOf(currentNote?.folder ?: "Default") }
        var tempTags by remember { mutableStateOf(currentNote?.tags ?: "") }

        AlertDialog(
            onDismissRequest = { showFolderTagDialog = false },
            title = { Text("Organize Note", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Folder:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    OutlinedTextField(
                        value = tempFolder,
                        onValueChange = { tempFolder = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Tags (comma-separated):", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    OutlinedTextField(
                        value = tempTags,
                        onValueChange = { tempTags = it },
                        singleLine = true,
                        placeholder = { Text("work, ideas, draft", color = MaterialTheme.colorScheme.outline) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentNote?.let {
                            val updated = it.copy(folder = tempFolder.trim(), tags = tempTags.trim())
                            currentNote = updated
                            viewModel.updateNote(updated)
                        }
                        showFolderTagDialog = false
                    }
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderTagDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Note?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this note?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        currentNote?.let { viewModel.deleteNote(it) }
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
