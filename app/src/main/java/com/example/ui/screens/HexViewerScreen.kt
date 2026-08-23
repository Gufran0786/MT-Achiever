package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MTCyan
import com.example.ui.theme.ZAGold
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Composable
fun HexViewerScreen(
    title: String,
    bytes: ByteArray,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOffset by remember { mutableIntStateOf(0) }
    var searchHexQuery by remember { mutableStateOf("") }
    val totalBytes = bytes.size
    val rowCount = (totalBytes + 15) / 16

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Hex: $title",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Size: $totalBytes bytes (${String.format("0x%X", totalBytes)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Selected Byte Inspector Card (MT Manager Signature)
        if (selectedOffset in 0 until totalBytes) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            ) {
                val b = bytes[selectedOffset]
                val u8 = b.toInt() and 0xFF
                val i8 = b.toInt()

                val u16 = if (selectedOffset + 1 < totalBytes) {
                    ByteBuffer.wrap(bytes, selectedOffset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
                } else null

                val i32 = if (selectedOffset + 3 < totalBytes) {
                    ByteBuffer.wrap(bytes, selectedOffset, 4).order(ByteOrder.LITTLE_ENDIAN).int
                } else null

                val charVal = if (u8 in 32..126) u8.toChar().toString() else "·"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Offset: 0x${String.format("%06X", selectedOffset)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MTCyan
                    )
                    Text(
                        text = "UInt8: $u8 | Int8: $i8 | Char: '$charVal'",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (i32 != null) {
                        Text(
                            text = "Int32: $i32",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = ZAGold
                        )
                    }
                }
            }
        }

        // Hex Columns Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Offset   ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E 0F",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MTCyan,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "  ASCII",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Hex Rows List
        val hScroll = rememberScrollState()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            items(rowCount) { rowIndex ->
                val startOffset = rowIndex * 16
                val rowHex = StringBuilder()
                val rowAscii = StringBuilder()

                for (col in 0 until 16) {
                    val currentOffset = startOffset + col
                    if (currentOffset < totalBytes) {
                        val b = bytes[currentOffset].toInt() and 0xFF
                        rowHex.append(String.format("%02X ", b))
                        if (col == 7) rowHex.append(" ")
                        val c = if (b in 32..126) b.toChar() else '.'
                        rowAscii.append(c)
                    } else {
                        rowHex.append("   ")
                        if (col == 7) rowHex.append(" ")
                        rowAscii.append(" ")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { selectedOffset = startOffset }
                        .background(
                            if (selectedOffset in startOffset until (startOffset + 16))
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Offset
                    Text(
                        text = String.format("%08X ", startOffset),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    // Hex bytes
                    Text(
                        text = rowHex.toString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // ASCII chars
                    Text(
                        text = " $rowAscii",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
