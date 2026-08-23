package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileItem
import com.example.model.FileType
import com.example.model.SortMode
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.FileItemRow
import com.example.ui.theme.MTCyan
import com.example.ui.theme.ZAGold
import com.example.viewmodel.MTZUiState
import com.example.viewmodel.MTZViewModel
import com.example.viewmodel.PaneState
import java.io.File

@Composable
fun DualPaneFileExplorer(
    uiState: MTZUiState,
    viewModel: MTZViewModel,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val activePaneIndex = uiState.activePaneIndex
    val activePane = if (activePaneIndex == 0) uiState.paneLeft else uiState.paneRight
    val otherPane = if (activePaneIndex == 0) uiState.paneRight else uiState.paneLeft

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Quick Header / App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MTCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MT",
                            fontWeight = FontWeight.Black,
                            color = MTCyan,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ZAGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Z",
                            fontWeight = FontWeight.Black,
                            color = ZAGold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MT Z-Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.isDualPaneMode) "Dual-Pane Mode" else "Single-Pane Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Header Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.showSearchDialog(true) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { viewModel.loadInstalledApps() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = "App Manager",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleDualPaneMode() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isDualPaneMode) Icons.Default.ViewColumn else Icons.Default.ViewAgenda,
                            contentDescription = "Toggle Layout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Tools",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("⚡ APK Signer Tool") },
                                onClick = {
                                    showMoreMenu = false
                                    val selectedApk = activePane.items.find { activePane.selectedPaths.contains(it.path) && it.isApk }
                                    if (selectedApk != null) {
                                        viewModel.signApk(selectedApk.file)
                                    } else {
                                        viewModel.showMessage("Select an APK file to sign.")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⚖ Compare Panes (Diff Files)") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.comparePanesFiles()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📊 Storage Analyzer") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.showStorageAnalyzerSheet(true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🏷 Batch Rename Files") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.showBatchRenameDialog(true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📁 New Folder") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.showNewFolderDialog(true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📄 New File") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.showNewFileDialog(true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🔄 Refresh Both Panes") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.refreshBothPanes()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (activePane.showHidden) "👁 Hide Hidden Files" else "👁 Show Hidden Files") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.toggleShowHidden(activePaneIndex)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Quick Bookmark Storage Shortcuts
        ScrollableTabRow(
            selectedTabIndex = 0,
            edgePadding = 8.dp,
            divider = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            uiState.bookmarks.forEach { bm ->
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 3.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.navigateTo(activePaneIndex, File(bm.path))
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bm.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Explorer Panes Container
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isDualPaneMode) {
                // Side-by-side Dual Pane View
                Row(modifier = Modifier.fillMaxSize()) {
                    SinglePaneView(
                        paneState = uiState.paneLeft,
                        paneIndex = 0,
                        isActive = uiState.activePaneIndex == 0,
                        accentColor = MTCyan,
                        paneLabel = "Left Pane",
                        onPaneClick = { viewModel.setActivePane(0) },
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )

                    // Vertical Divider Line
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    SinglePaneView(
                        paneState = uiState.paneRight,
                        paneIndex = 1,
                        isActive = uiState.activePaneIndex == 1,
                        accentColor = ZAGold,
                        paneLabel = "Right Pane",
                        onPaneClick = { viewModel.setActivePane(1) },
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Single Pane View with top Pane Switcher Tabs
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = uiState.activePaneIndex,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = uiState.activePaneIndex == 0,
                            onClick = { viewModel.setActivePane(0) },
                            text = { Text("Left Pane (${uiState.paneLeft.items.size})") }
                        )
                        Tab(
                            selected = uiState.activePaneIndex == 1,
                            onClick = { viewModel.setActivePane(1) },
                            text = { Text("Right Pane (${uiState.paneRight.items.size})") }
                        )
                    }

                    SinglePaneView(
                        paneState = activePane,
                        paneIndex = activePaneIndex,
                        isActive = true,
                        accentColor = if (activePaneIndex == 0) MTCyan else ZAGold,
                        paneLabel = if (activePaneIndex == 0) "Left Pane" else "Right Pane",
                        onPaneClick = {},
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Bottom Power Toolbar (MT + ZArchiver action suite)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasSelection = activePane.selectedPaths.isNotEmpty()

                // Side Copy (Left -> Right or Right -> Left)
                IconButton(
                    onClick = { viewModel.copySelectedToOtherPane() },
                    enabled = hasSelection
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Side Copy",
                            tint = if (hasSelection) MTCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Copy ➔",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = if (hasSelection) MTCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }

                // Side Move
                IconButton(
                    onClick = { viewModel.moveSelectedToOtherPane() },
                    enabled = hasSelection
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                            contentDescription = "Side Move",
                            tint = if (hasSelection) ZAGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Move ➔",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = if (hasSelection) ZAGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }

                // Compress (ZArchiver Engine)
                IconButton(
                    onClick = { viewModel.showCompressDialog(true) },
                    enabled = hasSelection
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Compress",
                            tint = if (hasSelection) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Compress",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = if (hasSelection) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }

                // Delete
                IconButton(
                    onClick = { viewModel.showDeleteConfirmDialog(true) },
                    enabled = hasSelection
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (hasSelection) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = if (hasSelection) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }

                // Select All / Deselect
                IconButton(
                    onClick = {
                        if (hasSelection) viewModel.clearSelection(activePaneIndex)
                        else viewModel.selectAll(activePaneIndex)
                    }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (hasSelection) Icons.Default.Close else Icons.Default.SelectAll,
                            contentDescription = "Select All",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (hasSelection) "Clear" else "Select All",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // New Folder/File FAB
                IconButton(
                    onClick = { viewModel.showNewFolderDialog(true) }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "+ Folder",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SinglePaneView(
    paneState: PaneState,
    paneIndex: Int,
    isActive: Boolean,
    accentColor: Color,
    paneLabel: String,
    onPaneClick: () -> Unit,
    viewModel: MTZViewModel,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable { onPaneClick() }
            .background(
                if (isActive) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.background
            )
            .border(
                width = if (isActive) 1.5.dp else 0.dp,
                color = if (isActive) accentColor.copy(alpha = 0.5f) else Color.Transparent
            )
    ) {
        // Pane Title & Nav Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateUp(paneIndex) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Up",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = paneLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )

                    if (paneState.selectedPaths.isNotEmpty()) {
                        Text(
                            text = " (${paneState.selectedPaths.size}/${paneState.items.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Name (A to Z)") },
                                onClick = { showSortMenu = false; viewModel.setSortMode(paneIndex, SortMode.NAME_ASC) }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z to A)") },
                                onClick = { showSortMenu = false; viewModel.setSortMode(paneIndex, SortMode.NAME_DESC) }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Newest first)") },
                                onClick = { showSortMenu = false; viewModel.setSortMode(paneIndex, SortMode.DATE_DESC) }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Oldest first)") },
                                onClick = { showSortMenu = false; viewModel.setSortMode(paneIndex, SortMode.DATE_ASC) }
                            )
                            DropdownMenuItem(
                                text = { Text("Size (Largest first)") },
                                onClick = { showSortMenu = false; viewModel.setSortMode(paneIndex, SortMode.SIZE_DESC) }
                            )
                            DropdownMenuItem(
                                text = { Text("Type (Extension)") },
                                onClick = { showSortMenu = false; viewModel.setSortMode(paneIndex, SortMode.TYPE_ASC) }
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.refreshPane(paneIndex) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Breadcrumb Navigation
        BreadcrumbBar(
            currentDir = paneState.currentPath,
            isInsideArchive = paneState.isInsideArchive,
            archiveName = paneState.archiveFile?.name ?: "",
            archiveInternalPath = paneState.archiveInternalPath,
            onNavigateToDir = { dir -> viewModel.navigateTo(paneIndex, dir) }
        )

        // Loading or File List
        if (paneState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = accentColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (paneState.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Empty folder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(paneState.items, key = { it.path }) { item ->
                    val isSelected = paneState.selectedPaths.contains(item.path)

                    FileItemRow(
                        item = item,
                        isSelected = isSelected,
                        isSelectionMode = paneState.selectedPaths.isNotEmpty(),
                        onItemClick = {
                            if (paneState.selectedPaths.isNotEmpty()) {
                                viewModel.toggleFileSelection(paneIndex, item.path)
                            } else {
                                if (item.isDirectory) {
                                    if (paneState.isInsideArchive && paneState.archiveFile != null) {
                                        viewModel.enterArchive(paneIndex, paneState.archiveFile, item.virtualEntryPath)
                                    } else {
                                        viewModel.navigateTo(paneIndex, item.file)
                                    }
                                } else if (item.isArchive) {
                                    // Open archive directly as virtual folder (ZArchiver core feature!)
                                    viewModel.enterArchive(paneIndex, item.file)
                                } else if (item.fileType == FileType.APK) {
                                    // MT Manager core: open APK analyzer
                                    viewModel.analyzeApk(item.file)
                                } else if (item.fileType in listOf(FileType.CODE, FileType.TEXT)) {
                                    viewModel.openInTextEditor(item)
                                } else if (item.fileType in listOf(FileType.DEX, FileType.BINARY)) {
                                    viewModel.openInHexViewer(item)
                                } else {
                                    viewModel.openInTextEditor(item)
                                }
                            }
                        },
                        onItemLongClick = {
                            viewModel.toggleFileSelection(paneIndex, item.path)
                        },
                        onToggleSelect = {
                            viewModel.toggleFileSelection(paneIndex, item.path)
                        },
                        onOpenTextEditor = { viewModel.openInTextEditor(item) },
                        onOpenHexViewer = { viewModel.openInHexViewer(item) },
                        onAnalyzeApk = { viewModel.analyzeApk(item.file) },
                        onSignApk = { viewModel.signApk(item.file) },
                        onExtractArchive = { viewModel.showExtractDialog(item.file) },
                        onTestArchive = { viewModel.testArchive(item.file) },
                        onCalculateHash = { viewModel.calculateHash(item) },
                        onRename = { viewModel.showRenameDialog(item) },
                        onShowProperties = { viewModel.showFilePropertiesDialog(item) }
                    )
                }
            }
        }
    }
}
