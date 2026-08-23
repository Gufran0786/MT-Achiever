package com.example.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.engine.FileEngine
import com.example.model.ImageViewerState
import com.example.ui.theme.MTCyan
import com.example.ui.theme.ZAGold
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    state: ImageViewerState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(state.rotationAngle) }
    var isGrayscale by remember { mutableStateOf(state.isGrayscale) }
    var isInvert by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    val file = state.file
    val grayscaleMatrix = remember {
        ColorMatrix().apply { setToSaturation(0f) }
    }
    val invertMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
    ) {
        // Image Canvas with Zoom / Pan / Rotate
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = file,
                contentDescription = file.name,
                contentScale = ContentScale.Fit,
                colorFilter = when {
                    isInvert -> ColorFilter.colorMatrix(invertMatrix)
                    isGrayscale -> ColorFilter.colorMatrix(grayscaleMatrix)
                    else -> null
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                        rotationZ = rotation
                    )
            )
        }

        // Top Glassmorphic Navigation Bar
        if (showControls) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Color(0xCC0D1117),
                tonalElevation = 8.dp
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
                                text = file.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = "${FileEngine.formatFileSize(file.length())} • ${file.extension.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MTCyan
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { showInfoSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = ZAGold
                            )
                        }
                        IconButton(onClick = {
                            val uri = Uri.fromFile(file)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Action Control Toolbar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(24.dp)),
                color = Color(0xDD161B22),
                tonalElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = MTCyan)
                            Text("Rotate", fontSize = 9.sp, color = MTCyan)
                        }
                    }

                    IconButton(onClick = { isGrayscale = !isGrayscale }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ColorLens, contentDescription = "B&W", tint = if (isGrayscale) ZAGold else Color.White)
                            Text("B&W", fontSize = 9.sp, color = if (isGrayscale) ZAGold else Color.White)
                        }
                    }

                    IconButton(onClick = { scale = (scale + 0.5f).coerceAtMost(5f) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                            Text("+", fontSize = 9.sp, color = Color.White)
                        }
                    }

                    IconButton(onClick = { scale = (scale - 0.5f).coerceAtLeast(0.5f) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                            Text("-", fontSize = 9.sp, color = Color.White)
                        }
                    }

                    IconButton(onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                        rotation = 0f
                        isGrayscale = false
                        isInvert = false
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                            Text("Reset", fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Image Metadata / EXIF Sheet
        if (showInfoSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF161B22)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Image Specifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MTCyan
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow(label = "Filename", value = file.name)
                            InfoRow(label = "Full Path", value = file.absolutePath)
                            InfoRow(label = "File Size", value = FileEngine.formatFileSize(file.length()))
                            InfoRow(label = "Extension", value = file.extension.uppercase())
                            InfoRow(label = "Last Modified", value = dateStr)
                            InfoRow(label = "Read/Write", value = if (file.canWrite()) "Read + Write" else "Read Only")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8B949E))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
