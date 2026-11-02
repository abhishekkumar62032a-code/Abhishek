package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Language
import com.example.model.TranslationRecord
import com.example.ui.components.HeaderBar
import com.example.util.HistoryExportHelper
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allRecords by viewModel.allHistoryRecords.collectAsState()
    val playbackState by viewModel.audioPlayerManager.playbackState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") } // ALL, RECORDING, CHAT, FLOATING
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Export State
    var exportFormat by remember { mutableStateOf(HistoryExportHelper.ExportFormat.CSV) }
    var exportScopeAll by remember { mutableStateOf(true) } // true: all records, false: currently filtered
    var pendingExportContent by remember { mutableStateOf("") }

    val filteredRecords = remember(allRecords, searchQuery, selectedCategory) {
        allRecords.filter { record ->
            val matchesCategory = when (selectedCategory) {
                "RECORDING" -> record.category == "RECORDING"
                "CHAT" -> record.category == "CHAT"
                "FLOATING" -> record.category == "FLOATING"
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                record.originalText.contains(searchQuery, ignoreCase = true) ||
                        record.translatedText.contains(searchQuery, ignoreCase = true)
            }

            matchesCategory && matchesSearch
        }
    }

    // Storage Access Framework Launcher: prompts system save file picker without dangerous permissions
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(exportFormat.mimeType)
    ) { uri: Uri? ->
        if (uri != null && pendingExportContent.isNotEmpty()) {
            val success = HistoryExportHelper.writeToUri(context, uri, pendingExportContent)
            if (success) {
                Toast.makeText(context, "फ़ाइल डिवाइस में सफलतापूर्वक सहेजी गई! (Export Successful)", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "फ़ाइल सहेजने में विफल।", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.audioPlayerManager.stop()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HeaderBar(
            title = "अनुवाद इतिहास (History)",
            onBackClick = { viewModel.navigateBack() },
            actions = {
                if (allRecords.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Export Button
                        IconButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.testTag("history_export_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "निर्यात करें (Export CSV / JSON)",
                                tint = Color(0xFF00E5FF)
                            )
                        }

                        // Clear All Button
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("history_clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "सभी मिटाएँ (Clear All)",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        )

        // Search Bar & Export Info Strip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input"),
                    placeholder = { Text("इतिहास में खोजें...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "साफ करें")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "ALL",
                            onClick = { selectedCategory = "ALL" },
                            label = { Text("सभी (${allRecords.size})") },
                            modifier = Modifier.testTag("filter_all")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == "RECORDING",
                            onClick = { selectedCategory = "RECORDING" },
                            label = { Text("🎙️ रिकॉर्डिंग्स") },
                            modifier = Modifier.testTag("filter_recording")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == "CHAT",
                            onClick = { selectedCategory = "CHAT" },
                            label = { Text("💬 चैट") },
                            modifier = Modifier.testTag("filter_chat")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == "FLOATING",
                            onClick = { selectedCategory = "FLOATING" },
                            label = { Text("⚡ फ्लोटिंग") },
                            modifier = Modifier.testTag("filter_floating")
                        )
                    }
                }
            }
        }

        // List
        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "कोई रिकॉर्ड नहीं मिला",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "वॉयस रिकॉर्डिंग या चैट अनुवाद करने पर इतिहास यहाँ दिखाई देगा।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Export Shortcut Banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showExportDialog = true }
                            .testTag("history_export_banner"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF003543).copy(alpha = 0.45f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "डेटा निर्यात (Export to CSV / JSON)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFE2E8F0)
                                    )
                                    Text(
                                        text = "${filteredRecords.size} अनुवाद और रिकॉर्डिंग डिवाइस पर सहेजें",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF00E5FF).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "निर्यात करें ➔",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF00E5FF),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                items(filteredRecords, key = { it.id }) { record ->
                    HistoryItemCard(
                        record = record,
                        isPlayingThisAudio = playbackState.isPlaying && playbackState.currentFilePath == record.audioFilePath,
                        onPlayAudio = { path ->
                            if (playbackState.isPlaying && playbackState.currentFilePath == path) {
                                viewModel.audioPlayerManager.pause()
                            } else {
                                viewModel.audioPlayerManager.play(path)
                            }
                        },
                        onSpeakText = { text, langCode ->
                            val lang = Language.findByCode(langCode)
                            viewModel.speak(text, lang)
                        },
                        onCopyText = { text ->
                            copyText(context, text)
                            Toast.makeText(context, "टेक्स्ट कॉपी किया गया", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteRecord = {
                            viewModel.deleteHistoryRecord(record)
                            Toast.makeText(context, "रिकॉर्ड इतिहास से हटा दिया गया", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        val targetRecords = if (exportScopeAll) allRecords else filteredRecords

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "इतिहास निर्यात करें (Export)", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "अपने अनुवाद और रिकॉर्डिंग्स का बैकअप CSV स्प्रेडशीट या JSON डेटा फ़ाइल के रूप में सहेजें:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Format Selection Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "फ़ाइल प्रारूप (File Format):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            // CSV Option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportFormat = HistoryExportHelper.ExportFormat.CSV }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = exportFormat == HistoryExportHelper.ExportFormat.CSV,
                                    onClick = { exportFormat = HistoryExportHelper.ExportFormat.CSV }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "CSV (.csv) - Excel / Sheets अनुकूल",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "टेबल फॉर्मेट में आसानी से पढ़ने व प्रिंट करने हेतु",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // JSON Option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportFormat = HistoryExportHelper.ExportFormat.JSON }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = exportFormat == HistoryExportHelper.ExportFormat.JSON,
                                    onClick = { exportFormat = HistoryExportHelper.ExportFormat.JSON }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "JSON (.json) - सम्पूर्ण संरचित डेटा",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "बैकअप और तकनीकी उपयोग हेतु",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Scope Selection (If Filtered)
                    if (filteredRecords.size != allRecords.size) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "रिकॉर्ड दायरा (Records Scope):",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { exportScopeAll = true }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = exportScopeAll, onClick = { exportScopeAll = true })
                                    Text(text = "सभी रिकॉर्ड (${allRecords.size})", style = MaterialTheme.typography.bodySmall)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { exportScopeAll = false }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = !exportScopeAll, onClick = { exportScopeAll = false })
                                    Text(text = "वर्तमान फ़िल्टर किए गए रिकॉर्ड (${filteredRecords.size})", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF064E3B).copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✓ कुल ${targetRecords.size} रिकॉर्ड ${exportFormat.extension.uppercase()} फ़ाइल में निर्यात किए जाएंगे।",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share Export Button
                    OutlinedButton(
                        onClick = {
                            val content = if (exportFormat == HistoryExportHelper.ExportFormat.CSV) {
                                HistoryExportHelper.convertToCsv(targetRecords)
                            } else {
                                HistoryExportHelper.convertToJson(targetRecords)
                            }
                            val fileName = HistoryExportHelper.generateFileName(exportFormat)
                            HistoryExportHelper.shareExportFile(context, content, fileName, exportFormat.mimeType)
                            showExportDialog = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("export_dialog_share_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("शेयर करें", fontSize = 12.sp)
                    }

                    // Save to Device via Storage Access Framework
                    Button(
                        onClick = {
                            val content = if (exportFormat == HistoryExportHelper.ExportFormat.CSV) {
                                HistoryExportHelper.convertToCsv(targetRecords)
                            } else {
                                HistoryExportHelper.convertToJson(targetRecords)
                            }
                            pendingExportContent = content
                            val fileName = HistoryExportHelper.generateFileName(exportFormat)
                            createDocumentLauncher.launch(fileName)
                            showExportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("export_dialog_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color(0xFF0A1128),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("डिवाइस में सहेजें", color = Color(0xFF0A1128), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("इतिहास साफ़ करें?") },
            text = { Text("क्या आप संपूर्ण अनुवाद व रिकॉर्डिंग इतिहास हटाना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "सम्पूर्ण इतिहास साफ़ कर दिया गया", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("हाँ, साफ़ करें", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun HistoryItemCard(
    record: TranslationRecord,
    isPlayingThisAudio: Boolean,
    onPlayAudio: (String) -> Unit,
    onSpeakText: (String, String) -> Unit,
    onCopyText: (String) -> Unit,
    onDeleteRecord: () -> Unit
) {
    val srcLang = Language.findByCode(record.sourceLangCode)
    val tgtLang = Language.findByCode(record.targetLangCode)
    val formattedDate = remember(record.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(record.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_record_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Category badge & Time & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (record.category) {
                            "RECORDING" -> Color(0xFF00E5FF).copy(alpha = 0.15f)
                            "CHAT" -> Color(0xFF38BDF8).copy(alpha = 0.15f)
                            else -> Color(0xFFFF9E00).copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = when (record.category) {
                                "RECORDING" -> "🎙️ रिकॉर्डिंग"
                                "CHAT" -> "💬 चैट"
                                else -> "⚡ फ्लोटिंग"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (record.category) {
                                "RECORDING" -> Color(0xFF00E5FF)
                                "CHAT" -> Color(0xFF38BDF8)
                                else -> Color(0xFFFF9E00)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${srcLang.flagEmoji} ➔ ${tgtLang.flagEmoji} • $formattedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDeleteRecord,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "हटाएँ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Original Text
            Text(
                text = record.originalText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Translated Text
            Text(
                text = record.translatedText,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!record.notes.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "★ ${record.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA855F7)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Replay (if recorded file exists)
                if (!record.audioFilePath.isNullOrEmpty()) {
                    IconButton(
                        onClick = { onPlayAudio(record.audioFilePath) },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (isPlayingThisAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "ऑडियो रिकॉर्डिंग चलाएँ",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // TTS Speak Translated
                IconButton(
                    onClick = { onSpeakText(record.translatedText, record.targetLangCode) },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "अनुवाद सुनें",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Copy Translated Text
                IconButton(
                    onClick = { onCopyText(record.translatedText) },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "अनुवाद कॉपी करें",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Translation", text)
    clipboard.setPrimaryClip(clip)
}
