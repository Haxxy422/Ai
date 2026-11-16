package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MainCoreScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PermissionCenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.RoseAiTheme

object RoseRoutes {
    const val ONBOARDING = "onboarding"
    const val CORE = "core"
    const val CHAT = "chat"
    const val HISTORY = "history"
    const val MEMORY = "memory"
    const val PERMISSIONS = "permissions"
    const val SETTINGS = "settings"
}

@Composable
fun RoseAppRoot(roseViewModel: RoseViewModel = viewModel()) {
    val prefs by roseViewModel.preferences.collectAsStateWithLifecycle()
    val coreState by roseViewModel.coreState.collectAsStateWithLifecycle()
    val isOnline by roseViewModel.isOnline.collectAsStateWithLifecycle()
    val isWakeActive by roseViewModel.isWakeActive.collectAsStateWithLifecycle()
    val rmsLevel by roseViewModel.rmsLevel.collectAsStateWithLifecycle()
    val partialTranscript by roseViewModel.partialTranscript.collectAsStateWithLifecycle()
    val liveTranscription by roseViewModel.liveTranscription.collectAsStateWithLifecycle()
    val lastResponseText by roseViewModel.lastResponseText.collectAsStateWithLifecycle()
    val lastSystemSummary by roseViewModel.lastSystemSummary.collectAsStateWithLifecycle()
    val showPlaceholderAiBanner by roseViewModel.showPlaceholderAiBanner.collectAsStateWithLifecycle()
    val pendingConfirmation by roseViewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val proactiveBanner by roseViewModel.proactiveBanner.collectAsStateWithLifecycle()
    val chatMessages by roseViewModel.chatMessages.collectAsStateWithLifecycle()
    val commandHistory by roseViewModel.commandHistory.collectAsStateWithLifecycle()
    val memories by roseViewModel.memories.collectAsStateWithLifecycle()
    val permissions by roseViewModel.permissions.collectAsStateWithLifecycle()
    val availableVoices by roseViewModel.availableVoices.collectAsStateWithLifecycle()
    val isKeystoreBacked by roseViewModel.isKeystoreBacked.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val startDestination = if (prefs.onboardingCompleted) RoseRoutes.CORE else RoseRoutes.ONBOARDING

    RoseAiTheme(accentPreset = prefs.accentPreset) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(RoseRoutes.ONBOARDING) {
                OnboardingScreen(
                    viewModel = roseViewModel,
                    prefs = prefs,
                    permissions = permissions,
                    voices = availableVoices,
                    isKeystoreBacked = isKeystoreBacked,
                    onFinished = {
                        navController.navigate(RoseRoutes.CORE) {
                            popUpTo(RoseRoutes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(RoseRoutes.CORE) {
                MainCoreScreen(
                    coreState = coreState,
                    isOnline = isOnline,
                    isWakeActive = isWakeActive,
                    rmsLevel = rmsLevel,
                    partialTranscript = partialTranscript,
                    liveTranscription = liveTranscription,
                    lastResponseText = lastResponseText,
                    lastSystemSummary = lastSystemSummary,
                    showPlaceholderAiBanner = showPlaceholderAiBanner,
                    pendingConfirmation = pendingConfirmation,
                    proactiveBanner = proactiveBanner,
                    prefs = prefs,
                    onTapMic = { roseViewModel.toggleMicListening() },
                    onToggleWake = { roseViewModel.toggleWakeMode(it) },
                    onSubmitText = { roseViewModel.submitTextCommand(it) },
                    onConfirmPending = { roseViewModel.confirmPendingAction() },
                    onCancelPending = { roseViewModel.cancelPendingAction() },
                    onDismissProactive = { roseViewModel.dismissProactiveBanner() },
                    onQuickActionClick = { roseViewModel.launchQuickAction(it) },
                    onToggleQuickActionPreset = { roseViewModel.toggleQuickAction(it) },
                    onNavigateChat = { navController.navigate(RoseRoutes.CHAT) },
                    onNavigateHistory = { navController.navigate(RoseRoutes.HISTORY) },
                    onNavigateMemory = { navController.navigate(RoseRoutes.MEMORY) },
                    onNavigatePermissions = { navController.navigate(RoseRoutes.PERMISSIONS) },
                    onNavigateSettings = { navController.navigate(RoseRoutes.SETTINGS) }
                )
            }

            composable(RoseRoutes.CHAT) {
                ChatScreen(
                    messages = chatMessages,
                    pendingConfirmation = pendingConfirmation,
                    onBack = { navController.popBackStack() },
                    onSend = { roseViewModel.submitTextCommand(it) },
                    onTapMic = { roseViewModel.toggleMicListening() },
                    onConfirmPending = { roseViewModel.confirmPendingAction() },
                    onCancelPending = { roseViewModel.cancelPendingAction() },
                    onClearChat = { roseViewModel.clearChat() }
                )
            }

            composable(RoseRoutes.HISTORY) {
                HistoryScreen(
                    history = commandHistory,
                    historyEnabled = prefs.historyEnabled,
                    onBack = { navController.popBackStack() },
                    onToggleHistoryEnabled = { roseViewModel.setHistoryEnabled(it) },
                    onDeleteItem = { roseViewModel.deleteHistoryItem(it) },
                    onClearAll = { roseViewModel.clearHistory() }
                )
            }

            composable(RoseRoutes.MEMORY) {
                MemoryScreen(
                    memories = memories,
                    memoryEnabled = prefs.memoryEnabled,
                    onBack = { navController.popBackStack() },
                    onToggleMemoryEnabled = { roseViewModel.setMemoryEnabled(it) },
                    onAddMemory = { roseViewModel.addMemory(it) },
                    onDeleteMemory = { roseViewModel.deleteMemory(it) },
                    onClearAll = { roseViewModel.clearMemories() }
                )
            }

            composable(RoseRoutes.PERMISSIONS) {
                PermissionCenterScreen(
                    permissions = permissions,
                    permissionManager = roseViewModel.permissionManager,
                    onRefresh = { roseViewModel.refreshPermissions() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(RoseRoutes.SETTINGS) {
                SettingsScreen(
                    prefs = prefs,
                    permissions = permissions,
                    voices = availableVoices,
                    isKeystoreBacked = isKeystoreBacked,
                    permissionManager = roseViewModel.permissionManager,
                    onBack = { navController.popBackStack() },
                    onLanguageChange = { roseViewModel.setPreferredLanguage(it) },
                    onResponseStyleChange = { roseViewModel.setResponseStyle(it) },
                    onTemperatureChange = { roseViewModel.setAiTemperature(it) },
                    onVoiceSelected = { roseViewModel.setSelectedVoice(it) },
                    onVoiceSpeedChange = { roseViewModel.setVoiceSpeed(it) },
                    onVoicePitchChange = { roseViewModel.setVoicePitch(it) },
                    onTestVoice = { roseViewModel.testFemaleVoice() },
                    onWakePhraseChange = { roseViewModel.setWakePhrase(it) },
                    onWakeEnabledChange = { roseViewModel.toggleWakeMode(it) },
                    onCoreDesignChange = { roseViewModel.setCoreDesignStyle(it) },
                    onAnimationIntensityChange = { roseViewModel.setAnimationIntensity(it) },
                    onAccentPresetChange = { roseViewModel.setAccentPreset(it) },
                    onProactiveModeChange = { roseViewModel.setProactiveMode(it) },
                    onHistoryEnabledChange = { roseViewModel.setHistoryEnabled(it) },
                    onMemoryEnabledChange = { roseViewModel.setMemoryEnabled(it) },
                    onSaveVaultNote = { roseViewModel.saveEncryptedVaultNote(it) },
                    onReadVaultNote = { roseViewModel.readEncryptedVaultNote() },
                    onClearAllData = { roseViewModel.clearAllUserData() },
                    onOpenPermissions = { navController.navigate(RoseRoutes.PERMISSIONS) }
                )
            }
        }
    }
}
