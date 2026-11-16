package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.automation.PermissionManager
import com.example.data.model.PermissionSnapshot
import com.example.ui.components.CosmicSpaceBackground
import com.example.ui.components.RoseGlassCard
import com.example.ui.theme.LocalRoseAccents
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PermissionCenterScreen(
    permissions: PermissionSnapshot,
    permissionManager: PermissionManager,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onRefresh() }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onRefresh() }

    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onRefresh() }

    val phoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onRefresh() }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Permission Center", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }
                    IconButton(onClick = onRefresh, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh status", tint = TextSecondary)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PermissionItemCard(
                        icon = Icons.Rounded.Mic,
                        title = "Microphone",
                        statusText = if (permissions.microphoneGranted) "✓ Granted" else "○ Not granted",
                        granted = permissions.microphoneGranted,
                        explanation = "Required for speech-to-text voice commands and optional \"Hey Rose\" wake word listening.",
                        actionLabel = if (permissions.microphoneGranted) "Granted" else "Grant Microphone",
                        testTag = "perm_mic_btn",
                        onAction = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    )

                    PermissionItemCard(
                        icon = Icons.Rounded.Notifications,
                        title = "Notifications",
                        statusText = if (permissions.notificationsGranted) "✓ Granted" else "○ Not granted",
                        granted = permissions.notificationsGranted,
                        explanation = "Used only when Proactive Mode is enabled to show gentle reminders and focus break alerts.",
                        actionLabel = if (permissions.notificationsGranted) "Granted" else "Allow Notifications",
                        testTag = "perm_notif_btn",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )

                    PermissionItemCard(
                        icon = Icons.Rounded.AccessibilityNew,
                        title = "Accessibility Service",
                        statusText = if (permissions.accessibilityEnabled) "✓ Enabled" else "○ Not enabled",
                        granted = permissions.accessibilityEnabled,
                        explanation = "Optional screen automation. Allows Rose to scroll, go back, go home, tap buttons, or type text when you explicitly ask.",
                        actionLabel = if (permissions.accessibilityEnabled) "Manage in Settings" else "Enable Accessibility",
                        alwaysClickable = true,
                        testTag = "perm_accessibility_btn",
                        onAction = {
                            context.startActivity(permissionManager.openAccessibilitySettingsIntent())
                        }
                    )

                    PermissionItemCard(
                        icon = Icons.Rounded.MusicNote,
                        title = "Media Session Access",
                        statusText = if (permissions.notificationListenerEnabled) "✓ Enabled" else "○ Not enabled",
                        granted = permissions.notificationListenerEnabled,
                        explanation = "Optional Notification Listener bridge so Rose can directly control Spotify, YouTube Music, and active media sessions.",
                        actionLabel = if (permissions.notificationListenerEnabled) "Manage in Settings" else "Enable Media Access",
                        alwaysClickable = true,
                        testTag = "perm_media_btn",
                        onAction = {
                            context.startActivity(permissionManager.openNotificationListenerSettingsIntent())
                        }
                    )

                    PermissionItemCard(
                        icon = Icons.Rounded.Contacts,
                        title = "Contacts",
                        statusText = if (permissions.contactsGranted) "✓ Granted" else "○ Not granted",
                        granted = permissions.contactsGranted,
                        explanation = "Allows Rose to resolve contact names when you say \"Call Mom\" or \"Message Rahul\". Never uploaded or shared.",
                        actionLabel = if (permissions.contactsGranted) "Granted" else "Grant Contacts",
                        testTag = "perm_contacts_btn",
                        onAction = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) }
                    )

                    PermissionItemCard(
                        icon = Icons.Rounded.Phone,
                        title = "Phone",
                        statusText = if (permissions.phoneGranted) "✓ Granted" else "○ Not granted",
                        granted = permissions.phoneGranted,
                        explanation = "Allows placing calls after your explicit confirmation. Without it, Rose safely opens your dialer instead.",
                        actionLabel = if (permissions.phoneGranted) "Granted" else "Grant Phone",
                        testTag = "perm_phone_btn",
                        onAction = { phoneLauncher.launch(Manifest.permission.CALL_PHONE) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    statusText: String,
    granted: Boolean,
    explanation: String,
    actionLabel: String,
    testTag: String,
    alwaysClickable: Boolean = false,
    onAction: () -> Unit
) {
    val accents = LocalRoseAccents.current
    RoseGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (granted) StatusOnline else accents.primaryGlow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (granted) StatusOnline else StatusWarning,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (granted) StatusOnline else StatusWarning
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(explanation, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(10.dp))
        if (granted && !alwaysClickable) {
            OutlinedButton(
                onClick = onAction,
                enabled = false,
                modifier = Modifier.fillMaxWidth().testTag(testTag)
            ) {
                Text(actionLabel)
            }
        } else {
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(testTag),
                colors = ButtonDefaults.buttonColors(containerColor = accents.primaryGlow)
            ) {
                Text(actionLabel)
            }
        }
    }
}
