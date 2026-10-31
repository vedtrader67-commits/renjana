package com.fesu.renjana.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fesu.renjana.core.CrashHandler
import com.fesu.renjana.ui.screens.CalculatorGate
import com.fesu.renjana.ui.screens.CrashScreen
import com.fesu.renjana.ui.theme.RenjanaTheme
import com.fesu.renjana.utils.PermissionManager
import com.fesu.renjana.utils.ThemePreferences

class MainActivity : ComponentActivity() {
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            Log.w(TAG, "Storage permissions denied — limited access")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissionsIfNeeded()
        setContent {
            var unlocked by rememberSaveable { mutableStateOf(false) }
            val darkMode = remember { mutableStateOf(ThemePreferences.isDarkMode(this)) }
            val accentColor = remember { mutableStateOf(ThemePreferences.getAccentColor(this)) }
            val pendingCrash = remember { mutableStateOf(CrashHandler.getPendingCrash(this)) }
            val crashContent = remember(pendingCrash.value) { pendingCrash.value?.readText() }

            RenjanaTheme(darkTheme = darkMode.value, dynamicColor = false, accentColor = accentColor.value) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!unlocked) {
                        CalculatorGate(onUnlocked = { unlocked = true })
                    } else if (crashContent != null) {
                        CrashScreen(
                            crashContent = crashContent,
                            onRestart = {
                                CrashHandler.clearCrashMarker(this)
                                pendingCrash.value = null
                                val intent = packageManager.getLaunchIntentForPackage(packageName)
                                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            },
                            onDismiss = {
                                CrashHandler.clearCrashMarker(this)
                                pendingCrash.value = null
                            }
                        )
                    } else {
                        RenjanaApp(
                            darkMode = darkMode.value,
                            onToggleDarkMode = { enabled ->
                                darkMode.value = enabled
                                ThemePreferences.setDarkMode(this, enabled)
                            },
                            accentColor = accentColor.value,
                            onAccentChange = { color ->
                                accentColor.value = color
                                ThemePreferences.setAccentColor(this, color)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (PermissionManager.hasStoragePermission(this)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            PermissionManager.openManageStorageSettings(this)
        } else {
            storagePermissionLauncher.launch(PermissionManager.getRequiredPermissions())
        }
    }

    companion object { private const val TAG = "MainActivity" }
}
