package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.automation.PermissionManager
import com.example.data.model.AccentPreset
import com.example.data.model.AnimationIntensity
import com.example.data.model.AssistantLanguage
import com.example.data.model.CoreDesignStyle
import com.example.data.model.PermissionSnapshot
import com.example.data.model.ResponseStyle
import com.example.data.model.RoseUserPreferences
import com.example.data.model.VoiceOption
import com.example.ui.components.CosmicSpaceBackground
import com.example.ui.components.PlaceholderAiNoticeBanner
import com.example.ui.components.RoseGlassCard
import com.example.ui.theme.LocalRoseAccents
import com.example.ui.theme.SpaceDeepNavy
import com.example.ui.theme.SpaceSurface
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    prefs: RoseUserPreferences,
    permissions: PermissionSnapshot,
    voices: List<VoiceOption>,
    isKeystoreBacked: Boolean,
    permissionManager: PermissionManager,
    onBack: () -> Unit,
    onLanguageChange: (AssistantLanguage) -> Unit,
    onResponseStyleChange: (ResponseStyle) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onVoiceSelected: (String) -> Unit,
    onVoiceSpeedChange: (Float) -> Unit,
    onVoicePitchChange: (Float) -> Unit,
    onTestVoice: () -> Unit,
    onWakePhraseChange: (String) -> Unit,
    onWakeEnabledChange: (Boolean) -> Unit,
    onCoreDesignChange: (CoreDesignStyle) -> Unit,
    onAnimationIntensityChange: (AnimationIntensity) -> Unit,
    onAccentPresetChange: (AccentPreset) -> Unit,
    onProactiveModeChange: (Boolean) -> Unit,
    onHistoryEnabledChange: (Boolean) -> Unit,
    onMemoryEnabledChange: (Boolean) -> Unit,
    onSaveVaultNote: suspend (String) -> Boolean,
    onReadVaultNote: suspend () -> String,
    onClearAllData: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val accents = LocalRoseAccents.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var wakePhraseDraft by remember(prefs.wakePhrase) { mutableStateOf(prefs.wakePhrase) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showVaultDialog by remember { mutableStateOf(false) }
    var vaultNoteDraft by remember { mutableStateOf("") }
    var vaultStatus by remember { mutableStateOf<String?>(null) }

    CosmicSpaceBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ROSE Settings", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. AI SETTINGS
                    RoseGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(Icons.Rounded.AutoAwesome, "AI SETTINGS")
                        Spacer(modifier = Modifier.height(8.dp))
                        PlaceholderAiNoticeBanner()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("AI Model: Placeholder Engine (Firebase AI Logic Disabled)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Response Style", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResponseStyle.entries.forEach { style ->
                                SelectChip(
                                    label = style.label,
                                    selected = prefs.responseStyle == style,
                                    onClick = { onResponseStyleChange(style) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Creativity / Temperature: ${"%.2f".format(prefs.aiTemperature)}", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        Slider(
                            value = prefs.aiTemperature,
                            onValueChange = onTemperatureChange,
                            valueRange = 0f..1f
                        )
                        Text("Language", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistantLanguage.entries.forEach { lang ->
                                SelectChip(
                                    label = lang.nativeLabel,
                                    selected = prefs.preferredLanguage == lang,
                                    onClick = { onLanguageChange(lang) }
                                )
                            }
                        }
                    }

                    // 2. VOICE
                    RoseGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(Icons.Rounded.GraphicEq, "VOICE & WAKE WORD")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hands-Free Wake Listening", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("Continuously listen for wake phrase while ROSE is active", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            Switch(
                                checked = prefs.wakeListenerEnabled,
                                onCheckedChange = onWakeEnabledChange
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = wakePhraseDraft,
                            onValueChange = { wakePhraseDraft = it },
                            label = { Text("Custom Wake Phrase") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { onWakePhraseChange(wakePhraseDraft) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Wake Phrase")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Voice Speed: ${"%.2fx".format(prefs.voiceSpeed)}", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        Slider(
                            value = prefs.voiceSpeed,
                            onValueChange = onVoiceSpeedChange,
                            valueRange = 0.7f..1.5f
                        )
                        Text("Voice Pitch (Feminine Tone): ${"%.2f".format(prefs.voicePitch)}", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        Slider(
                            value = prefs.voicePitch,
                            onValueChange = onVoicePitchChange,
                            valueRange = 0.8f..1.4f
                        )

                        if (voices.isNotEmpty()) {
                            Text("Female Voice Profile", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                voices.take(6).forEach { voice ->
                                    SelectChip(
                                        label = voice.displayLabel.take(20),
                                        selected = prefs.selectedVoiceName == voice.name,
                                        onClick = { onVoiceSelected(voice.name) }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onTestVoice) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Rose Voice")
                        }
                    }

                    // 3. APPEARANCE
                    RoseGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(Icons.Rounded.Palette, "APPEARANCE")
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Core Design", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CoreDesignStyle.entries.forEach { design ->
                                SelectChip(
                                    label = design.label,
                                    selected = prefs.coreDesignStyle == design,
                                    onClick = { onCoreDesignChange(design) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Animation Intensity", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimationIntensity.entries.forEach { intensity ->
                                SelectChip(
                                    label = intensity.label,
                                    selected = prefs.animationIntensity == intensity,
                                    onClick = { onAnimationIntensityChange(intensity) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Accent Color", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AccentPreset.entries.forEach { preset ->
                                SelectChip(
                                    label = preset.label,
                                    selected = prefs.accentPreset == preset,
                                    onClick = { onAccentPresetChange(preset) }
                                )
                            }
                        }
                    }

                    // 4. AUTOMATION
                    RoseGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(Icons.Rounded.Tune, "AUTOMATION & PROACTIVE MODE")
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusRow(
                            label = "Accessibility Service",
                            value = if (permissions.accessibilityEnabled) "Enabled" else "Off",
                            ok = permissions.accessibilityEnabled,
                            onClick = { context.startActivity(permissionManager.openAccessibilitySettingsIntent()) }
                        )
                        StatusRow(
                            label = "Media Session Access",
                            value = if (permissions.notificationListenerEnabled) "Enabled" else "Off",
                            ok = permissions.notificationListenerEnabled,
                            onClick = { context.startActivity(permissionManager.openNotificationListenerSettingsIntent()) }
                        )
                        StatusRow(
                            label = "Microphone",
                            value = if (permissions.microphoneGranted) "Granted" else "Needed",
                            ok = permissions.microphoneGranted,
                            onClick = onOpenPermissions
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Proactive Assistance", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("Optional reminder alerts & 50-min study break check-ins", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            Switch(
                                checked = prefs.proactiveModeEnabled,
                                onCheckedChange = onProactiveModeChange
                            )
                        }
                    }

                    // 5. PRIVACY & SECURITY
                    RoseGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(Icons.Rounded.Security, "PRIVACY & KEYSTORE SECURITY")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Enable Memory", color = TextPrimary)
                            Switch(checked = prefs.memoryEnabled, onCheckedChange = onMemoryEnabledChange)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Enable Command History", color = TextPrimary)
                            Switch(checked = prefs.historyEnabled, onCheckedChange = onHistoryEnabledChange)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isKeystoreBacked) {
                                "Hardware AndroidKeyStore AES-256-GCM encryption is active. Voice audio stays on-device via Android SpeechRecognizer."
                            } else {
                                "Local privacy vault active. Voice processing uses Android SpeechRecognizer."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    vaultNoteDraft = onReadVaultNote()
                                    vaultStatus = null
                                    showVaultDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (prefs.hasCustomVaultNote) "Open Keystore Vault (Encrypted)"
                                else "Open Keystore Encrypted Vault"
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showClearDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("clear_all_data_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear All Local Data")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        if (showVaultDialog) {
            AlertDialog(
                onDismissRequest = { showVaultDialog = false },
                containerColor = SpaceDeepNavy,
                title = { Text("Android Keystore Vault", color = TextPrimary) },
                text = {
                    Column {
                        Text(
                            "Stored locally using AES-256-GCM inside AndroidKeyStore. Never transmitted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = vaultNoteDraft,
                            onValueChange = { vaultNoteDraft = it },
                            label = { Text("Encrypted Private Note / Config") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (vaultStatus != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(vaultStatus!!, style = MaterialTheme.typography.labelMedium, color = StatusOnline)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            val ok = onSaveVaultNote(vaultNoteDraft)
                            vaultStatus = if (ok) "Encrypted & saved in AndroidKeyStore" else "Keystore unavailable on this device"
                        }
                    }) {
                        Text("Encrypt & Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVaultDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                containerColor = SpaceDeepNavy,
                title = { Text("Clear All ROSE Data?", color = TextPrimary) },
                text = {
                    Text(
                        "This permanently deletes your conversation history, command logs, saved memories, reminders, and Keystore vault.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearAllData()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                    ) {
                        Text("Clear Everything")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    val accents = LocalRoseAccents.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accents.secondaryGlow)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = accents.secondaryGlow)
    }
}

@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val accents = LocalRoseAccents.current
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accents.primaryGlow.copy(alpha = 0.22f) else SpaceSurface,
        border = BorderStroke(1.dp, if (selected) accents.primaryGlow else TextMuted.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) TextPrimary else TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String, ok: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = if (ok) StatusOnline else LocalRoseAccents.current.primaryGlow
        )
    }
}
