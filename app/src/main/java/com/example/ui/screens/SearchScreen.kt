package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Note
import com.example.ui.components.AmoledCard
import com.example.ui.components.AmoledChip
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.NotesViewModel

enum class SearchFilter {
    ALL,
    TITLES_ONLY,
    CONTENT_ONLY,
    HAS_TAGS,
    PINNED_ONLY
}

@Composable
fun SearchScreen(
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit,
    onRequireUnlock: (Long) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val unlockedNotes by viewModel.unlockedNotes.collectAsState()

    var activeFilter by remember { mutableStateOf(SearchFilter.ALL) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val filteredResults = remember(searchResults, activeFilter) {
        when (activeFilter) {
            SearchFilter.ALL -> searchResults
            SearchFilter.TITLES_ONLY -> searchResults.filter { it.title.contains(query, ignoreCase = true) }
            SearchFilter.CONTENT_ONLY -> searchResults.filter { it.content.contains(query, ignoreCase = true) }
            SearchFilter.HAS_TAGS -> searchResults.filter { it.tagList.isNotEmpty() }
            SearchFilter.PINNED_ONLY -> searchResults.filter { it.isPinned }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 8.dp)
            ) {
                // Search Input Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            BasicTextField(
                                value = query,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .testTag("search_text_input")
                            )

                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.setSearchQuery("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (query.isEmpty()) {
                            Text(
                                text = "Search notes, folders, tags...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(start = 28.dp)
                            )
                        }
                    }
                }

                // Filter options
                val filterScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(filterScroll)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AmoledChip(
                        text = "All",
                        isSelected = activeFilter == SearchFilter.ALL,
                        onClick = { activeFilter = SearchFilter.ALL }
                    )
                    AmoledChip(
                        text = "In Titles",
                        isSelected = activeFilter == SearchFilter.TITLES_ONLY,
                        onClick = { activeFilter = SearchFilter.TITLES_ONLY }
                    )
                    AmoledChip(
                        text = "In Content",
                        isSelected = activeFilter == SearchFilter.CONTENT_ONLY,
                        onClick = { activeFilter = SearchFilter.CONTENT_ONLY }
                    )
                    AmoledChip(
                        text = "Has Tags",
                        isSelected = activeFilter == SearchFilter.HAS_TAGS,
                        onClick = { activeFilter = SearchFilter.HAS_TAGS }
                    )
                    AmoledChip(
                        text = "Pinned",
                        isSelected = activeFilter == SearchFilter.PINNED_ONLY,
                        onClick = { activeFilter = SearchFilter.PINNED_ONLY }
                    )
                }

                if (query.isNotBlank()) {
                    Text(
                        text = "${filteredResults.size} RESULTS FOR \"${query.uppercase()}\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredResults.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (query.isBlank()) "Type to search notes" else "No notes found for \"$query\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Try searching by keyword, title, tag, or folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredResults, key = { it.id }) { note ->
                        val isLocked = note.isLocked && !unlockedNotes.contains(note.id)

                        val primaryColor = MaterialTheme.colorScheme.primary
                        AmoledCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isLocked) onRequireUnlock(note.id)
                                    else onNavigateToEditor(note.id)
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Title with match highlighting
                                val highlightedTitle = remember(note.displayTitle, query, primaryColor) {
                                    buildHighlightedString(note.displayTitle, query, primaryColor)
                                }
                                Text(
                                    text = if (isLocked) buildAnnotatedString { append("Locked Note") } else highlightedTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Snippet preview with match highlighting
                                val snippet = if (isLocked) "Authenticate to view" else {
                                    findSnippet(note.content, query)
                                }
                                val highlightedSnippet = remember(snippet, query, primaryColor) {
                                    buildHighlightedString(snippet, query, primaryColor)
                                }
                                Text(
                                    text = highlightedSnippet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${note.wordCount} words • ${note.folder}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (note.tagList.isNotEmpty()) {
                                        Text(
                                            text = note.tagList.take(2).joinToString(" ") { "#$it" },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun findSnippet(content: String, query: String): String {
    if (query.isBlank()) return content.take(120)
    val index = content.indexOf(query, ignoreCase = true)
    if (index == -1) return content.take(120)
    val start = maxOf(0, index - 30)
    val end = minOf(content.length, index + query.length + 60)
    val prefix = if (start > 0) "..." else ""
    val suffix = if (end < content.length) "..." else ""
    return "$prefix${content.substring(start, end)}$suffix"
}

private fun buildHighlightedString(
    fullText: String,
    query: String,
    highlightColor: androidx.compose.ui.graphics.Color
): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(fullText)
    return buildAnnotatedString {
        append(fullText)
        var startIndex = 0
        while (startIndex < fullText.length) {
            val match = fullText.indexOf(query, startIndex, ignoreCase = true)
            if (match == -1) break
            val endMatch = match + query.length
            addStyle(
                SpanStyle(
                    fontWeight = FontWeight.Black,
                    textDecoration = TextDecoration.Underline
                ),
                match,
                endMatch
            )
            startIndex = endMatch
        }
    }
}
