package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.TranslationRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HistoryExportHelper {

    enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
        CSV("csv", "text/csv", "CSV Spreadsheet (.csv)"),
        JSON("json", "application/json", "JSON Data (.json)")
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun generateFileName(format: ExportFormat, prefix: String = "translations_export"): String {
        val timestamp = fileTimestampFormat.format(Date())
        return "${prefix}_${timestamp}.${format.extension}"
    }

    /**
     * Converts a list of TranslationRecord entities to a properly formatted CSV string.
     */
    fun convertToCsv(records: List<TranslationRecord>): String {
        val sb = StringBuilder()
        // CSV Header
        sb.append("id,timestamp,formatted_date,category,source_language,target_language,original_text,translated_text,audio_file_path,duration_ms,notes\n")

        for (record in records) {
            val dateStr = dateFormat.format(Date(record.timestamp))
            sb.append(record.id).append(",")
            sb.append(record.timestamp).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(record.category)).append(",")
            sb.append(escapeCsv(record.sourceLangCode)).append(",")
            sb.append(escapeCsv(record.targetLangCode)).append(",")
            sb.append(escapeCsv(record.originalText)).append(",")
            sb.append(escapeCsv(record.translatedText)).append(",")
            sb.append(escapeCsv(record.audioFilePath ?: "")).append(",")
            sb.append(record.durationMs).append(",")
            sb.append(escapeCsv(record.notes ?: ""))
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            str = str.replace("\"", "\"\"")
            return "\"$str\""
        }
        return str
    }

    /**
     * Converts a list of TranslationRecord entities to a human-readable JSON string.
     */
    fun convertToJson(records: List<TranslationRecord>): String {
        val root = JSONObject()
        root.put("app_name", "Smart Voice Translator")
        root.put("export_date", dateFormat.format(Date()))
        root.put("total_records", records.size)

        val jsonArray = JSONArray()
        for (record in records) {
            val item = JSONObject()
            item.put("id", record.id)
            item.put("timestamp", record.timestamp)
            item.put("formatted_date", dateFormat.format(Date(record.timestamp)))
            item.put("category", record.category)
            item.put("source_language", record.sourceLangCode)
            item.put("target_language", record.targetLangCode)
            item.put("original_text", record.originalText)
            item.put("translated_text", record.translatedText)
            item.put("audio_file_path", record.audioFilePath ?: "")
            item.put("duration_ms", record.durationMs)
            item.put("notes", record.notes ?: "")
            item.put("is_favorite", record.isFavorite)
            jsonArray.put(item)
        }

        root.put("records", jsonArray)
        return root.toString(2)
    }

    /**
     * Writes content string to a target document Uri obtained via ActivityResultContracts.CreateDocument.
     */
    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            Log.e("HistoryExportHelper", "Failed to write content to URI: ${e.message}", e)
            false
        }
    }

    /**
     * Creates a shareable temporary file in the cache directory and launches the Android system share sheet.
     */
    fun shareExportFile(context: Context, content: String, fileName: String, mimeType: String) {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Smart Voice Translator History Export")
                putExtra(Intent.EXTRA_TEXT, "यहाँ स्मार्ट वॉयस ट्रांसलेटर से निर्यात किए गए अनुवाद और रिकॉर्डिंग्स हैं।")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "इतिहास निर्यात साझा करें (Share History Export)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("HistoryExportHelper", "Failed to share export file: ${e.message}", e)
        }
    }
}
