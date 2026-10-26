package com.voicestudio.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voicestudio.app.ui.AppTab
import com.voicestudio.app.ui.MainViewModel
import com.voicestudio.app.ui.components.LanguageSelectorModal
import com.voicestudio.app.ui.components.VoiceStudioTopBar
import com.voicestudio.app.ui.screens.HistoryScreen
import com.voicestudio.app.ui.screens.SettingsScreen
import com.voicestudio.app.ui.screens.TextToSpeechScreen
import com.voicestudio.app.ui.screens.VoiceToTextScreen
import com.voicestudio.app.ui.theme.AccentCyan
import com.voicestudio.app.ui.theme.MyApplicationTheme
import com.voicestudio.app.ui.theme.PrimaryIndigo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VoiceStudioApp()
            }
        }
    }
}

@Composable
fun VoiceStudioApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val currentLanguage by viewModel.selectedLanguage.collectAsState()
    val showLanguageDialog by viewModel.showLanguageDialog.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect toast/snackbar events
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Back handling: if in non-primary tab, go back to Voice-to-Text
    if (currentTab != AppTab.VOICE_TO_TEXT) {
        BackHandler {
            viewModel.selectTab(AppTab.VOICE_TO_TEXT)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            VoiceStudioTopBar(
                currentLanguage = currentLanguage,
                onOpenLanguageSelector = { viewModel.openLanguageDialog() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.VOICE_TO_TEXT,
                    onClick = { viewModel.selectTab(AppTab.VOICE_TO_TEXT) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice to Text"
                        )
                    },
                    label = { Text("صوت لنص") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryIndigo,
                        indicatorColor = PrimaryIndigo.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_item_vtt")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.TEXT_TO_SPEECH,
                    onClick = { viewModel.selectTab(AppTab.TEXT_TO_SPEECH) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Text to Speech"
                        )
                    },
                    label = { Text("نص لصوت") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentCyan,
                        indicatorColor = AccentCyan.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_item_tts")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.HISTORY,
                    onClick = { viewModel.selectTab(AppTab.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("السجل") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryIndigo,
                        indicatorColor = PrimaryIndigo.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_item_history")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS,
                    onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("الإعدادات") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryIndigo,
                        indicatorColor = PrimaryIndigo.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.VOICE_TO_TEXT -> VoiceToTextScreen(viewModel = viewModel)
                AppTab.TEXT_TO_SPEECH -> TextToSpeechScreen(viewModel = viewModel)
                AppTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    // Language Selector Modal Dialog
    if (showLanguageDialog) {
        LanguageSelectorModal(
            currentLanguage = currentLanguage,
            onLanguageSelected = { lang ->
                viewModel.setLanguage(lang)
            },
            onDismissRequest = {
                viewModel.closeLanguageDialog()
            }
        )
    }
}
