package com.example.ui.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PermissionStatus
import com.example.engine.RootShizukuEngine
import com.example.ui.theme.MTCyan
import com.example.ui.theme.ZAGold
import kotlinx.coroutines.launch

@Composable
fun RootShizukuDialog(
    status: PermissionStatus,
    onRequestRoot: () -> Unit,
    onRequestSaf: (isObb: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var terminalCmd by remember { mutableStateOf("ls -la /storage/emulated/0/Android/data") }
    var terminalOutput by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161B22),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Root & Shizuku Manager",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "High-Privilege Access Controls",
                        style = MaterialTheme.typography.labelSmall,
                        color = MTCyan
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0D1117),
                    contentColor = MTCyan
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Permissions", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Root Terminal", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // All Files Access
                        StatusCard(
                            title = "Manage All Files (MANAGE_EXTERNAL_STORAGE)",
                            subtitle = "Full read/write storage access",
                            isGranted = status.hasManageAllFiles,
                            onGrant = onRequestRoot
                        )

                        // Root Superuser (su)
                        StatusCard(
                            title = "Root Access (Superuser / Magisk / KSU)",
                            subtitle = "Bypass Android/data and Android/obb restrictions",
                            isGranted = status.hasRootAccess,
                            onGrant = onRequestRoot
                        )

                        // Shizuku ADB
                        StatusCard(
                            title = "Shizuku ADB Service",
                            subtitle = "Elevated ADB binder without root",
                            isGranted = status.hasShizukuAccess,
                            onGrant = onRequestRoot
                        )

                        // SAF Android/data
                        StatusCard(
                            title = "SAF Android/data Tree Permission",
                            subtitle = "Access app data folders via SAF DocumentFile",
                            isGranted = status.hasDataSafGranted,
                            onGrant = { onRequestSaf(false) }
                        )

                        // SAF Android/obb
                        StatusCard(
                            title = "SAF Android/obb Tree Permission",
                            subtitle = "Access game OBB packages via SAF DocumentFile",
                            isGranted = status.hasObbSafGranted,
                            onGrant = { onRequestSaf(true) }
                        )
                    }
                } else {
                    // Terminal Execution
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = terminalCmd,
                                onValueChange = { terminalCmd = it },
                                label = { Text("Shell Command") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MTCyan,
                                    unfocusedBorderColor = Color(0xFF30363D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        isExecuting = true
                                        val (_, res) = RootShizukuEngine.executeShellCommand(terminalCmd, asRoot = status.hasRootAccess)
                                        terminalOutput = res
                                        isExecuting = false
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MTCyan, RoundedCornerShape(8.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Run",
                                    tint = Color(0xFF0D1117)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val scrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0D1117))
                                .padding(8.dp)
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = if (terminalOutput.isEmpty()) "$ Ready to execute shell/root commands." else terminalOutput,
                                color = if (terminalOutput.isEmpty()) Color(0xFF8B949E) else Color(0xFF00FF88),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MTCyan)
            ) {
                Text("Close", color = Color(0xFF0D1117), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun StatusCard(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF00E676) else Color(0xFFFF9100),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8B949E),
                        fontSize = 10.sp
                    )
                }
            }

            Surface(
                color = if (isGranted) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFFFF9100).copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (isGranted) "ACTIVE" else "GRANT",
                    color = if (isGranted) Color(0xFF00E676) else Color(0xFFFF9100),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
