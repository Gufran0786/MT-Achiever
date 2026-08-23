package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ArchiveEngine
import com.example.model.CompressionOptions
import com.example.ui.theme.MTCyan
import com.example.ui.theme.ZAGold
import java.io.File

@Composable
fun CompressDialog(
    initialName: String = "Archive",
    onDismiss: () -> Unit,
    onCompress: (options: CompressionOptions, fileName: String) -> Unit
) {
    var fileName by remember { mutableStateOf(initialName) }
    var targetFormat by remember { mutableStateOf("ZIP") }
    var compressionLevel by remember { mutableFloatStateOf(6f) }
    var password by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    tint = ZAGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Archive (ZArchiver)")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Archive Name
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Archive Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Archive Format Selector
                Text("Archive Format:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("ZIP", "TAR", "GZ", "7Z").forEach { fmt ->
                        FilterChip(
                            selected = targetFormat == fmt,
                            onClick = { targetFormat = fmt },
                            label = { Text(fmt, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Compression Level Slider
                val levelLabel = when (compressionLevel.toInt()) {
                    0 -> "0 - Store (No Compression)"
                    1, 2, 3 -> "${compressionLevel.toInt()} - Fast"
                    4, 5, 6 -> "${compressionLevel.toInt()} - Normal"
                    7, 8 -> "${compressionLevel.toInt()} - Maximum"
                    else -> "9 - Ultra (LZMA)"
                }
                Text("Compression Level: $levelLabel", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = compressionLevel,
                    onValueChange = { compressionLevel = it },
                    valueRange = 0f..9f,
                    steps = 8
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Password Encryption (Optional)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Archive Comment
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Archive Comment") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val opts = CompressionOptions(
                        targetFormat = targetFormat,
                        compressionLevel = compressionLevel.toInt(),
                        password = password,
                        comment = comment
                    )
                    onCompress(opts, fileName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZAGold, contentColor = Color.Black)
            ) {
                Text("Compress")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExtractDialog(
    archiveFile: File,
    destDir: File,
    onDismiss: () -> Unit,
    onExtract: (archiveFile: File, targetDir: File) -> Unit
) {
    var extractHere by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf(archiveFile.nameWithoutExtension) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    tint = MTCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Extract Archive")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Archive: ${archiveFile.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Target Directory: ${destDir.absolutePath}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !extractHere,
                        onClick = { extractHere = false },
                        label = { Text("Extract to folder") }
                    )
                    FilterChip(
                        selected = extractHere,
                        onClick = { extractHere = true },
                        label = { Text("Extract Here") }
                    )
                }

                if (!extractHere) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTarget = if (extractHere) destDir else File(destDir, folderName)
                    onExtract(archiveFile, finalTarget)
                }
            ) {
                Text("Extract")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TestArchiveDialog(
    result: ArchiveEngine.IntegrityTestResult?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (result?.isSuccessful == true) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (result?.isSuccessful == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Archive Integrity Test")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (result != null) {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (result.isSuccessful) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Entries Tested: ${result.totalEntries}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "Valid CRC Matches: ${result.passedEntries}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (result.corruptedEntries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Corrupt Entries (${result.corruptedEntries.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        result.corruptedEntries.take(5).forEach {
                            Text(
                                text = "• $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
