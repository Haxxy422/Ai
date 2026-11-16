package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PendingConfirmation
import com.example.data.model.QuickActionItem
import com.example.data.model.RoseCoreState
import com.example.data.model.RoseUserPreferences
import com.example.ui.components.ConfirmationPromptCard
import com.example.ui.components.CosmicSpaceBackground
import com.example.ui.components.NetworkStatusPill
import com.example.ui.components.PlaceholderAiNoticeBanner
import com.example.ui.components.ProactiveBannerCard
import com.example.ui.components.RoseCoreVisualizer
import com.example.ui.components.RoseGlassCard
import com.example.ui.theme.LocalRoseAccents
import com.example.ui.theme.SpaceDeepNavy
import com.example.ui.theme.SpaceSurface
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCoreScreen(
    coreState: RoseCoreState,
    isOnline: Boolean,
    isWakeActive: Boolean,
    rmsLevel: Float,
    partialTranscript: String,
    liveTranscription: String,
    lastResponseText: String,
    lastSystemSummary: String?,
    showPlaceholderAiBanner: Boolean,
    pendingConfirmation: PendingConfirmation?,
    proactiveBanner: String?,
    prefs: RoseUserPreferences,
    onTapMic: () -> Unit,
    onToggleWake: (Boolean) -> Unit,
    onSubmitText: (String) -> Unit,
    onConfirmPending: () -> Unit,
    onCancelPending: () -> Unit,
    onDismissProactive: () -> Unit,
    onQuickActionClick: (QuickActionItem) -> Unit,
    onToggleQuickActionPreset: (String) -> Unit,
    onNavigateChat: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateMemory: () -> Unit,
    onNavigatePermissions: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val accents = LocalRoseAccents.current
    var showInlineKeyboard by remember { mutableStateOf(false) }
    var commandDraft by remember { mutableStateOf("") }
    var showCustomizeActions by remember { mutableStateOf(false) }

    val quickActions = remember(prefs.enabledQuickActionIds) {
        QuickActionItem.ALL_PRESETS.filter { it.id in prefs.enabledQuickActionIds }
    }

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
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ROSE AI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                letterSpacing = 2.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        NetworkStatusPill(isOnline = isOnline)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = onNavigateHistory,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("nav_history_btn")
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = "Command History", tint = TextSecondary)
                        }
                        IconButton(
                            onClick = onNavigateMemory,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("nav_memory_btn")
                        ) {
                            Icon(Icons.Rounded.Psychology, contentDescription = "Memory", tint = TextSecondary)
                        }
                        IconButton(
                            onClick = onNavigatePermissions,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("nav_permissions_btn")
                        ) {
                            Icon(Icons.Rounded.Security, contentDescription = "Permission Center", tint = TextSecondary)
                        }
                        IconButton(
                            onClick = onNavigateSettings,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("nav_settings_btn")
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextSecondary)
                        }
                    }
                }

                // Scrollable middle section for Core + Live Transcription + Cards
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (proactiveBanner != null) {
                        ProactiveBannerCard(
                            message = proactiveBanner,
                            onDismiss = onDismissProactive,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    RoseCoreVisualizer(
                        coreState = coreState,
                        rmsLevel = rmsLevel,
                        designStyle = prefs.coreDesignStyle,
                        animationIntensity = prefs.animationIntensity,
                        onTapCore = onTapMic
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Wake listener toggle pill
                    Surface(
                        modifier = Modifier
                            .clickable { onToggleWake(!isWakeActive) }
                            .testTag("wake_toggle_pill"),
                        shape = CircleShape,
                        color = if (isWakeActive) accents.secondaryGlow.copy(alpha = 0.18f) else SpaceSurface.copy(alpha = 0.7f),
                        border = BorderStroke(
                            1.dp,
                            if (isWakeActive) accents.secondaryGlow else TextMuted.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = "Wake phrase mode",
                                tint = if (isWakeActive) accents.secondaryGlow else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isWakeActive) {
                                    "LISTENING FOR \"${prefs.wakePhrase.uppercase()}\""
                                } else {
                                    "WAKE MODE OFF · \"${prefs.wakePhrase}\""
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isWakeActive) accents.secondaryGlow else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Transcription & Rose Response Card
                    RoseGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("live_transcription_card")
                    ) {
                        val activeTranscript = when {
                            coreState == RoseCoreState.LISTENING && partialTranscript.isNotBlank() -> partialTranscript
                            liveTranscription.isNotBlank() -> liveTranscription
                            else -> null
                        }
                        if (activeTranscript != null) {
                            Text(
                                text = "YOU",
                                style = MaterialTheme.typography.labelSmall,
                                color = accents.secondaryGlow
                            )
                            Text(
                                text = activeTranscript,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }

                        Text(
                            text = "ROSE",
                            style = MaterialTheme.typography.labelSmall,
                            color = accents.primaryGlow
                        )
                        Text(
                            text = lastResponseText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )

                        if (!lastSystemSummary.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = lastSystemSummary,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (lastSystemSummary.startsWith("✕")) StatusError else StatusOnline
                            )
                        }
                    }

                    if (showPlaceholderAiBanner) {
                        Spacer(modifier = Modifier.height(10.dp))
                        PlaceholderAiNoticeBanner()
                    }

                    if (pendingConfirmation != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ConfirmationPromptCard(
                            confirmation = pendingConfirmation,
                            onConfirm = onConfirmPending,
                            onCancel = onCancelPending
                        )
                    }

                    AnimatedVisibility(visible = showInlineKeyboard) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commandDraft,
                                onValueChange = { commandDraft = it },
                                placeholder = { Text("Ask Rose or give a command...", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_command_input"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accents.secondaryGlow,
                                    unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (commandDraft.isNotBlank()) {
                                        onSubmitText(commandDraft)
                                        commandDraft = ""
                                    }
                                })
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (commandDraft.isNotBlank()) {
                                        onSubmitText(commandDraft)
                                        commandDraft = ""
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("quick_command_send")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Send command",
                                    tint = accents.secondaryGlow
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Actions Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickActions.forEach { item ->
                        Surface(
                            modifier = Modifier
                                .clickable { onQuickActionClick(item) }
                                .testTag("quick_action_${item.id}"),
                            shape = RoundedCornerShape(14.dp),
                            color = SpaceSurface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, accents.primaryGlow.copy(alpha = 0.28f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = iconForQuickAction(item.iconKey),
                                    contentDescription = item.label,
                                    tint = accents.secondaryGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .clickable { showCustomizeActions = true }
                            .testTag("customize_quick_actions_btn"),
                        shape = RoundedCornerShape(14.dp),
                        color = SpaceSurface.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Tune,
                                contentDescription = "Customize quick actions",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Control Deck: Keyboard Toggle · Mic FAB · Chat Screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clickable { showInlineKeyboard = !showInlineKeyboard }
                            .testTag("inline_keyboard_toggle"),
                        shape = CircleShape,
                        color = SpaceSurface,
                        border = BorderStroke(1.dp, accents.secondaryGlow.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Keyboard,
                                contentDescription = "Type command",
                                tint = if (showInlineKeyboard) accents.secondaryGlow else TextSecondary
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = onTapMic,
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("main_mic_button"),
                        shape = CircleShape,
                        containerColor = if (coreState == RoseCoreState.LISTENING) {
                            accents.secondaryGlow
                        } else {
                            accents.primaryGlow
                        },
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = if (coreState == RoseCoreState.LISTENING) {
                                Icons.Rounded.Stop
                            } else {
                                Icons.Rounded.Mic
                            },
                            contentDescription = if (coreState == RoseCoreState.LISTENING) {
                                "Stop listening"
                            } else {
                                "Speak to Rose"
                            },
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clickable(onClick = onNavigateChat)
                            .testTag("nav_chat_btn"),
                        shape = CircleShape,
                        color = SpaceSurface,
                        border = BorderStroke(1.dp, accents.primaryGlow.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Chat,
                                contentDescription = "Open Conversation",
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        if (showCustomizeActions) {
            ModalBottomSheet(
                onDismissRequest = { showCustomizeActions = false },
                containerColor = SpaceDeepNavy
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        "Customize Quick Actions",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        "Tap to toggle apps shown on the Rose Core deck.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    QuickActionItem.ALL_PRESETS.forEach { preset ->
                        val active = preset.id in prefs.enabledQuickActionIds
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onToggleQuickActionPreset(preset.id) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) accents.primaryGlow.copy(alpha = 0.16f) else SpaceSurface,
                            border = BorderStroke(1.dp, if (active) accents.primaryGlow else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        iconForQuickAction(preset.iconKey),
                                        contentDescription = null,
                                        tint = accents.secondaryGlow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(preset.label, color = TextPrimary)
                                }
                                if (active) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Enabled", tint = accents.primaryGlow)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun iconForQuickAction(key: String): ImageVector = when (key) {
    "play" -> Icons.Rounded.PlayArrow
    "music" -> Icons.Rounded.MusicNote
    "globe" -> Icons.Rounded.Public
    "camera" -> Icons.Rounded.CameraAlt
    "map" -> Icons.Rounded.Map
    "calc" -> Icons.Rounded.Calculate
    "chat" -> Icons.Rounded.Forum
    "headphones" -> Icons.Rounded.Headphones
    "photo" -> Icons.Rounded.PhotoCamera
    "send" -> Icons.AutoMirrored.Rounded.Send
    "alarm" -> Icons.Rounded.Alarm
    "calendar" -> Icons.Rounded.CalendarMonth
    "settings" -> Icons.Rounded.Settings
    else -> Icons.Rounded.Apps
}
