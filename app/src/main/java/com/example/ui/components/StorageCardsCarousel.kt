package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MTCyan
import com.example.ui.theme.ZAGold
import java.io.File

data class StorageDriveInfo(
    val title: String,
    val subtitle: String,
    val path: File,
    val icon: ImageVector,
    val accentColor: Color,
    val badge: String? = null,
    val progress: Float = 0.65f
)

@Composable
fun StorageCardsCarousel(
    defaultStorage: File,
    workspace: File,
    onSelectDrive: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val drives = listOf(
        StorageDriveInfo(
            title = "Internal Storage",
            subtitle = "/storage/emulated/0",
            path = defaultStorage,
            icon = Icons.Default.Storage,
            accentColor = MTCyan,
            badge = "Primary",
            progress = 0.58f
        ),
        StorageDriveInfo(
            title = "Android / data",
            subtitle = "App data & game saves",
            path = File(defaultStorage, "Android/data"),
            icon = Icons.Default.Android,
            accentColor = Color(0xFF00E676),
            badge = "SAF / ROOT",
            progress = 0.72f
        ),
        StorageDriveInfo(
            title = "Android / obb",
            subtitle = "Game expansion packages",
            path = File(defaultStorage, "Android/obb"),
            icon = Icons.Default.Gamepad,
            accentColor = ZAGold,
            badge = "UNLOCKED",
            progress = 0.45f
        ),
        StorageDriveInfo(
            title = "Root FileSystem",
            subtitle = "Linux root / (Superuser)",
            path = File("/"),
            icon = Icons.Default.Security,
            accentColor = Color(0xFFFF5252),
            badge = "SU / ROOT",
            progress = 0.85f
        ),
        StorageDriveInfo(
            title = "MTZ Workspace",
            subtitle = "Decompiled APKs & scripts",
            path = workspace,
            icon = Icons.Default.Code,
            accentColor = Color(0xFFB388FF),
            badge = "Dev Lab",
            progress = 0.30f
        ),
        StorageDriveInfo(
            title = "Downloads",
            subtitle = "Browser archives & APKs",
            path = File(defaultStorage, "Download"),
            icon = Icons.Default.Download,
            accentColor = Color(0xFF40C4FF),
            progress = 0.50f
        )
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(drives, key = { it.title }) { drive ->
            Card(
                modifier = Modifier
                    .width(170.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectDrive(drive.path) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = androidx.compose.foundation.BorderStroke(1.dp, drive.accentColor.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(drive.accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = drive.icon,
                                contentDescription = drive.title,
                                tint = drive.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        drive.badge?.let { b ->
                            Surface(
                                color = drive.accentColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = b,
                                    color = drive.accentColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = drive.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1
                    )

                    Text(
                        text = drive.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8B949E),
                        fontSize = 10.sp,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { drive.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = drive.accentColor,
                        trackColor = Color(0xFF21262D)
                    )
                }
            }
        }
    }
}
