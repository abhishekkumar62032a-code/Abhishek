package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_records")
data class TranslationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceLangCode: String,
    val targetLangCode: String,
    val originalText: String,
    val translatedText: String,
    val audioFilePath: String? = null,
    val category: String = "CHAT", // "RECORDING", "CHAT", "FLOATING", "TOUCH"
    val isFavorite: Boolean = false,
    val durationMs: Long = 0,
    val notes: String? = null
)
