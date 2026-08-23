package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.ApkEngine
import com.example.engine.ArchiveEngine
import com.example.engine.DiffEngine
import com.example.engine.FileEngine
import com.example.model.ApkInfo
import com.example.model.BookmarkItem
import com.example.model.CompressionOptions
import com.example.model.FileDiffLine
import com.example.model.FileItem
import com.example.model.HashResult
import com.example.model.InstalledAppItem
import com.example.model.SortMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class PaneState(
    val currentPath: File,
    val items: List<FileItem> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val isInsideArchive: Boolean = false,
    val archiveFile: File? = null,
    val archiveInternalPath: String = "",
    val sortMode: SortMode = SortMode.NAME_ASC,
    val showHidden: Boolean = false,
    val isLoading: Boolean = false
)

enum class ActiveScreen {
    EXPLORER,
    TEXT_EDITOR,
    HEX_VIEWER,
    DIFF_VIEWER
}

data class MTZUiState(
    val activeScreen: ActiveScreen = ActiveScreen.EXPLORER,
    val isDualPaneMode: Boolean = true,
    val activePaneIndex: Int = 0, // 0 = Left, 1 = Right
    val paneLeft: PaneState,
    val paneRight: PaneState,
    val bookmarks: List<BookmarkItem> = emptyList(),
    val statusMessage: String? = null,
    val isOperationInProgress: Boolean = false,
    val operationProgressText: String = "",

    // Dialog & Tool States
    val showNewFolderDialog: Boolean = false,
    val showNewFileDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val targetFileForRename: FileItem? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val showCompressDialog: Boolean = false,
    val showExtractDialog: Boolean = false,
    val targetArchiveForExtract: File? = null,
    val showTestArchiveDialog: Boolean = false,
    val testArchiveResult: ArchiveEngine.IntegrityTestResult? = null,
    val showApkAnalyzerDialog: Boolean = false,
    val activeApkInfo: ApkInfo? = null,
    val showInstalledAppsSheet: Boolean = false,
    val installedApps: List<InstalledAppItem> = emptyList(),
    val isLoadingInstalledApps: Boolean = false,
    val showHashCalculatorDialog: Boolean = false,
    val hashResult: HashResult? = null,
    val isCalculatingHash: Boolean = false,
    val targetFileForHash: FileItem? = null,
    val showBatchRenameDialog: Boolean = false,
    val showStorageAnalyzerSheet: Boolean = false,
    val showFilePropertiesDialog: Boolean = false,
    val targetFileForProperties: FileItem? = null,
    val showPermissionsDialog: Boolean = false,
    val showSearchDialog: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<FileItem> = emptyList(),
    val isSearching: Boolean = false,

    // Text Editor State
    val editorFile: File? = null,
    val editorVirtualArchive: Pair<File, String>? = null,
    val editorTitle: String = "",
    val editorContent: String = "",
    val editorIsModified: Boolean = false,

    // Hex Viewer State
    val hexFile: File? = null,
    val hexVirtualArchive: Pair<File, String>? = null,
    val hexBytes: ByteArray = ByteArray(0),
    val hexTitle: String = "",

    // Diff Viewer State
    val diffFileA: File? = null,
    val diffFileB: File? = null,
    val diffLines: List<FileDiffLine> = emptyList()
)

class MTZViewModel(application: Application) : AndroidViewModel(application) {

    private val defaultRoot: File = FileEngine.getDefaultStoragePath()
    private val appWorkspace: File

    private val _uiState: MutableStateFlow<MTZUiState>

    init {
        // Initialize sample workspace or fallback to default
        val workspace = File(application.filesDir, "MTZ_Workspace")
        appWorkspace = workspace

        val initialLeft = if (workspace.exists()) workspace else defaultRoot
        val initialRight = defaultRoot

        _uiState = MutableStateFlow(
            MTZUiState(
                paneLeft = PaneState(currentPath = initialLeft),
                paneRight = PaneState(currentPath = initialRight),
                bookmarks = listOf(
                    BookmarkItem("MTZ Workspace", workspace.absolutePath, "CODE"),
                    BookmarkItem("Internal Storage", defaultRoot.absolutePath, "STORAGE"),
                    BookmarkItem("Downloads", File(defaultRoot, "Download").absolutePath, "DOWNLOAD"),
                    BookmarkItem("DCIM (Camera)", File(defaultRoot, "DCIM").absolutePath, "CAMERA"),
                    BookmarkItem("Android/data", File(defaultRoot, "Android/data").absolutePath, "ANDROID"),
                    BookmarkItem("App Internal Files", application.filesDir.absolutePath, "FOLDER")
                )
            )
        )

        viewModelScope.launch {
            FileEngine.setupSampleWorkspace(getApplication())
            refreshPane(0)
            refreshPane(1)
        }
    }

    val uiState: StateFlow<MTZUiState> = _uiState.asStateFlow()

    fun toggleDualPaneMode() {
        _uiState.update { it.copy(isDualPaneMode = !it.isDualPaneMode) }
    }

    fun setActivePane(index: Int) {
        _uiState.update { it.copy(activePaneIndex = index) }
    }

    fun navigateTo(paneIndex: Int, newPath: File) {
        if (!newPath.exists()) return
        updatePane(paneIndex) {
            it.copy(
                currentPath = newPath,
                isInsideArchive = false,
                archiveFile = null,
                archiveInternalPath = "",
                selectedPaths = emptySet()
            )
        }
        refreshPane(paneIndex)
    }

    fun navigateUp(paneIndex: Int) {
        val pane = getPane(paneIndex)
        if (pane.isInsideArchive) {
            val clean = pane.archiveInternalPath.trim('/')
            if (clean.isEmpty() || !clean.contains('/')) {
                // Exit archive back to directory containing archive
                updatePane(paneIndex) {
                    it.copy(
                        isInsideArchive = false,
                        archiveFile = null,
                        archiveInternalPath = "",
                        selectedPaths = emptySet()
                    )
                }
                refreshPane(paneIndex)
            } else {
                val parentSubPath = clean.substringBeforeLast('/') + "/"
                updatePane(paneIndex) {
                    it.copy(archiveInternalPath = parentSubPath, selectedPaths = emptySet())
                }
                refreshPane(paneIndex)
            }
        } else {
            val parent = pane.currentPath.parentFile
            if (parent != null && parent.canRead()) {
                navigateTo(paneIndex, parent)
            }
        }
    }

    fun enterArchive(paneIndex: Int, archiveFile: File, internalSubPath: String = "") {
        updatePane(paneIndex) {
            it.copy(
                isInsideArchive = true,
                archiveFile = archiveFile,
                archiveInternalPath = internalSubPath,
                selectedPaths = emptySet()
            )
        }
        refreshPane(paneIndex)
    }

    fun toggleFileSelection(paneIndex: Int, filePath: String) {
        updatePane(paneIndex) { pane ->
            val updated = pane.selectedPaths.toMutableSet()
            if (updated.contains(filePath)) {
                updated.remove(filePath)
            } else {
                updated.add(filePath)
            }
            pane.copy(selectedPaths = updated)
        }
    }

    fun selectAll(paneIndex: Int) {
        updatePane(paneIndex) { pane ->
            val allPaths = pane.items.map { it.path }.toSet()
            pane.copy(selectedPaths = allPaths)
        }
    }

    fun clearSelection(paneIndex: Int) {
        updatePane(paneIndex) { pane ->
            pane.copy(selectedPaths = emptySet())
        }
    }

    fun setSortMode(paneIndex: Int, mode: SortMode) {
        updatePane(paneIndex) { it.copy(sortMode = mode) }
        refreshPane(paneIndex)
    }

    fun toggleShowHidden(paneIndex: Int) {
        updatePane(paneIndex) { it.copy(showHidden = !it.showHidden) }
        refreshPane(paneIndex)
    }

    fun refreshPane(paneIndex: Int) {
        viewModelScope.launch {
            val pane = getPane(paneIndex)
            updatePane(paneIndex) { it.copy(isLoading = true) }

            val items = if (pane.isInsideArchive && pane.archiveFile != null) {
                ArchiveEngine.getVirtualDirectoryItems(pane.archiveFile, pane.archiveInternalPath)
            } else {
                FileEngine.getDirectoryContents(
                    dir = pane.currentPath,
                    showHidden = pane.showHidden,
                    sortMode = pane.sortMode
                )
            }

            updatePane(paneIndex) { it.copy(items = items, isLoading = false) }
        }
    }

    fun refreshBothPanes() {
        refreshPane(0)
        refreshPane(1)
    }

    // Cross-Pane Actions (MT Manager Power Duality)
    fun copySelectedToOtherPane() {
        viewModelScope.launch {
            val srcIndex = _uiState.value.activePaneIndex
            val destIndex = 1 - srcIndex
            val srcPane = getPane(srcIndex)
            val destPane = getPane(destIndex)

            val selectedItems = srcPane.items.filter { srcPane.selectedPaths.contains(it.path) }
            if (selectedItems.isEmpty()) {
                showMessage("No items selected to copy.")
                return@launch
            }

            _uiState.update {
                it.copy(isOperationInProgress = true, operationProgressText = "Copying ${selectedItems.size} items...")
            }

            var successCount = 0
            for (item in selectedItems) {
                if (destPane.isInsideArchive && destPane.archiveFile != null) {
                    // Add into archive
                    val zipEntryPath = "${destPane.archiveInternalPath}${item.name}"
                    if (ArchiveEngine.addOrUpdateFileInZip(destPane.archiveFile, item.file, zipEntryPath)) {
                        successCount++
                    }
                } else {
                    if (FileEngine.copyFileOrDirectory(item.file, destPane.currentPath)) {
                        successCount++
                    }
                }
            }

            _uiState.update { it.copy(isOperationInProgress = false) }
            showMessage("Copied $successCount items to ${destPane.currentPath.name}")
            clearSelection(srcIndex)
            refreshBothPanes()
        }
    }

    fun moveSelectedToOtherPane() {
        viewModelScope.launch {
            val srcIndex = _uiState.value.activePaneIndex
            val destIndex = 1 - srcIndex
            val srcPane = getPane(srcIndex)
            val destPane = getPane(destIndex)

            val selectedItems = srcPane.items.filter { srcPane.selectedPaths.contains(it.path) }
            if (selectedItems.isEmpty()) {
                showMessage("No items selected to move.")
                return@launch
            }

            _uiState.update {
                it.copy(isOperationInProgress = true, operationProgressText = "Moving ${selectedItems.size} items...")
            }

            var successCount = 0
            for (item in selectedItems) {
                if (FileEngine.moveFileOrDirectory(item.file, destPane.currentPath)) {
                    successCount++
                }
            }

            _uiState.update { it.copy(isOperationInProgress = false) }
            showMessage("Moved $successCount items to ${destPane.currentPath.name}")
            clearSelection(srcIndex)
            refreshBothPanes()
        }
    }

    fun deleteSelected(paneIndex: Int) {
        viewModelScope.launch {
            val pane = getPane(paneIndex)
            val selected = pane.items.filter { pane.selectedPaths.contains(it.path) }
            if (selected.isEmpty()) return@launch

            _uiState.update { it.copy(isOperationInProgress = true, operationProgressText = "Deleting files...") }

            if (pane.isInsideArchive && pane.archiveFile != null) {
                for (item in selected) {
                    ArchiveEngine.deleteEntryFromZip(pane.archiveFile, item.virtualEntryPath)
                }
            } else {
                FileEngine.deleteFiles(selected.map { it.file })
            }

            _uiState.update { it.copy(isOperationInProgress = false, showDeleteConfirmDialog = false) }
            showMessage("Deleted ${selected.size} items.")
            clearSelection(paneIndex)
            refreshPane(paneIndex)
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val activePane = getActivePane()
            if (FileEngine.createNewFolder(activePane.currentPath, name)) {
                showMessage("Folder '$name' created.")
                refreshPane(_uiState.value.activePaneIndex)
            } else {
                showMessage("Failed to create folder.")
            }
            _uiState.update { it.copy(showNewFolderDialog = false) }
        }
    }

    fun createFile(name: String, content: String = "") {
        viewModelScope.launch {
            val activePane = getActivePane()
            if (FileEngine.createNewFile(activePane.currentPath, name, content)) {
                showMessage("File '$name' created.")
                refreshPane(_uiState.value.activePaneIndex)
            } else {
                showMessage("Failed to create file.")
            }
            _uiState.update { it.copy(showNewFileDialog = false) }
        }
    }

    fun renameFile(target: File, newName: String) {
        viewModelScope.launch {
            if (FileEngine.renameFile(target, newName)) {
                showMessage("Renamed to '$newName'.")
                refreshBothPanes()
            } else {
                showMessage("Failed to rename.")
            }
            _uiState.update { it.copy(showRenameDialog = false, targetFileForRename = null) }
        }
    }

    fun batchRename(
        findPattern: String,
        replaceWith: String,
        prefix: String,
        suffix: String,
        startNumber: Int,
        digits: Int,
        useRegex: Boolean
    ) {
        viewModelScope.launch {
            val pane = getActivePane()
            val selected = pane.items.filter { pane.selectedPaths.contains(it.path) }.map { it.file }
            val targets = if (selected.isNotEmpty()) selected else pane.items.map { it.file }

            _uiState.update { it.copy(isOperationInProgress = true, operationProgressText = "Batch renaming...") }
            val count = FileEngine.batchRename(
                files = targets,
                findPattern = findPattern,
                replaceWith = replaceWith,
                prefix = prefix,
                suffix = suffix,
                startNumber = startNumber,
                digits = digits,
                useRegex = useRegex
            )
            _uiState.update { it.copy(isOperationInProgress = false, showBatchRenameDialog = false) }
            showMessage("Renamed $count files successfully.")
            clearSelection(_uiState.value.activePaneIndex)
            refreshBothPanes()
        }
    }

    // Archive Dialogs & Operations (ZArchiver Engine)
    fun compressSelected(options: CompressionOptions, outputFileName: String) {
        viewModelScope.launch {
            val pane = getActivePane()
            val selected = pane.items.filter { pane.selectedPaths.contains(it.path) }.map { it.file }
            if (selected.isEmpty()) {
                showMessage("No files selected for compression.")
                return@launch
            }

            val finalName = if (outputFileName.contains('.')) outputFileName else "$outputFileName.${options.targetFormat.lowercase()}"
            val outputFile = File(pane.currentPath, finalName)

            _uiState.update {
                it.copy(isOperationInProgress = true, operationProgressText = "Compressing to ${options.targetFormat}...")
            }

            val success = ArchiveEngine.createZipArchive(
                files = selected,
                outputZip = outputFile,
                options = options
            )

            _uiState.update { it.copy(isOperationInProgress = false, showCompressDialog = false) }
            if (success) {
                showMessage("Archive '$finalName' created successfully!")
                clearSelection(_uiState.value.activePaneIndex)
                refreshBothPanes()
            } else {
                showMessage("Failed to create archive.")
            }
        }
    }

    fun extractArchive(archiveFile: File, targetDir: File) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isOperationInProgress = true, operationProgressText = "Extracting archive...")
            }

            val success = ArchiveEngine.extractAll(archiveFile, targetDir)
            _uiState.update {
                it.copy(isOperationInProgress = false, showExtractDialog = false, targetArchiveForExtract = null)
            }
            if (success) {
                showMessage("Extracted to ${targetDir.name} successfully!")
                refreshBothPanes()
            } else {
                showMessage("Extraction failed.")
            }
        }
    }

    fun testArchive(archiveFile: File) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isOperationInProgress = true, operationProgressText = "Testing archive CRC & headers...")
            }
            val result = ArchiveEngine.testArchiveIntegrity(archiveFile)
            _uiState.update {
                it.copy(
                    isOperationInProgress = false,
                    showTestArchiveDialog = true,
                    testArchiveResult = result
                )
            }
        }
    }

    // APK Tools & Reverse Engineering (MT Manager Engine)
    fun analyzeApk(apkFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, operationProgressText = "Parsing APK structure...") }
            val apkInfo = ApkEngine.analyzeApk(getApplication(), apkFile)
            _uiState.update {
                it.copy(
                    isOperationInProgress = false,
                    showApkAnalyzerDialog = true,
                    activeApkInfo = apkInfo
                )
            }
        }
    }

    fun signApk(sourceApk: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, operationProgressText = "Signing APK with MTZ-Key...") }
            val outputApk = File(sourceApk.parentFile, "${sourceApk.nameWithoutExtension}_signed.apk")
            val success = ApkEngine.signApk(sourceApk, outputApk)
            _uiState.update { it.copy(isOperationInProgress = false) }
            if (success) {
                showMessage("APK signed successfully: ${outputApk.name}")
                refreshBothPanes()
            } else {
                showMessage("APK signing failed.")
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInstalledApps = true, showInstalledAppsSheet = true) }
            val apps = ApkEngine.getInstalledApps(getApplication())
            _uiState.update { it.copy(installedApps = apps, isLoadingInstalledApps = false) }
        }
    }

    fun extractInstalledApp(app: InstalledAppItem) {
        viewModelScope.launch {
            val destDir = getActivePane().currentPath
            _uiState.update { it.copy(isOperationInProgress = true, operationProgressText = "Extracting APK for ${app.appName}...") }
            val extracted = ApkEngine.extractInstalledApk(getApplication(), app, destDir)
            _uiState.update { it.copy(isOperationInProgress = false, showInstalledAppsSheet = false) }
            if (extracted != null) {
                showMessage("Extracted ${extracted.name} to current directory!")
                refreshBothPanes()
            } else {
                showMessage("Failed to extract APK.")
            }
        }
    }

    // Hash Tool
    fun calculateHash(fileItem: FileItem) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showHashCalculatorDialog = true,
                    targetFileForHash = fileItem,
                    isCalculatingHash = true,
                    hashResult = null
                )
            }
            val res = FileEngine.calculateHashes(fileItem.file)
            _uiState.update { it.copy(isCalculatingHash = false, hashResult = res) }
        }
    }

    // Search
    fun performSearch(query: String, isRegex: Boolean, searchContent: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchQuery = query) }
            val currentDir = getActivePane().currentPath
            val results = FileEngine.searchFiles(currentDir, query, isRegex, searchContent)
            _uiState.update { it.copy(isSearching = false, searchResults = results) }
        }
    }

    // Text & Code Editor
    fun openInTextEditor(fileItem: FileItem) {
        viewModelScope.launch {
            val content = if (fileItem.isVirtualArchiveEntry) {
                val archiveFile = getActivePane().archiveFile ?: return@launch
                ArchiveEngine.readVirtualArchiveEntryText(archiveFile, fileItem.virtualEntryPath)
            } else {
                try {
                    fileItem.file.readText()
                } catch (e: Exception) {
                    "Error reading file: ${e.message}"
                }
            }

            _uiState.update {
                it.copy(
                    activeScreen = ActiveScreen.TEXT_EDITOR,
                    editorFile = if (!fileItem.isVirtualArchiveEntry) fileItem.file else null,
                    editorVirtualArchive = if (fileItem.isVirtualArchiveEntry) Pair(getActivePane().archiveFile!!, fileItem.virtualEntryPath) else null,
                    editorTitle = fileItem.name,
                    editorContent = content,
                    editorIsModified = false
                )
            }
        }
    }

    fun updateEditorContent(newContent: String) {
        _uiState.update { it.copy(editorContent = newContent, editorIsModified = true) }
    }

    fun saveEditorContent() {
        viewModelScope.launch {
            val st = _uiState.value
            var saved = false
            if (st.editorFile != null) {
                try {
                    st.editorFile.writeText(st.editorContent)
                    saved = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (st.editorVirtualArchive != null) {
                val (archiveFile, entryPath) = st.editorVirtualArchive
                val temp = File(getApplication<Application>().cacheDir, File(entryPath).name)
                temp.writeText(st.editorContent)
                saved = ArchiveEngine.addOrUpdateFileInZip(archiveFile, temp, entryPath)
            }

            if (saved) {
                _uiState.update { it.copy(editorIsModified = false) }
                showMessage("Saved '${st.editorTitle}' successfully.")
                refreshBothPanes()
            } else {
                showMessage("Failed to save file.")
            }
        }
    }

    // Hex Viewer
    fun openInHexViewer(fileItem: FileItem) {
        viewModelScope.launch {
            val bytes = if (fileItem.isVirtualArchiveEntry) {
                val archiveFile = getActivePane().archiveFile ?: return@launch
                ArchiveEngine.readVirtualArchiveEntryBytes(archiveFile, fileItem.virtualEntryPath)
            } else {
                try {
                    fileItem.file.readBytes()
                } catch (e: Exception) {
                    ByteArray(0)
                }
            }

            _uiState.update {
                it.copy(
                    activeScreen = ActiveScreen.HEX_VIEWER,
                    hexFile = if (!fileItem.isVirtualArchiveEntry) fileItem.file else null,
                    hexVirtualArchive = if (fileItem.isVirtualArchiveEntry) Pair(getActivePane().archiveFile!!, fileItem.virtualEntryPath) else null,
                    hexTitle = fileItem.name,
                    hexBytes = bytes
                )
            }
        }
    }

    // File Diff / Compare Left & Right
    fun comparePanesFiles() {
        viewModelScope.launch {
            val leftSelected = _uiState.value.paneLeft.items.filter { _uiState.value.paneLeft.selectedPaths.contains(it.path) }
            val rightSelected = _uiState.value.paneRight.items.filter { _uiState.value.paneRight.selectedPaths.contains(it.path) }

            val fileA = leftSelected.firstOrNull()?.file ?: _uiState.value.paneLeft.items.firstOrNull { !it.isDirectory }?.file
            val fileB = rightSelected.firstOrNull()?.file ?: _uiState.value.paneRight.items.firstOrNull { !it.isDirectory }?.file

            if (fileA == null || fileB == null) {
                showMessage("Please select or navigate to a file in both panes to compare.")
                return@launch
            }

            val textA = try { fileA.readText() } catch (e: Exception) { "" }
            val textB = try { fileB.readText() } catch (e: Exception) { "" }

            val diff = DiffEngine.computeDiff(textA, textB)

            _uiState.update {
                it.copy(
                    activeScreen = ActiveScreen.DIFF_VIEWER,
                    diffFileA = fileA,
                    diffFileB = fileB,
                    diffLines = diff
                )
            }
        }
    }

    fun closeActiveTool() {
        _uiState.update { it.copy(activeScreen = ActiveScreen.EXPLORER) }
    }

    // Dialog state helpers
    fun showNewFolderDialog(show: Boolean) = _uiState.update { it.copy(showNewFolderDialog = show) }
    fun showNewFileDialog(show: Boolean) = _uiState.update { it.copy(showNewFileDialog = show) }
    fun showRenameDialog(fileItem: FileItem?) = _uiState.update { it.copy(showRenameDialog = fileItem != null, targetFileForRename = fileItem) }
    fun showDeleteConfirmDialog(show: Boolean) = _uiState.update { it.copy(showDeleteConfirmDialog = show) }
    fun showCompressDialog(show: Boolean) = _uiState.update { it.copy(showCompressDialog = show) }
    fun showExtractDialog(archiveFile: File?) = _uiState.update { it.copy(showExtractDialog = archiveFile != null, targetArchiveForExtract = archiveFile) }
    fun showTestArchiveDialog(show: Boolean) = _uiState.update { it.copy(showTestArchiveDialog = show) }
    fun showApkAnalyzerDialog(show: Boolean) = _uiState.update { it.copy(showApkAnalyzerDialog = show) }
    fun showInstalledAppsSheet(show: Boolean) = _uiState.update { it.copy(showInstalledAppsSheet = show) }
    fun showHashCalculatorDialog(show: Boolean) = _uiState.update { it.copy(showHashCalculatorDialog = show) }
    fun showBatchRenameDialog(show: Boolean) = _uiState.update { it.copy(showBatchRenameDialog = show) }
    fun showStorageAnalyzerSheet(show: Boolean) = _uiState.update { it.copy(showStorageAnalyzerSheet = show) }
    fun showFilePropertiesDialog(fileItem: FileItem?) = _uiState.update { it.copy(showFilePropertiesDialog = fileItem != null, targetFileForProperties = fileItem) }
    fun showSearchDialog(show: Boolean) = _uiState.update { it.copy(showSearchDialog = show, searchResults = emptyList()) }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(statusMessage = msg) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun getPane(index: Int): PaneState = if (index == 0) _uiState.value.paneLeft else _uiState.value.paneRight
    private fun getActivePane(): PaneState = getPane(_uiState.value.activePaneIndex)

    private fun updatePane(index: Int, transform: (PaneState) -> PaneState) {
        _uiState.update { current ->
            if (index == 0) {
                current.copy(paneLeft = transform(current.paneLeft))
            } else {
                current.copy(paneRight = transform(current.paneRight))
            }
        }
    }
}
