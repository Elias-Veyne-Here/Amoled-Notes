package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["isPinned", "updatedAt"]),
        Index(value = ["isArchived", "updatedAt"]),
        Index(value = ["folder"]),
        Index(value = ["updatedAt"])
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val folder: String = "Default",
    val tags: String = "", // Comma-separated list of tags
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val wordCount: Int
        get() {
            if (content.isBlank()) return 0
            return content.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
        }

    val charCount: Int
        get() = content.length

    val tagList: List<String>
        get() = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val displayTitle: String
        get() {
            if (title.isNotBlank()) return title
            val firstLine = content.lines().firstOrNull { it.isNotBlank() }
            return firstLine?.take(50)?.trim() ?: "Untitled Note"
        }

    val displayPreview: String
        get() {
            val lines = content.lines().filter { it.isNotBlank() }
            val relevantLines = if (title.isBlank() && lines.isNotEmpty()) lines.drop(1) else lines
            return relevantLines.take(2).joinToString(" ").take(140)
        }
}
