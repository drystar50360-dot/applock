package com.userapp.applock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AppLockScreen(
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onOpenOverlaySettings = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var packages by remember { mutableStateOf(PrefsManager.getBlockedPackages(context).toList()) }
    var inputText by remember { mutableStateOf("") }

    fun refresh() {
        packages = PrefsManager.getBlockedPackages(context).toList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("AppLock 지연기", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "등록된 앱은 실행 시 20초 지연됩니다",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("패키지명 (예: com.game.example)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val pkg = inputText.trim()
                if (pkg.isNotEmpty()) {
                    PrefsManager.addPackage(context, pkg)
                    inputText = ""
                    refresh()
                }
            }) { Text("추가") }
        }

        Spacer(Modifier.height(12.dp))

        Button(onClick = onOpenAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
            Text("접근성 서비스 켜기")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onOpenOverlaySettings, modifier = Modifier.fillMaxWidth()) {
            Text("다른 앱 위에 표시 권한 켜기")
        }

        Spacer(Modifier.height(20.dp))
        Text("등록된 앱 목록 (눌러서 삭제)", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(packages) { pkg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = {
                        PrefsManager.removePackage(context, pkg)
                        refresh()
                    }
                ) {
                    Text(pkg, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
