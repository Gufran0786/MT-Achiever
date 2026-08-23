package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import com.example.model.FileItem
import com.example.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

enum class AccessTier {
    STANDARD_STORAGE,
    SAF_DOCUMENT_TREE,
    SHIZUKU_ADB,
    ROOT_SUPERUSER
}

data class PermissionStatus(
    val hasManageAllFiles: Boolean = false,
    val hasRootAccess: Boolean = false,
    val hasShizukuAccess: Boolean = false,
    val hasDataSafGranted: Boolean = false,
    val hasObbSafGranted: Boolean = false,
    val activeTier: AccessTier = AccessTier.STANDARD_STORAGE
)

object RootShizukuEngine {

    private var dataTreeUri: Uri? = null
    private var obbTreeUri: Uri? = null

    fun checkRootAccess(): Boolean {
        val suPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in suPaths) {
            if (File(path).exists()) return true
        }

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.destroy()
            !line.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    fun checkShizukuAvailable(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: Exception) {
            try {
                // Check if shizuku binder / socket is open
                File("/data/local/tmp/shizuku").exists() || File("/data/local/tmp/rish").exists()
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun getPermissionStatus(context: Context): PermissionStatus {
        val hasManage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        val hasRoot = checkRootAccess()
        val hasShizuku = checkShizukuAvailable(context)
        val hasDataSaf = dataTreeUri != null
        val hasObbSaf = obbTreeUri != null

        val tier = when {
            hasRoot -> AccessTier.ROOT_SUPERUSER
            hasShizuku -> AccessTier.SHIZUKU_ADB
            hasDataSaf || hasObbSaf -> AccessTier.SAF_DOCUMENT_TREE
            else -> AccessTier.STANDARD_STORAGE
        }

        return PermissionStatus(
            hasManageAllFiles = hasManage,
            hasRootAccess = hasRoot,
            hasShizukuAccess = hasShizuku,
            hasDataSafGranted = hasDataSaf,
            hasObbSafGranted = hasObbSaf,
            activeTier = tier
        )
    }

    fun setDataTreeUri(uri: Uri) {
        dataTreeUri = uri
    }

    fun setObbTreeUri(uri: Uri) {
        obbTreeUri = uri
    }

    suspend fun executeShellCommand(cmd: String, asRoot: Boolean = false): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val shell = if (asRoot) "su" else "sh"
            val process = Runtime.getRuntime().exec(shell)
            val outputStream = DataOutputStream(process.outputStream)
            val inputStream = BufferedReader(InputStreamReader(process.inputStream))
            val errorStream = BufferedReader(InputStreamReader(process.errorStream))

            outputStream.writeBytes("$cmd\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val output = StringBuilder()
            var line: String?
            while (inputStream.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val error = StringBuilder()
            while (errorStream.readLine().also { line = it } != null) {
                error.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Pair(true, output.toString().trim())
            } else {
                Pair(false, if (error.isNotEmpty()) error.toString().trim() else output.toString().trim())
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Execution error")
        }
    }

    /**
     * Resolves Android/data and Android/obb directory contents using SAF, Root, Shizuku,
     * or by discovering installed application package containers.
     */
    suspend fun listProtectedDirectory(
        context: Context,
        dir: File
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val path = dir.absolutePath
        val isDataDir = path.endsWith("Android/data") || path.endsWith("Android/data/")
        val isObbDir = path.endsWith("Android/obb") || path.endsWith("Android/obb/")

        // 1. Try standard listing first if available
        if (dir.exists() && dir.canRead()) {
            val standardList = dir.listFiles()
            if (!standardList.isNullOrEmpty()) {
                return@withContext standardList.map { FileItem(it) }
            }
        }

        // 2. Try Root shell access if rooted
        if (checkRootAccess()) {
            val (success, output) = executeShellCommand("ls -1p \"$path\"", asRoot = true)
            if (success && output.isNotBlank()) {
                val names = output.lines().filter { it.isNotBlank() }
                return@withContext names.map { rawName ->
                    val isDir = rawName.endsWith("/")
                    val cleanName = rawName.trimEnd('/')
                    val childFile = File(dir, cleanName)
                    FileItem(
                        file = childFile,
                        name = cleanName,
                        path = childFile.absolutePath,
                        isDirectory = isDir
                    )
                }
            }
        }

        // 3. Try SAF ContentResolver Documents Contract if granted
        val activeUri = if (isDataDir) dataTreeUri else if (isObbDir) obbTreeUri else null
        if (activeUri != null) {
            try {
                val docId = DocumentsContract.getTreeDocumentId(activeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(activeUri, docId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                )

                val cursor = context.contentResolver.query(childrenUri, projection, null, null, null)
                cursor?.use { c ->
                    val nameIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    val safItems = mutableListOf<FileItem>()
                    while (c.moveToNext()) {
                        val displayName = if (nameIdx != -1) c.getString(nameIdx) ?: "unknown" else "unknown"
                        val mime = if (mimeIdx != -1) c.getString(mimeIdx) ?: "" else ""
                        val isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR
                        val size = if (sizeIdx != -1 && !isDirectory) c.getLong(sizeIdx) else 0L
                        val lastMod = if (modIdx != -1) c.getLong(modIdx) else System.currentTimeMillis()

                        val childFile = File(dir, displayName)
                        safItems.add(
                            FileItem(
                                file = childFile,
                                name = displayName,
                                path = childFile.absolutePath,
                                isDirectory = isDirectory,
                                size = size,
                                lastModified = lastMod
                            )
                        )
                    }
                    if (safItems.isNotEmpty()) {
                        return@withContext safItems.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Fallback & Pro Enhancement: Package-Aware App Container Listing
        // When Android blocks Android/data or Android/obb, list all installed packages
        // so user can seamlessly access package folders for Free Fire, BGMI, WhatsApp, etc.
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        val packageItems = mutableListOf<FileItem>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo
            val appName = try {
                if (appInfo != null) pm.getApplicationLabel(appInfo).toString() else pkg.packageName
            } catch (e: Exception) {
                pkg.packageName
            }

            val pkgDir = File(dir, pkg.packageName)
            // Ensure virtual directory representation
            val hasData = if (isObbDir) {
                // Check if obb folder exists or dummy representation
                pkgDir.exists() || pkg.packageName.contains("tencent") || pkg.packageName.contains("pubg") || pkg.packageName.contains("dts")
            } else true

            if (hasData) {
                val size = try {
                    if (appInfo != null) File(appInfo.sourceDir).length() else 0L
                } catch (e: Exception) {
                    0L
                }

                packageItems.add(
                    FileItem(
                        file = pkgDir,
                        name = pkg.packageName,
                        path = pkgDir.absolutePath,
                        isDirectory = true,
                        size = size,
                        lastModified = pkg.lastUpdateTime
                    )
                )
            }
        }

        return@withContext packageItems.sortedBy { it.name.lowercase() }
    }
}
