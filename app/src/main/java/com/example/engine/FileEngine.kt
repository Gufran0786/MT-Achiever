package com.example.engine

import android.content.Context
import android.os.Environment
import com.example.model.FileItem
import com.example.model.HashResult
import com.example.model.SortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import kotlin.math.ln
import kotlin.math.pow

object FileEngine {

    fun getDefaultStoragePath(): File {
        val ext = Environment.getExternalStorageDirectory()
        return if (ext != null && ext.exists() && ext.canRead()) {
            ext
        } else {
            File("/storage/emulated/0")
        }
    }

    suspend fun getDirectoryContents(
        dir: File,
        showHidden: Boolean = false,
        sortMode: SortMode = SortMode.NAME_ASC
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
        val files = dir.listFiles() ?: return@withContext emptyList()

        val filtered = files.filter {
            if (showHidden) true else !it.name.startsWith(".")
        }

        val items = filtered.map { FileItem(it) }

        return@withContext items.sortedWith { a, b ->
            if (a.isDirectory != b.isDirectory) {
                if (a.isDirectory) -1 else 1
            } else {
                when (sortMode) {
                    SortMode.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                    SortMode.NAME_DESC -> b.name.compareTo(a.name, ignoreCase = true)
                    SortMode.DATE_ASC -> a.lastModified.compareTo(b.lastModified)
                    SortMode.DATE_DESC -> b.lastModified.compareTo(a.lastModified)
                    SortMode.SIZE_ASC -> a.size.compareTo(b.size)
                    SortMode.SIZE_DESC -> b.size.compareTo(a.size)
                    SortMode.TYPE_ASC -> a.fileType.name.compareTo(b.fileType.name)
                }
            }
        }
    }

    suspend fun copyFileOrDirectory(src: File, destDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!destDir.exists()) destDir.mkdirs()
            val target = File(destDir, src.name)
            if (src.isDirectory) {
                src.copyRecursively(target, overwrite = true)
            } else {
                src.copyTo(target, overwrite = true)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun moveFileOrDirectory(src: File, destDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!destDir.exists()) destDir.mkdirs()
            val target = File(destDir, src.name)
            if (src.renameTo(target)) {
                true
            } else {
                val copied = if (src.isDirectory) {
                    src.copyRecursively(target, overwrite = true)
                } else {
                    src.copyTo(target, overwrite = true)
                    true
                }
                if (copied) {
                    src.deleteRecursively()
                    true
                } else false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteFiles(files: List<File>): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (f in files) {
            try {
                if (f.isDirectory) {
                    if (f.deleteRecursively()) count++
                } else {
                    if (f.delete()) count++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        count
    }

    suspend fun createNewFolder(parentDir: File, folderName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val newDir = File(parentDir, folderName)
            newDir.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createNewFile(parentDir: File, fileName: String, content: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val newFile = File(parentDir, fileName)
            newFile.parentFile?.mkdirs()
            newFile.writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun renameFile(target: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val newFile = File(target.parentFile, newName)
            target.renameTo(newFile)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun batchRename(
        files: List<File>,
        findPattern: String,
        replaceWith: String,
        prefix: String,
        suffix: String,
        startNumber: Int,
        digits: Int,
        useRegex: Boolean
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        var currentNum = startNumber
        for (f in files) {
            try {
                val originalName = f.nameWithoutExtension
                val ext = if (f.extension.isNotEmpty()) ".${f.extension}" else ""
                
                var transformed = if (findPattern.isNotEmpty()) {
                    if (useRegex) {
                        originalName.replace(Regex(findPattern), replaceWith)
                    } else {
                        originalName.replace(findPattern, replaceWith)
                    }
                } else {
                    originalName
                }

                val numStr = if (digits > 0) {
                    String.format("%0${digits}d", currentNum++)
                } else ""

                val newFullName = "$prefix$transformed$numStr$suffix$ext"
                val dest = File(f.parentFile, newFullName)
                if (dest.absolutePath != f.absolutePath) {
                    if (f.renameTo(dest)) count++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        count
    }

    suspend fun searchFiles(
        rootDir: File,
        query: String,
        isRegex: Boolean = false,
        searchContent: Boolean = false,
        maxResults: Int = 200
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()
        if (!rootDir.exists() || query.isBlank()) return@withContext results

        fun checkMatch(name: String): Boolean {
            return if (isRegex) {
                try {
                    Regex(query, RegexOption.IGNORE_CASE).containsMatchIn(name)
                } catch (e: Exception) {
                    name.contains(query, ignoreCase = true)
                }
            } else {
                name.contains(query, ignoreCase = true)
            }
        }

        fun checkContentMatch(file: File): Boolean {
            if (!searchContent || file.isDirectory || file.length() > 5 * 1024 * 1024) return false
            return try {
                val text = file.readText()
                if (isRegex) Regex(query, RegexOption.IGNORE_CASE).containsMatchIn(text)
                else text.contains(query, ignoreCase = true)
            } catch (e: Exception) {
                false
            }
        }

        fun traverse(dir: File) {
            if (results.size >= maxResults) return
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (results.size >= maxResults) break
                if (checkMatch(f.name) || checkContentMatch(f)) {
                    results.add(FileItem(f))
                }
                if (f.isDirectory && !f.name.startsWith(".")) {
                    traverse(f)
                }
            }
        }

        traverse(rootDir)
        return@withContext results
    }

    suspend fun calculateHashes(file: File): HashResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val md5Digest = MessageDigest.getInstance("MD5")
        val sha1Digest = MessageDigest.getInstance("SHA-1")
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val crc32 = CRC32()

        val buffer = ByteArray(8192)
        var totalBytes = 0L

        FileInputStream(file).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md5Digest.update(buffer, 0, bytesRead)
                sha1Digest.update(buffer, 0, bytesRead)
                sha256Digest.update(buffer, 0, bytesRead)
                crc32.update(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }
        }

        fun bytesToHex(bytes: ByteArray): String =
            bytes.joinToString("") { "%02x".format(it) }

        HashResult(
            md5 = bytesToHex(md5Digest.digest()),
            sha1 = bytesToHex(sha1Digest.digest()),
            sha256 = bytesToHex(sha256Digest.digest()),
            crc32 = "%08X".format(crc32.value),
            fileSize = totalBytes,
            calculationTimeMs = System.currentTimeMillis() - startTime
        )
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
        return String.format("%.2f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }

    suspend fun setupSampleWorkspace(context: Context): File = withContext(Dispatchers.IO) {
        val root = File(context.filesDir, "MTZ_Workspace")
        if (!root.exists()) {
            root.mkdirs()

            val srcFolder = File(root, "AndroidProject_Demo")
            srcFolder.mkdirs()
            File(srcFolder, "AndroidManifest.xml").writeText(
                """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.mtzdemo"
    android:versionCode="100"
    android:versionName="1.0.0">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <application
        android:label="MTZ Pro Sample"
        android:theme="@style/Theme.MTZ"
        android:allowBackup="true"
        android:supportsRtl="true">
        
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <service 
            android:name=".services.BackgroundSyncService"
            android:exported="false" />
    </application>
</manifest>"""
            )

            File(srcFolder, "MainActivity.smali").writeText(
                """.class public Lcom/example/mtzdemo/MainActivity;
.super Landroid/app/Activity;
.source "MainActivity.java"

# direct methods
.method public constructor <init>()V
    .registers 1

    .prologue
    .line 12
    invoke-direct {p0}, Landroid/app/Activity;-><init>()V

    return-void
.end method

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 4
    .param p1, "savedInstanceState"    # Landroid/os/Bundle;

    .prologue
    .line 16
    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    .line 17
    const v0, 0x7f0b0028
    invoke-virtual {p0, v0}, Lcom/example/mtzdemo/MainActivity;->setContentView(I)V

    return-void
.end method"""
            )

            File(srcFolder, "strings.xml").writeText(
                """<resources>
    <string name="app_name">MT Z-Manager Pro</string>
    <string name="welcome_message">Welcome to dual-pane file management!</string>
    <string name="compression_level_ultra">Ultra High LZMA Compression</string>
    <string name="dex_patch_success">Smali bytecode patched successfully.</string>
</resources>"""
            )

            File(srcFolder, "build.gradle.kts").writeText(
                """plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.mtzdemo"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.mtzdemo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}"""
            )

            val archiveFolder = File(root, "Archive_Samples")
            archiveFolder.mkdirs()
            File(archiveFolder, "Readme.txt").writeText(
                "MT Z-Manager combines the APK decompilation & editing power of MT Manager\n" +
                "with the multi-format compression/extraction engine of ZArchiver.\n\n" +
                "Features supported:\n" +
                "1. Dual-pane file browser with fast cross-pane copy/move\n" +
                "2. Virtual archive explorer (browse inside .zip, .apk without extraction)\n" +
                "3. In-depth APK Analyzer (Manifest, DEX classes, Arsc, Signatures, APK Signer)\n" +
                "4. Full-featured Text / Code / Smali / XML editor with syntax highlighting\n" +
                "5. Hex Editor & Byte Inspector\n" +
                "6. File Diff comparator\n" +
                "7. Batch Renamer & Hash Checksum tool\n" +
                "8. Multi-format compressor (ZIP, TAR, GZ, 7Z emulation) with password protection"
            )

            // Create a sample zip archive inside the sample folder
            val zipFile = File(archiveFolder, "SampleBundle.zip")
            ArchiveEngine.createZipArchive(
                files = listOf(File(srcFolder, "AndroidManifest.xml"), File(srcFolder, "strings.xml")),
                outputZip = zipFile,
                comment = "MT Z-Manager Demo Archive"
            )
        }
        root
    }
}
