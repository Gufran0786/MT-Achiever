package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PdfDocumentData(
    val file: File,
    val title: String,
    val pageCount: Int,
    val renderedPages: List<Bitmap> = emptyList(),
    val extractedText: String = "",
    val error: String? = null
)

object PdfDocumentEngine {

    suspend fun loadPdfDocument(file: File, maxPagesToRender: Int = 10): PdfDocumentData = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            return@withContext PdfDocumentData(
                file = file,
                title = file.name,
                pageCount = 0,
                error = "File does not exist or is empty."
            )
        }

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val bitmaps = mutableListOf<Bitmap>()

        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            val pagesToRender = totalPages.coerceAtMost(maxPagesToRender)

            for (i in 0 until pagesToRender) {
                val page = renderer.openPage(i)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }

            return@withContext PdfDocumentData(
                file = file,
                title = file.name,
                pageCount = totalPages,
                renderedPages = bitmaps
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // If PDF rendering fails or file is a general document / text
            val text = try {
                file.readText()
            } catch (e2: Exception) {
                "Unable to render PDF (${e.localizedMessage})."
            }

            return@withContext PdfDocumentData(
                file = file,
                title = file.name,
                pageCount = 1,
                extractedText = text,
                error = e.localizedMessage
            )
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
