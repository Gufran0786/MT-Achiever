package com.example.engine

import com.example.model.ArchiveEntryItem
import com.example.model.CompressionOptions
import com.example.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ArchiveEngine {

    suspend fun listArchiveEntries(archiveFile: File): List<ArchiveEntryItem> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<ArchiveEntryItem>()
        if (!archiveFile.exists()) return@withContext entries

        try {
            ZipFile(archiveFile).use { zip ->
                val enumEntries = zip.entries()
                while (enumEntries.hasMoreElements()) {
                    val entry = enumEntries.nextElement()
                    val normalizedName = entry.name.removePrefix("/").removeSuffix("/")
                    val shortName = normalizedName.substringAfterLast('/')
                    entries.add(
                        ArchiveEntryItem(
                            name = if (shortName.isEmpty()) normalizedName else shortName,
                            fullPath = entry.name,
                            isDirectory = entry.isDirectory,
                            size = entry.size,
                            compressedSize = entry.compressedSize,
                            crc = entry.crc,
                            time = entry.time,
                            comment = entry.comment ?: ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        entries
    }

    suspend fun getVirtualDirectoryItems(
        archiveFile: File,
        currentInternalPath: String // e.g. "" or "res/" or "META-INF/"
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()
        val cleanPath = currentInternalPath.trim('/').let { if (it.isEmpty()) "" else "$it/" }
        val seenDirectories = mutableSetOf<String>()

        try {
            ZipFile(archiveFile).use { zip ->
                val enumEntries = zip.entries()
                while (enumEntries.hasMoreElements()) {
                    val entry = enumEntries.nextElement()
                    val name = entry.name.removePrefix("/")

                    if (cleanPath.isEmpty()) {
                        // Root of archive
                        val firstSlash = name.indexOf('/')
                        if (firstSlash != -1) {
                            val dirName = name.substring(0, firstSlash)
                            if (seenDirectories.add(dirName)) {
                                items.add(
                                    FileItem(
                                        file = File(archiveFile, dirName),
                                        name = dirName,
                                        path = "$dirName/",
                                        isDirectory = true,
                                        isVirtualArchiveEntry = true,
                                        virtualEntryPath = "$dirName/"
                                    )
                                )
                            }
                        } else {
                            if (!entry.isDirectory) {
                                items.add(
                                    FileItem(
                                        file = File(archiveFile, name),
                                        name = name,
                                        path = name,
                                        isDirectory = false,
                                        size = entry.size,
                                        lastModified = entry.time,
                                        isVirtualArchiveEntry = true,
                                        virtualEntryPath = entry.name,
                                        compressedSize = entry.compressedSize,
                                        crc32 = entry.crc
                                    )
                                )
                            }
                        }
                    } else {
                        // Subdirectory inside archive
                        if (name.startsWith(cleanPath) && name != cleanPath) {
                            val subName = name.removePrefix(cleanPath)
                            val nextSlash = subName.indexOf('/')
                            if (nextSlash != -1) {
                                val subDirName = subName.substring(0, nextSlash)
                                val fullSubPath = "$cleanPath$subDirName/"
                                if (seenDirectories.add(subDirName)) {
                                    items.add(
                                        FileItem(
                                            file = File(archiveFile, subDirName),
                                            name = subDirName,
                                            path = fullSubPath,
                                            isDirectory = true,
                                            isVirtualArchiveEntry = true,
                                            virtualEntryPath = fullSubPath
                                        )
                                    )
                                }
                            } else {
                                if (!entry.isDirectory) {
                                    items.add(
                                        FileItem(
                                            file = File(archiveFile, subName),
                                            name = subName,
                                            path = name,
                                            isDirectory = false,
                                            size = entry.size,
                                            lastModified = entry.time,
                                            isVirtualArchiveEntry = true,
                                            virtualEntryPath = entry.name,
                                            compressedSize = entry.compressedSize,
                                            crc32 = entry.crc
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        items.sortedWith { a, b ->
            if (a.isDirectory != b.isDirectory) {
                if (a.isDirectory) -1 else 1
            } else {
                a.name.compareTo(b.name, ignoreCase = true)
            }
        }
    }

    suspend fun readVirtualArchiveEntryText(archiveFile: File, entryPath: String): String = withContext(Dispatchers.IO) {
        try {
            ZipFile(archiveFile).use { zip ->
                val entry = zip.getEntry(entryPath) ?: return@withContext "Error: Entry not found in archive"
                zip.getInputStream(entry).bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            "Error reading archive entry: ${e.message}"
        }
    }

    suspend fun readVirtualArchiveEntryBytes(archiveFile: File, entryPath: String): ByteArray = withContext(Dispatchers.IO) {
        try {
            ZipFile(archiveFile).use { zip ->
                val entry = zip.getEntry(entryPath) ?: return@withContext ByteArray(0)
                zip.getInputStream(entry).use { it.readBytes() }
            }
        } catch (e: Exception) {
            ByteArray(0)
        }
    }

    suspend fun extractAll(
        archiveFile: File,
        targetDir: File,
        onProgress: ((String, Int, Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            ZipFile(archiveFile).use { zip ->
                val totalEntries = zip.size()
                var current = 0
                val enumEntries = zip.entries()
                while (enumEntries.hasMoreElements()) {
                    val entry = enumEntries.nextElement()
                    current++
                    onProgress?.invoke(entry.name, current, totalEntries)

                    val outFile = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(outFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun extractSelectedEntries(
        archiveFile: File,
        targetDir: File,
        entryPaths: List<String>
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            ZipFile(archiveFile).use { zip ->
                for (entryPath in entryPaths) {
                    val entry = zip.getEntry(entryPath) ?: continue
                    val outFile = File(targetDir, File(entryPath).name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(outFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        count
    }

    data class IntegrityTestResult(
        val totalEntries: Int,
        val passedEntries: Int,
        val corruptedEntries: List<String>,
        val isSuccessful: Boolean,
        val message: String
    )

    suspend fun testArchiveIntegrity(archiveFile: File): IntegrityTestResult = withContext(Dispatchers.IO) {
        var total = 0
        var passed = 0
        val corrupted = mutableListOf<String>()

        try {
            ZipFile(archiveFile).use { zip ->
                total = zip.size()
                val enumEntries = zip.entries()
                val buffer = ByteArray(8192)

                while (enumEntries.hasMoreElements()) {
                    val entry = enumEntries.nextElement()
                    if (!entry.isDirectory) {
                        val crc = CRC32()
                        try {
                            zip.getInputStream(entry).use { input ->
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    crc.update(buffer, 0, read)
                                }
                            }
                            if (entry.crc != -1L && entry.crc != crc.value) {
                                corrupted.add("${entry.name} (CRC mismatch)")
                            } else {
                                passed++
                            }
                        } catch (e: Exception) {
                            corrupted.add("${entry.name} (${e.message})")
                        }
                    } else {
                        passed++
                    }
                }
            }
        } catch (e: Exception) {
            return@withContext IntegrityTestResult(
                totalEntries = total,
                passedEntries = passed,
                corruptedEntries = listOf("Archive Header Corrupt: ${e.message}"),
                isSuccessful = false,
                message = "Archive header is invalid or corrupt"
            )
        }

        IntegrityTestResult(
            totalEntries = total,
            passedEntries = passed,
            corruptedEntries = corrupted,
            isSuccessful = corrupted.isEmpty(),
            message = if (corrupted.isEmpty()) "All $total entries verified successfully! Archive is 100% healthy."
            else "Found ${corrupted.size} corrupted entries out of $total total entries."
        )
    }

    suspend fun createZipArchive(
        files: List<File>,
        outputZip: File,
        options: CompressionOptions = CompressionOptions(),
        comment: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            outputZip.parentFile?.mkdirs()
            val zos = ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip)))
            zos.use { zipOut ->
                // Map compression level 0..9
                val deflaterLevel = when (options.compressionLevel) {
                    0 -> Deflater.NO_COMPRESSION
                    1, 2, 3 -> Deflater.BEST_SPEED
                    4, 5, 6 -> Deflater.DEFAULT_COMPRESSION
                    else -> Deflater.BEST_COMPRESSION
                }
                zipOut.setLevel(deflaterLevel)
                if (options.comment.isNotEmpty() || comment.isNotEmpty()) {
                    zipOut.setComment(if (options.comment.isNotEmpty()) options.comment else comment)
                }

                for (file in files) {
                    if (file.isDirectory) {
                        zipFolder(file, file.name, zipOut)
                    } else {
                        zipSingleFile(file, file.name, zipOut)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun zipFolder(folder: File, parentPath: String, zos: ZipOutputStream) {
        val files = folder.listFiles() ?: return
        if (files.isEmpty()) {
            val entry = ZipEntry("$parentPath/")
            zos.putNextEntry(entry)
            zos.closeEntry()
            return
        }
        for (f in files) {
            val currentPath = "$parentPath/${f.name}"
            if (f.isDirectory) {
                zipFolder(f, currentPath, zos)
            } else {
                zipSingleFile(f, currentPath, zos)
            }
        }
    }

    private fun zipSingleFile(file: File, entryPath: String, zos: ZipOutputStream) {
        val entry = ZipEntry(entryPath)
        entry.time = file.lastModified()
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        zos.closeEntry()
    }

    suspend fun deleteEntryFromZip(archiveFile: File, entryPathToDelete: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(archiveFile.parentFile, "${archiveFile.name}.tmp")
            ZipFile(archiveFile).use { srcZip ->
                ZipOutputStream(FileOutputStream(tempFile)).use { destZip ->
                    val enumEntries = srcZip.entries()
                    while (enumEntries.hasMoreElements()) {
                        val entry = enumEntries.nextElement()
                        if (entry.name == entryPathToDelete || entry.name.startsWith("$entryPathToDelete/")) {
                            continue // Skip deleted entry
                        }
                        val newEntry = ZipEntry(entry.name)
                        newEntry.time = entry.time
                        destZip.putNextEntry(newEntry)
                        srcZip.getInputStream(entry).use { it.copyTo(destZip) }
                        destZip.closeEntry()
                    }
                }
            }
            if (archiveFile.delete()) {
                tempFile.renameTo(archiveFile)
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun addOrUpdateFileInZip(archiveFile: File, fileToAdd: File, entryPathInZip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(archiveFile.parentFile, "${archiveFile.name}.tmp")
            ZipFile(archiveFile).use { srcZip ->
                ZipOutputStream(FileOutputStream(tempFile)).use { destZip ->
                    val enumEntries = srcZip.entries()
                    while (enumEntries.hasMoreElements()) {
                        val entry = enumEntries.nextElement()
                        if (entry.name == entryPathInZip) {
                            continue // Replace with new file below
                        }
                        val newEntry = ZipEntry(entry.name)
                        newEntry.time = entry.time
                        destZip.putNextEntry(newEntry)
                        srcZip.getInputStream(entry).use { it.copyTo(destZip) }
                        destZip.closeEntry()
                    }

                    // Add new/updated file
                    val newEntry = ZipEntry(entryPathInZip)
                    newEntry.time = fileToAdd.lastModified()
                    destZip.putNextEntry(newEntry)
                    FileInputStream(fileToAdd).use { it.copyTo(destZip) }
                    destZip.closeEntry()
                }
            }
            if (archiveFile.delete()) {
                tempFile.renameTo(archiveFile)
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
