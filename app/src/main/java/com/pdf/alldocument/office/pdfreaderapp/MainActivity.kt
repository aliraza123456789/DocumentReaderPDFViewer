package com.pdf.alldocument.office.pdfreaderapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.DocumentReaderTheme
import com.pdf.alldocument.office.pdfreaderapp.core.update.InAppUpdateManager
import com.pdf.alldocument.office.pdfreaderapp.presentation.navigation.AppNavigation
import com.pdf.alldocument.office.pdfreaderapp.presentation.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var inAppUpdateManager: InAppUpdateManager

    private val pendingExternalPdf = kotlinx.coroutines.flow.MutableStateFlow<com.pdf.alldocument.office.pdfreaderapp.core.util.UriUtils.ParsedFileInfo?>(null)

    @OptIn(ExperimentalSerializationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIncomingIntent(intent)

        // True edge-to-edge: both bars stay transparent so app content draws
        // behind them. Icon contrast is handled by DocumentReaderTheme via
        // WindowInsetsController (the app is light-only).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        // Check for in-app updates
        inAppUpdateManager.checkForUpdate(this)

        setContent {
            DocumentReaderTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* permissions evaluated reactively by MediaStore Scanner */ }

                // Track if permissions have been checked/requested for MainActivity main screen
                var hasRequestedPermissionOnMainScreen by rememberSaveable { mutableStateOf(false) }

                // Navigate directly to viewer if an external PDF was opened
                LaunchedEffect(Unit) {
                    pendingExternalPdf.collect { fileInfo ->
                        if (fileInfo != null) {
                            navController.navigate(
                                Screen.PdfViewer(
                                    filePath = fileInfo.filePath,
                                    title = fileInfo.displayName
                                )
                            ) {
                                // Pop Splash so back button doesn't loop back to splash
                                popUpTo(Screen.Splash) { inclusive = true }
                            }
                            pendingExternalPdf.value = null
                        }
                    }
                }

                // Request permissions ONLY when the user reaches the main screen (Screen.Home),
                // NEVER during SplashScreen, PrivacyPolicy, or Onboarding.
                LaunchedEffect(currentRoute) {
                    val isMainScreen = currentRoute == Screen.Home.serializer().descriptor.serialName ||
                            currentRoute?.contains("Home") == true

                    if (isMainScreen && !hasRequestedPermissionOnMainScreen) {
                        hasRequestedPermissionOnMainScreen = true
                        requestAppPermissions { permissions ->
                            permissionLauncher.launch(permissions)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        
        lifecycleScope.launch(Dispatchers.IO) {
            val resolved = com.pdf.alldocument.office.pdfreaderapp.core.util.UriUtils.resolveUriToFile(
                this@MainActivity,
                uri
            )
            if (resolved != null) {
                pendingExternalPdf.value = resolved
            }
        }
    }

    fun requestAppPermissions(onPermissionsRequired: ((Array<String>) -> Unit)? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+): MANAGE_EXTERNAL_STORAGE is required to read and manage all documents
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // If storage access is already granted, request notification permission on Android 13+ if needed
                val permissions = mutableListOf<String>()
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (permissions.isNotEmpty()) {
                    onPermissionsRequired?.invoke(permissions.toTypedArray())
                }
            }
        } else {
            // Android 10 and below: standard runtime storage permissions
            val permissions = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (permissions.isNotEmpty()) {
                onPermissionsRequired?.invoke(permissions.toTypedArray())
            }
        }
    }

    companion object {
        fun hasStoragePermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}