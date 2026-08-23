package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FileEngine
import com.example.model.ApkInfo
import com.example.model.InstalledAppItem
import com.example.ui.theme.FileColorApk
import com.example.ui.theme.MTCyan
import com.example.ui.theme.ZAGold

@Composable
fun ApkAnalyzerDialog(
    apkInfo: ApkInfo,
    onSignApk: () -> Unit,
    onOpenManifestEditor: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Manifest", "Activities", "Permissions", "Signatures")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = FileColorApk,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("APK Analyzer (MT Core)", style = MaterialTheme.typography.titleMedium)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                // App Header Summary
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FileColorApk.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = FileColorApk,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = apkInfo.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${apkInfo.packageName} (v${apkInfo.versionName})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    divider = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, tabName ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(tabName, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tab Contents
                when (selectedTab) {
                    0 -> ApkOverviewTab(apkInfo)
                    1 -> ApkManifestTab(apkInfo, onOpenManifestEditor)
                    2 -> ApkActivitiesTab(apkInfo)
                    3 -> ApkPermissionsTab(apkInfo)
                    4 -> ApkSignaturesTab(apkInfo)
                }
            }
        },
        confirmButton = {
            Row {
                Button(
                    onClick = onSignApk,
                    colors = ButtonDefaults.buttonColors(containerColor = MTCyan, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sign APK", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
fun ApkOverviewTab(apkInfo: ApkInfo) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ApkInfoRow("File Size", FileEngine.formatFileSize(apkInfo.rawSize))
            ApkInfoRow("Package", apkInfo.packageName)
            ApkInfoRow("Version Name", apkInfo.versionName)
            ApkInfoRow("Version Code", "${apkInfo.versionCode}")
            ApkInfoRow("Min SDK", "Android API ${apkInfo.minSdk}")
            ApkInfoRow("Target SDK", "Android API ${apkInfo.targetSdk}")
            ApkInfoRow("DEX Classes Estimate", "~${apkInfo.dexClassesCount} classes")
            ApkInfoRow("Activities Count", "${apkInfo.activities.size}")
            ApkInfoRow("Services Count", "${apkInfo.services.size}")
            ApkInfoRow("Receivers Count", "${apkInfo.receivers.size}")
            ApkInfoRow("Permissions Count", "${apkInfo.permissions.size}")
        }
    }
}

@Composable
fun ApkManifestTab(apkInfo: ApkInfo, onOpenManifestEditor: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { onOpenManifestEditor(apkInfo.manifestXmlPreview) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit XML", fontSize = 11.sp)
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp)
        ) {
            item {
                Text(
                    text = apkInfo.manifestXmlPreview,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ApkActivitiesTab(apkInfo: ApkInfo) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (apkInfo.activities.isEmpty()) {
            item { Text("No activities declared in manifest.", style = MaterialTheme.typography.bodySmall) }
        } else {
            items(apkInfo.activities) { act ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = act,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MTCyan,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ApkPermissionsTab(apkInfo: ApkInfo) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (apkInfo.permissions.isEmpty()) {
            item { Text("No permissions requested.", style = MaterialTheme.typography.bodySmall) }
        } else {
            items(apkInfo.permissions) { perm ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = perm,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ZAGold,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ApkSignaturesTab(apkInfo: ApkInfo) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (apkInfo.signatures.isEmpty()) {
            item { Text("Signed with Android TestKey / V1 standard signature", style = MaterialTheme.typography.bodySmall) }
        } else {
            items(apkInfo.signatures) { cert ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text("Subject: ${cert.subject}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Algorithm: ${cert.algorithm}", fontSize = 10.sp)
                    Text("Valid: ${cert.validFrom} ➔ ${cert.validTo}", fontSize = 10.sp)
                    Text("MD5: ${cert.md5}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MTCyan)
                    Text("SHA-1: ${cert.sha1}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ZAGold)
                    Text("SHA-256: ${cert.sha256}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = FileColorApk)
                }
            }
        }
    }
}

@Composable
fun ApkInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun InstalledAppsSheet(
    apps: List<InstalledAppItem>,
    isLoading: Boolean,
    onExtractApk: (InstalledAppItem) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterSystem by remember { mutableStateOf(false) }

    val filteredApps = remember(apps, searchQuery, filterSystem) {
        apps.filter {
            (if (!filterSystem) !it.isSystemApp else true) &&
                    (it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Android, contentDescription = null, tint = FileColorApk, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Installed Apps Manager (${filteredApps.size})")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed packages...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !filterSystem,
                        onClick = { filterSystem = false },
                        label = { Text("User Apps") }
                    )
                    FilterChip(
                        selected = filterSystem,
                        onClick = { filterSystem = true },
                        label = { Text("All Apps (inc. System)") }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FileColorApk)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${app.packageName}  v${app.versionName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Size: ${FileEngine.formatFileSize(app.apkSize)}  SDK: ${app.targetSdk}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MTCyan
                                        )
                                    }

                                    Button(
                                        onClick = { onExtractApk(app) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Extract", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
