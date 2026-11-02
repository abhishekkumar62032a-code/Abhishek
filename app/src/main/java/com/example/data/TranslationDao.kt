package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.TranslationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translation_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<TranslationRecord>>

    @Query("SELECT * FROM translation_records WHERE category = :category ORDER BY timestamp DESC")
    fun getRecordsByCategory(category: String): Flow<List<TranslationRecord>>

    @Query("SELECT * FROM translation_records WHERE originalText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchRecords(query: String): Flow<List<TranslationRecord>>

    @Query("SELECT * FROM translation_records WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteRecords(): Flow<List<TranslationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TranslationRecord): Long

    @Update
    suspend fun updateRecord(record: TranslationRecord)

    @Delete
    suspend fun deleteRecord(record: TranslationRecord)

    @Query("DELETE FROM translation_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM translation_records")
    suspend fun clearAll()

    @Query("DELETE FROM translation_records WHERE category = :category")
    suspend fun clearByCategory(category: String)
}
