package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AssistantLanguage
import com.example.data.model.PermissionSnapshot
import com.example.data.model.RoseUserPreferences
import com.example.data.model.VoiceOption
import com.example.ui.RoseViewModel
import com.example.ui.components.CosmicSpaceBackground
import com.example.ui.components.PlaceholderAiNoticeBanner
import com.example.ui.components.RoseGlassCard
import com.example.ui.theme.LocalRoseAccents
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    viewModel: RoseViewModel,
    prefs: RoseUserPreferences,
    permissions: PermissionSnapshot,
    voices: List<VoiceOption>,
    isKeystoreBacked: Boolean,
    onFinished: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 9
    val accents = LocalRoseAccents.current
    val context = LocalContext.current

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    val contactsPhoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshPermissions() }

    CosmicSpaceBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (step in 1..7) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SETUP $step / 7",
                            style = MaterialTheme.typography.labelMedium,
                            color = accents.secondaryGlow
                        )
                        TextButton(onClick = {
                            viewModel.completeOnboarding()
                            onFinished()
                        }) {
                            Text("Skip Setup", color = TextSecondary)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { step / 7f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = accents.primaryGlow,
                        trackColor = accents.primaryGlow.copy(alpha = 0.18f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(targetState = step, label = "onboardingStep") { current ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when (current) {
                                0 -> MeetRoseIntroStep()
                                1 -> LanguageChoiceStep(
                                    selected = prefs.preferredLanguage,
                                    onSelect = { viewModel.setPreferredLanguage(it) }
                                )
                                2 -> VoiceChoiceStep(
                                    voices = voices,
                                    selectedVoiceName = prefs.selectedVoiceName,
                                    onSelectVoice = { viewModel.setSelectedVoice(it) },
                                    onTestVoice = { viewModel.testFemaleVoice() }
                                )
                                3 -> PermissionStepCard(
                                    icon = Icons.Rounded.Mic,
                                    title = "Microphone Permission",
                                    subtitle = "Required for voice commands, live transcription, and \"${prefs.wakePhrase}\" wake listening.",
                                    granted = permissions.microphoneGranted,
                                    buttonLabel = if (permissions.microphoneGranted) "Microphone Granted" else "Allow Microphone",
                                    onAction = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                                )
                                4 -> PermissionStepCard(
                                    icon = Icons.Rounded.NotificationsActive,
                                    title = "Optional Notifications",
                                    subtitle = "Enables gentle reminders and optional study break prompts when Proactive Mode is active.",
                                    granted = permissions.notificationsGranted,
                                    buttonLabel = if (permissions.notificationsGranted) "Notifications Granted" else "Allow Notifications",
                                    onAction = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                )
                                5 -> PermissionStepCard(
                                    icon = Icons.Rounded.AccessibilityNew,
                                    title = "Optional Screen Automation",
                                    subtitle = "When explicitly enabled by you in Android Settings, Rose can scroll, tap elements, go back, and type text on command.",
                                    granted = permissions.accessibilityEnabled,
                                    buttonLabel = if (permissions.accessibilityEnabled) "Accessibility Enabled" else "Open Accessibility Settings",
                                    onAction = {
                                        context.startActivity(viewModel.permissionManager.openAccessibilitySettingsIntent())
                                    }
                                )
                                6 -> PermissionStepCard(
                                    icon = Icons.Rounded.Contacts,
                                    title = "Optional Contacts & Phone",
                                    subtitle = "Allows Rose to look up names like \"Call Mom\" or prepare messages for contacts. Sensitive actions always ask confirmation.",
                                    granted = permissions.contactsGranted && permissions.phoneGranted,
                                    buttonLabel = if (permissions.contactsGranted) "Contacts & Phone Ready" else "Grant Contacts & Phone",
                                    onAction = {
                                        contactsPhoneLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CONTACTS,
                                                Manifest.permission.CALL_PHONE
                                            )
                                        )
                                    }
                                )
                                7 -> AiAndSecurityStep(isKeystoreBacked = isKeystoreBacked)
                                8 -> ReadyStep(wakePhrase = prefs.wakePhrase)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (step > 0 && step < totalSteps - 1) {
                        OutlinedButton(
                            onClick = { step -= 1 },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.refreshPermissions()
                            if (step < totalSteps - 1) {
                                step += 1
                            } else {
                                viewModel.completeOnboarding()
                                onFinished()
                            }
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(52.dp)
                            .testTag("onboarding_next_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = accents.primaryGlow)
                    ) {
                        Text(
                            text = when (step) {
                                0 -> "Begin Setup"
                                totalSteps - 1 -> "Launch ROSE AI"
                                else -> "Continue"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetRoseIntroStep() {
    val accents = LocalRoseAccents.current
    Image(
        painter = painterResource(id = R.drawable.img_rose_onboarding),
        contentDescription = "ROSE AI holographic core artwork",
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(24.dp)),
        contentScale = ContentScale.Crop
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "ROSE AI",
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp),
        color = accents.secondaryGlow
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Meet Rose.",
        style = MaterialTheme.typography.displayLarge,
        color = TextPrimary,
        textAlign = TextAlign.Center
    )
    Text(
        text = "Your personal AI assistant.",
        style = MaterialTheme.typography.headlineMedium,
        color = accents.primaryGlow,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Your Voice. Your Intelligence. Your Control.",
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun LanguageChoiceStep(
    selected: AssistantLanguage,
    onSelect: (AssistantLanguage) -> Unit
) {
    val accents = LocalRoseAccents.current
    Icon(
        imageVector = Icons.Rounded.Language,
        contentDescription = null,
        tint = accents.secondaryGlow,
        modifier = Modifier.size(44.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text("Choose Language", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        "Rose understands English, Bengali, Hindi, and mixed speech naturally.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))
    AssistantLanguage.entries.forEach { lang ->
        val isSelected = lang == selected
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clickable { onSelect(lang) },
            shape = RoundedCornerShape(16.dp),
            color = if (isSelected) accents.primaryGlow.copy(alpha = 0.18f) else Color(0xFF10162B),
            border = BorderStroke(
                1.dp,
                if (isSelected) accents.primaryGlow else Color(0xFF263052)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(lang.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(lang.nativeLabel, style = MaterialTheme.typography.labelMedium, color = accents.secondaryGlow)
                }
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) accents.primaryGlow else TextMuted
                )
            }
        }
    }
}

@Composable
private fun VoiceChoiceStep(
    voices: List<VoiceOption>,
    selectedVoiceName: String,
    onSelectVoice: (String) -> Unit,
    onTestVoice: () -> Unit
) {
    val accents = LocalRoseAccents.current
    Icon(
        imageVector = Icons.Rounded.GraphicEq,
        contentDescription = null,
        tint = accents.primaryGlow,
        modifier = Modifier.size(44.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text("Choose Female Voice", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        "Select a natural voice profile for Rose's spoken responses.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(14.dp))
    OutlinedButton(onClick = onTestVoice) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Preview Rose's Voice")
    }
    Spacer(modifier = Modifier.height(14.dp))
    if (voices.isEmpty()) {
        RoseGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Using default system female voice profile. Additional voices can be selected in Settings once Text-to-Speech finishes loading.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    } else {
        voices.take(5).forEach { voice ->
            val isSelected = selectedVoiceName == voice.name ||
                (selectedVoiceName.isBlank() && voice == voices.first())
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelectVoice(voice.name) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) accents.secondaryGlow.copy(alpha = 0.14f) else Color(0xFF10162B),
                border = BorderStroke(1.dp, if (isSelected) accents.secondaryGlow else Color(0xFF263052))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(voice.displayLabel, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        if (voice.isFemaleHint) {
                            Text("Recommended Female Profile", style = MaterialTheme.typography.labelSmall, color = accents.primaryGlow)
                        }
                    }
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) accents.secondaryGlow else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStepCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    buttonLabel: String,
    onAction: () -> Unit
) {
    val accents = LocalRoseAccents.current
    RoseGlassCard(modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (granted) StatusOnline else accents.primaryGlow,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onAction,
            enabled = !granted,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (granted) StatusOnline.copy(alpha = 0.25f) else accents.secondaryGlow,
                contentColor = if (granted) StatusOnline else Color.Black
            )
        ) {
            Text(buttonLabel, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AiAndSecurityStep(isKeystoreBacked: Boolean) {
    val accents = LocalRoseAccents.current
    RoseGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = accents.primaryGlow, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("AI Engine & Security", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(12.dp))
        PlaceholderAiNoticeBanner()
        Spacer(modifier = Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = StatusOnline, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isKeystoreBacked) {
                    "Android Keystore AES-256-GCM Vault Active"
                } else {
                    "Local Privacy Vault Configured"
                },
                style = MaterialTheme.typography.titleMedium,
                color = StatusOnline
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Sensitive actions (calls, messages, clearing memories) always require your explicit confirmation. No API keys are hardcoded.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun ReadyStep(wakePhrase: String) {
    val accents = LocalRoseAccents.current
    Image(
        painter = painterResource(id = R.drawable.img_rose_avatar),
        contentDescription = "Rose AI ready",
        modifier = Modifier
            .size(136.dp)
            .clip(CircleShape)
    )
    Spacer(modifier = Modifier.height(22.dp))
    Text(
        text = "ROSE AI is ready.",
        style = MaterialTheme.typography.displayLarge,
        color = TextPrimary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(10.dp))
    Surface(
        shape = CircleShape,
        color = accents.primaryGlow.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accents.primaryGlow.copy(alpha = 0.5f))
    ) {
        Text(
            text = "Say: \"$wakePhrase\"",
            style = MaterialTheme.typography.titleLarge,
            color = accents.secondaryGlow,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}
