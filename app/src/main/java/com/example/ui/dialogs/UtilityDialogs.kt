package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FileEngine
import com.example.model.FileItem
import com.example.model.HashResult
import com.example.ui.theme.FileColorApk
import com.example.ui.theme.FileColorArchive
import com.example.ui.theme.FileColorCode
import com.example.ui.theme.FileColorDoc
import com.example.ui.theme.FileColorFolder
import com.example.ui.theme.FileColorImage
import com.example.ui.theme.FileColorVideo
import com.example.ui.theme.MTCyan
import com.example.ui.theme.ZAGold
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BatchRenameDialog(
    onDismiss: () -> Unit,
    onRename: (find: String, replace: String, prefix: String, suffix: String, startNum: Int, digits: Int, useRegex: Boolean) -> Unit
) {
    var findPattern by remember { mutableStateOf("") }
    var replaceWith by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var startNum by remember { mutableIntStateOf(1) }
    var digits by remember { mutableIntStateOf(3) }
    var useRegex by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MTCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Batch Rename Tool")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("Prefix (e.g. IMG_)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = suffix,
                    onValueChange = { suffix = it },
                    label = { Text("Suffix") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = findPattern,
                    onValueChange = { findPattern = it },
                    label = { Text("Find Text") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = replaceWith,
                    onValueChange = { replaceWith = it },
                    label = { Text("Replace With") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = useRegex, onCheckedChange = { useRegex = it })
                    Text("Use Regular Expression (Regex)", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onRename(findPattern, replaceWith, prefix, suffix, startNum, digits, useRegex)
            }) {
                Text("Rename All")
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
fun HashCalculatorDialog(
    fileItem: FileItem?,
    hashResult: HashResult?,
    isCalculating: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var compareInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = ZAGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hash & Checksum (MT Core)")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("File: ${fileItem?.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (isCalculating) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ZAGold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Calculating SHA-256, SHA-1, MD5, CRC32...", fontSize = 11.sp)
                        }
                    }
                } else if (hashResult != null) {
                    HashRow(context, "CRC32", hashResult.crc32)
                    HashRow(context, "MD5", hashResult.md5)
                    HashRow(context, "SHA-1", hashResult.sha1)
                    HashRow(context, "SHA-256", hashResult.sha256)

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = compareInput,
                        onValueChange = { compareInput = it.trim() },
                        label = { Text("Compare Hash String") },
                        placeholder = { Text("Paste expected hash...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (compareInput.isNotEmpty()) {
                        val matches = listOf(hashResult.crc32, hashResult.md5, hashResult.sha1, hashResult.sha256)
                            .any { it.equals(compareInput, ignoreCase = true) }

                        Text(
                            text = if (matches) "✓ Hash matches!" else "✗ Hash does NOT match.",
                            color = if (matches) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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

@Composable
fun HashRow(context: Context, label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MTCyan)
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
            }
        }
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun StorageAnalyzerSheet(
    workspaceDir: File,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PieChart, contentDescription = null, tint = MTCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Storage Breakdown")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Storage Categories Distribution", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                CategoryBar("APKs & Apps", 0.45f, FileColorApk, "450 MB")
                CategoryBar("Archives (.zip, .7z)", 0.25f, FileColorArchive, "250 MB")
                CategoryBar("Code & Text (.smali, .kt)", 0.15f, FileColorCode, "150 MB")
                CategoryBar("Images & Media", 0.10f, FileColorImage, "100 MB")
                CategoryBar("Other Files", 0.05f, FileColorDoc, "50 MB")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CategoryBar(name: String, ratio: Float, color: Color, sizeText: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontSize = 11.sp)
            Text(sizeText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun FilePropertiesDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MTCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("File Properties")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Name: ${item.name}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Path: ${item.path}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Type: ${item.fileType.name}", fontSize = 12.sp)
                Text("Size: ${FileEngine.formatFileSize(item.size)} (${item.size} bytes)", fontSize = 12.sp)
                Text("Modified: ${if (item.lastModified > 0) dateFormat.format(Date(item.lastModified)) else "Unknown"}", fontSize = 12.sp)
                Text("Permissions: ${item.permissions} (Chmod: ${if (item.file.canWrite()) "0755" else "0555"})", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SimpleInputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    confirmText: String = "OK",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) }
            ) {
                Text(confirmText)
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
fun ConfirmDeleteDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to permanently delete $selectedCount selected items? This cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
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
fun SearchFilesDialog(
    onDismiss: () -> Unit,
    onSearch: (query: String, isRegex: Boolean, searchContent: Boolean) -> Unit,
    searchResults: List<FileItem>,
    isSearching: Boolean,
    onItemClick: (FileItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    var searchContent by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MTCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search Files & Grep Content")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Enter filename pattern or text...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isRegex, onCheckedChange = { isRegex = it })
                        Text("Regex", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = searchContent, onCheckedChange = { searchContent = it })
                        Text("Search in content (Grep)", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onSearch(query, isRegex, searchContent) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Search", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MTCyan)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (searchResults.isEmpty() && query.isNotEmpty()) {
                            item {
                                Text("No files matched the search.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(searchResults) { res ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onItemClick(res) },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text(res.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(res.path, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
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
