package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FileEngine
import com.example.model.FileItem
import com.example.model.FileType
import com.example.ui.theme.FileColorApk
import com.example.ui.theme.FileColorArchive
import com.example.ui.theme.FileColorAudio
import com.example.ui.theme.FileColorBin
import com.example.ui.theme.FileColorCode
import com.example.ui.theme.FileColorDex
import com.example.ui.theme.FileColorDoc
import com.example.ui.theme.FileColorFolder
import com.example.ui.theme.FileColorImage
import com.example.ui.theme.FileColorText
import com.example.ui.theme.FileColorVideo
import com.example.ui.theme.MTCyan
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BreadcrumbBar(
    currentDir: File,
    isInsideArchive: Boolean,
    archiveName: String,
    archiveInternalPath: String,
    onNavigateToDir: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = MTCyan,
                modifier = Modifier.size(18.dp)
            )

            if (!isInsideArchive) {
                val segments = currentDir.absolutePath.split("/").filter { it.isNotEmpty() }
                var accumulated = ""
                segments.forEachIndexed { index, seg ->
                    accumulated += "/$seg"
                    val target = File(accumulated)
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (seg == "0" && accumulated.contains("emulated")) "Internal Storage" else seg,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (index == segments.lastIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == segments.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .combinedClickable { onNavigateToDir(target) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            } else {
                // Inside virtual archive
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "📦 $archiveName",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                if (archiveInternalPath.isNotEmpty()) {
                    val archiveSegments = archiveInternalPath.split("/").filter { it.isNotEmpty() }
                    archiveSegments.forEach { seg ->
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = seg,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onOpenTextEditor: () -> Unit,
    onOpenHexViewer: () -> Unit,
    onAnalyzeApk: () -> Unit,
    onSignApk: () -> Unit,
    onExtractArchive: () -> Unit,
    onTestArchive: () -> Unit,
    onCalculateHash: () -> Unit,
    onRename: () -> Unit,
    onShowProperties: () -> Unit,
    onOpenMedia: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val (icon, tint) = getFileIconAndColor(item.fileType)

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dateStr = remember(item.lastModified) {
        if (item.lastModified > 0) dateFormat.format(Date(item.lastModified)) else ""
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            ),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox or selection indicator
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // File Icon Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.fileType.name,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Name and Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (item.isDirectory) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (item.isDirectory) "Folder" else FileEngine.formatFileSize(item.size),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Text(
                        text = "$dateStr  ${item.permissions}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Context Menu Button
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (item.fileType == FileType.APK) {
                        DropdownMenuItem(
                            text = { Text("⚡ APK Analyzer (Manifest & Dex)") },
                            onClick = { showMenu = false; onAnalyzeApk() }
                        )
                        DropdownMenuItem(
                            text = { Text("✍ Sign APK (V1/V2 Testkey)") },
                            onClick = { showMenu = false; onSignApk() }
                        )
                    }

                    if (item.isArchive) {
                        DropdownMenuItem(
                            text = { Text("📦 Extract All...") },
                            onClick = { showMenu = false; onExtractArchive() }
                        )
                        DropdownMenuItem(
                            text = { Text("🛡 Test Archive Integrity") },
                            onClick = { showMenu = false; onTestArchive() }
                        )
                    }

                    if (!item.isDirectory) {
                        if (item.fileType in listOf(FileType.IMAGE, FileType.VIDEO, FileType.AUDIO, FileType.DOCUMENT)) {
                            DropdownMenuItem(
                                text = {
                                    val label = when (item.fileType) {
                                        FileType.IMAGE -> "🖼 Open in Photo Viewer"
                                        FileType.VIDEO -> "🎬 Play in Video Player"
                                        FileType.AUDIO -> "🎵 Play in Music Player"
                                        FileType.DOCUMENT -> "📄 Open in PDF / Reader"
                                        else -> "▶ Open in Player"
                                    }
                                    Text(label)
                                },
                                onClick = { showMenu = false; onOpenMedia() }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("📝 Text / Code Editor") },
                            onClick = { showMenu = false; onOpenTextEditor() }
                        )
                        DropdownMenuItem(
                            text = { Text("🔍 Hex Viewer & Editor") },
                            onClick = { showMenu = false; onOpenHexViewer() }
                        )
                        DropdownMenuItem(
                            text = { Text("🔑 Calculate Hashes (MD5/SHA/CRC)") },
                            onClick = { showMenu = false; onCalculateHash() }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("✏ Rename") },
                        onClick = { showMenu = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("ℹ Properties & Details") },
                        onClick = { showMenu = false; onShowProperties() }
                    )
                }
            }
        }
    }
}

fun getFileIconAndColor(type: FileType): Pair<ImageVector, Color> {
    return when (type) {
        FileType.FOLDER -> Pair(Icons.Default.Folder, FileColorFolder)
        FileType.ARCHIVE -> Pair(Icons.Default.Archive, FileColorArchive)
        FileType.APK -> Pair(Icons.Default.Android, FileColorApk)
        FileType.CODE -> Pair(Icons.Default.Code, FileColorCode)
        FileType.TEXT -> Pair(Icons.Default.Description, FileColorText)
        FileType.IMAGE -> Pair(Icons.Default.Image, FileColorImage)
        FileType.AUDIO -> Pair(Icons.Default.AudioFile, FileColorAudio)
        FileType.VIDEO -> Pair(Icons.Default.VideoFile, FileColorVideo)
        FileType.DOCUMENT -> Pair(Icons.Default.Description, FileColorDoc)
        FileType.DEX -> Pair(Icons.Default.Memory, FileColorDex)
        FileType.BINARY -> Pair(Icons.Default.Memory, FileColorBin)
        FileType.UNKNOWN -> Pair(Icons.Default.Description, FileColorBin)
    }
}
