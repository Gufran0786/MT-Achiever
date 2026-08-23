package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.dialogs.ApkAnalyzerDialog
import com.example.ui.dialogs.BatchRenameDialog
import com.example.ui.dialogs.CompressDialog
import com.example.ui.dialogs.ConfirmDeleteDialog
import com.example.ui.dialogs.ExtractDialog
import com.example.ui.dialogs.FilePropertiesDialog
import com.example.ui.dialogs.HashCalculatorDialog
import com.example.ui.dialogs.InstalledAppsSheet
import com.example.ui.dialogs.SearchFilesDialog
import com.example.ui.dialogs.SimpleInputDialog
import com.example.ui.dialogs.StorageAnalyzerSheet
import com.example.ui.dialogs.TestArchiveDialog
import com.example.ui.screens.DualPaneFileExplorer
import com.example.ui.screens.FileDiffScreen
import com.example.ui.screens.HexViewerScreen
import com.example.ui.screens.TextEditorScreen
import com.example.ui.theme.MTCyan
import com.example.viewmodel.ActiveScreen
import com.example.viewmodel.MTZViewModel

@Composable
fun MTZMainScreen(
    viewModel: MTZViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.activeScreen) {
                ActiveScreen.EXPLORER -> {
                    DualPaneFileExplorer(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ActiveScreen.TEXT_EDITOR -> {
                    TextEditorScreen(
                        title = uiState.editorTitle,
                        content = uiState.editorContent,
                        isModified = uiState.editorIsModified,
                        onContentChange = { viewModel.updateEditorContent(it) },
                        onSave = { viewModel.saveEditorContent() },
                        onClose = { viewModel.closeActiveTool() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ActiveScreen.HEX_VIEWER -> {
                    HexViewerScreen(
                        title = uiState.hexTitle,
                        bytes = uiState.hexBytes,
                        onClose = { viewModel.closeActiveTool() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ActiveScreen.DIFF_VIEWER -> {
                    FileDiffScreen(
                        fileA = uiState.diffFileA,
                        fileB = uiState.diffFileB,
                        diffLines = uiState.diffLines,
                        onClose = { viewModel.closeActiveTool() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Operation Progress Banner Overlay
            AnimatedVisibility(
                visible = uiState.isOperationInProgress,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MTCyan,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.operationProgressText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Dialogs & Sheets
        if (uiState.showNewFolderDialog) {
            SimpleInputDialog(
                title = "Create New Folder",
                label = "Folder Name",
                initialValue = "New_Folder",
                confirmText = "Create",
                onDismiss = { viewModel.showNewFolderDialog(false) },
                onConfirm = { name -> viewModel.createFolder(name) }
            )
        }

        if (uiState.showNewFileDialog) {
            SimpleInputDialog(
                title = "Create New File",
                label = "File Name",
                initialValue = "script.sh",
                confirmText = "Create",
                onDismiss = { viewModel.showNewFileDialog(false) },
                onConfirm = { name -> viewModel.createFile(name) }
            )
        }

        if (uiState.showRenameDialog && uiState.targetFileForRename != null) {
            val target = uiState.targetFileForRename!!
            SimpleInputDialog(
                title = "Rename File",
                label = "New Name",
                initialValue = target.name,
                confirmText = "Rename",
                onDismiss = { viewModel.showRenameDialog(null) },
                onConfirm = { newName -> viewModel.renameFile(target.file, newName) }
            )
        }

        if (uiState.showDeleteConfirmDialog) {
            val activePane = if (uiState.activePaneIndex == 0) uiState.paneLeft else uiState.paneRight
            ConfirmDeleteDialog(
                selectedCount = activePane.selectedPaths.size,
                onDismiss = { viewModel.showDeleteConfirmDialog(false) },
                onConfirm = { viewModel.deleteSelected(uiState.activePaneIndex) }
            )
        }

        if (uiState.showCompressDialog) {
            CompressDialog(
                initialName = "Archive_${System.currentTimeMillis() % 1000}",
                onDismiss = { viewModel.showCompressDialog(false) },
                onCompress = { options, fileName ->
                    viewModel.compressSelected(options, fileName)
                }
            )
        }

        if (uiState.showExtractDialog && uiState.targetArchiveForExtract != null) {
            val destPane = if (uiState.activePaneIndex == 0) uiState.paneRight else uiState.paneLeft
            ExtractDialog(
                archiveFile = uiState.targetArchiveForExtract!!,
                destDir = destPane.currentPath,
                onDismiss = { viewModel.showExtractDialog(null) },
                onExtract = { archive, targetDir ->
                    viewModel.extractArchive(archive, targetDir)
                }
            )
        }

        if (uiState.showTestArchiveDialog) {
            TestArchiveDialog(
                result = uiState.testArchiveResult,
                onDismiss = { viewModel.showTestArchiveDialog(false) }
            )
        }

        if (uiState.showApkAnalyzerDialog && uiState.activeApkInfo != null) {
            val apkInfo = uiState.activeApkInfo!!
            ApkAnalyzerDialog(
                apkInfo = apkInfo,
                onSignApk = {
                    viewModel.showApkAnalyzerDialog(false)
                    viewModel.signApk(apkInfo.file)
                },
                onOpenManifestEditor = { xmlContent ->
                    viewModel.showApkAnalyzerDialog(false)
                    viewModel.openInTextEditor(
                        com.example.model.FileItem(
                            file = apkInfo.file,
                            name = "${apkInfo.appName}_Manifest.xml"
                        )
                    )
                },
                onDismiss = { viewModel.showApkAnalyzerDialog(false) }
            )
        }

        if (uiState.showInstalledAppsSheet) {
            InstalledAppsSheet(
                apps = uiState.installedApps,
                isLoading = uiState.isLoadingInstalledApps,
                onExtractApk = { app -> viewModel.extractInstalledApp(app) },
                onDismiss = { viewModel.showInstalledAppsSheet(false) }
            )
        }

        if (uiState.showHashCalculatorDialog) {
            HashCalculatorDialog(
                fileItem = uiState.targetFileForHash,
                hashResult = uiState.hashResult,
                isCalculating = uiState.isCalculatingHash,
                onDismiss = { viewModel.showHashCalculatorDialog(false) }
            )
        }

        if (uiState.showBatchRenameDialog) {
            BatchRenameDialog(
                onDismiss = { viewModel.showBatchRenameDialog(false) },
                onRename = { find, replace, prefix, suffix, startNum, digits, useRegex ->
                    viewModel.batchRename(find, replace, prefix, suffix, startNum, digits, useRegex)
                }
            )
        }

        if (uiState.showStorageAnalyzerSheet) {
            val activePane = if (uiState.activePaneIndex == 0) uiState.paneLeft else uiState.paneRight
            StorageAnalyzerSheet(
                workspaceDir = activePane.currentPath,
                onDismiss = { viewModel.showStorageAnalyzerSheet(false) }
            )
        }

        if (uiState.showFilePropertiesDialog && uiState.targetFileForProperties != null) {
            FilePropertiesDialog(
                item = uiState.targetFileForProperties!!,
                onDismiss = { viewModel.showFilePropertiesDialog(null) }
            )
        }

        if (uiState.showSearchDialog) {
            SearchFilesDialog(
                onDismiss = { viewModel.showSearchDialog(false) },
                onSearch = { q, regex, content ->
                    viewModel.performSearch(q, regex, content)
                },
                searchResults = uiState.searchResults,
                isSearching = uiState.isSearching,
                onItemClick = { fileItem ->
                    viewModel.showSearchDialog(false)
                    if (fileItem.isDirectory) {
                        viewModel.navigateTo(uiState.activePaneIndex, fileItem.file)
                    } else if (fileItem.isArchive) {
                        viewModel.enterArchive(uiState.activePaneIndex, fileItem.file)
                    } else {
                        viewModel.openInTextEditor(fileItem)
                    }
                }
            )
        }
    }
}
