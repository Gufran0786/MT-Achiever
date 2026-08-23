package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FileEngine
import com.example.engine.PdfDocumentData
import com.example.engine.PdfDocumentEngine
import com.example.model.PdfViewerState
import com.example.ui.theme.MTCyan
import com.example.ui.theme.ZAGold
import java.io.File

@Composable
fun PdfDocumentViewerScreen(
    state: PdfViewerState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var docData by remember { mutableStateOf<PdfDocumentData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(state.file) {
        isLoading = true
        val res = PdfDocumentEngine.loadPdfDocument(state.file)
        docData = res
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF161B22),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MTCyan
                        )
                    }
                    Column {
                        Text(
                            text = state.file.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = if (state.file.extension.equals("pdf", ignoreCase = true)) {
                                "PDF Document • ${docData?.pageCount ?: 0} Pages"
                            } else {
                                "Document Viewer • ${FileEngine.formatFileSize(state.file.length())}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = ZAGold
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MTCyan)
            }
        } else {
            val data = docData
            if (data != null && data.renderedPages.isNotEmpty()) {
                // PDF Bitmaps Rendered List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(data.renderedPages) { index, bitmap ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Surface(
                                    color = Color(0xFFF0F6FC),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Page ${index + 1} of ${data.pageCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Text View of document
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF161B22))
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = data?.extractedText?.ifEmpty { "No preview text available for this file." } ?: "Empty document",
                        color = Color(0xFFE6EDF3),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
