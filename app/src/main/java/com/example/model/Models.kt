package com.example.model

import java.io.File

enum class FileType {
    FOLDER,
    ARCHIVE,
    APK,
    CODE,
    TEXT,
    IMAGE,
    AUDIO,
    VIDEO,
    DOCUMENT,
    DEX,
    BINARY,
    UNKNOWN
}

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val isArchive: Boolean = isArchiveFile(file.name),
    val isApk: Boolean = file.name.endsWith(".apk", ignoreCase = true),
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val permissions: String = calculatePermissions(file),
    val isHidden: Boolean = file.name.startsWith("."),
    val isVirtualArchiveEntry: Boolean = false,
    val virtualEntryPath: String = "",
    val compressedSize: Long = 0L,
    val crc32: Long = 0L
) {
    val fileType: FileType
        get() {
            if (isDirectory) return FileType.FOLDER
            val ext = name.substringAfterLast('.', "").lowercase()
            return when {
                ext == "apk" -> FileType.APK
                ext in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "jar") -> FileType.ARCHIVE
                ext in listOf("dex", "arsc", "so") -> FileType.DEX
                ext in listOf("kt", "java", "smali", "xml", "json", "c", "cpp", "h", "py", "sh", "js", "ts", "html", "css") -> FileType.CODE
                ext in listOf("txt", "log", "md", "cfg", "ini", "properties", "mf") -> FileType.TEXT
                ext in listOf("png", "jpg", "jpeg", "webp", "gif", "svg", "bmp") -> FileType.IMAGE
                ext in listOf("mp3", "ogg", "wav", "flac", "m4a", "aac") -> FileType.AUDIO
                ext in listOf("mp4", "mkv", "avi", "webm", "mov") -> FileType.VIDEO
                ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx") -> FileType.DOCUMENT
                ext in listOf("bin", "dat", "exe", "dll", "o") -> FileType.BINARY
                else -> FileType.UNKNOWN
            }
        }

    companion object {
        fun isArchiveFile(fileName: String): Boolean {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return ext in listOf("zip", "apk", "jar", "tar", "gz", "7z", "rar", "bz2", "xz")
        }

        private fun calculatePermissions(file: File): String {
            val r = if (file.canRead()) "r" else "-"
            val w = if (file.canWrite()) "w" else "-"
            val x = if (file.canExecute()) "x" else "-"
            return "$r$w$x"
        }
    }
}

data class ArchiveEntryItem(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long,
    val crc: Long,
    val time: Long,
    val comment: String = ""
)

data class ApkInfo(
    val file: File,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val permissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>,
    val dexClassesCount: Int,
    val signatures: List<CertificateInfo>,
    val manifestXmlPreview: String,
    val rawSize: Long
)

data class CertificateInfo(
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validTo: String,
    val md5: String,
    val sha1: String,
    val sha256: String,
    val algorithm: String
)

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sourceDir: String,
    val apkSize: Long,
    val isSystemApp: Boolean,
    val targetSdk: Int,
    val installedTime: Long
)

data class FileDiffLine(
    val lineNumberA: Int?,
    val lineNumberB: Int?,
    val text: String,
    val type: DiffLineType
)

enum class DiffLineType {
    UNCHANGED,
    ADDED,
    DELETED,
    MODIFIED
}

data class CompressionOptions(
    val targetFormat: String = "ZIP", // ZIP, TAR, GZ, 7Z
    val compressionLevel: Int = 6, // 0..9
    val password: String = "",
    val comment: String = "",
    val splitVolumeSizeMb: Int = 0 // 0 = No split
)

enum class SortMode {
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC,
    SIZE_ASC,
    SIZE_DESC,
    TYPE_ASC
}

data class BookmarkItem(
    val title: String,
    val path: String,
    val iconType: String = "FOLDER"
)

data class HashResult(
    val md5: String,
    val sha1: String,
    val sha256: String,
    val crc32: String,
    val fileSize: Long,
    val calculationTimeMs: Long
)
