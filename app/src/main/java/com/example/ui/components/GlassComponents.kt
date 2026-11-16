package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.PendingConfirmation
import com.example.ui.theme.LocalRoseAccents
import com.example.ui.theme.SpaceDeepNavy
import com.example.ui.theme.SpaceObsidian
import com.example.ui.theme.SpaceSurface
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CosmicSpaceBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val accents = LocalRoseAccents.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SpaceObsidian,
                        SpaceDeepNavy,
                        accents.subtleBg.copy(alpha = 0.55f),
                        SpaceObsidian
                    )
                )
            ),
        content = content
    )
}

@Composable
fun RoseGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val accents = LocalRoseAccents.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = SpaceSurface.copy(alpha = 0.72f),
        border = BorderStroke(
            width = 1.dp,
            color = borderColor ?: accents.primaryGlow.copy(alpha = 0.24f)
        ),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun NetworkStatusPill(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val dotColor = if (isOnline) StatusOnline else StatusOffline
    val label = if (isOnline) "ONLINE" else "OFFLINE MODE"
    Surface(
        modifier = modifier.testTag("network_status_pill"),
        shape = CircleShape,
        color = SpaceSurface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = dotColor
            )
        }
    }
}

@Composable
fun PlaceholderAiNoticeBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("placeholder_ai_notice"),
        shape = RoundedCornerShape(14.dp),
        color = StatusWarning.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "AI status notice",
                tint = StatusWarning,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cloud Gemini AI features require Firebase AI Logic. Placeholder responses active; voice & Android controls run live.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.spCompat()
                ),
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun Int.spCompat() = with(androidx.compose.ui.unit.TextUnitType) {
    androidx.compose.ui.unit.TextUnit(this@spCompat.toFloat(), Sp)
}

@Composable
fun ConfirmationPromptCard(
    confirmation: PendingConfirmation,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accents = LocalRoseAccents.current
    RoseGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("confirmation_card"),
        borderColor = accents.secondaryGlow.copy(alpha = 0.6f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = "Confirmation required",
                tint = accents.secondaryGlow,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Confirmation Required",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }
        Text(
            text = confirmation.promptMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .testTag("cancel_action_button"),
                border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel")
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .testTag("confirm_action_button"),
                colors = ButtonDefaults.buttonColors(containerColor = accents.primaryGlow)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm")
            }
        }
    }
}

@Composable
fun ProactiveBannerCard(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accents = LocalRoseAccents.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = accents.tertiaryGlow.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accents.tertiaryGlow.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "Proactive insight",
                tint = accents.secondaryGlow,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onDismiss,
                border = null
            ) {
                Text("Dismiss", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
