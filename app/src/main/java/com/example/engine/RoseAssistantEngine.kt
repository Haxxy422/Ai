package com.example.engine

import com.example.ai.AiRepository
import com.example.ai.IntentParser
import com.example.automation.AndroidActionExecutor
import com.example.automation.PermissionManager
import com.example.data.model.ActionExecutionStatus
import com.example.data.model.ActionResult
import com.example.data.model.ActionType
import com.example.data.model.AssistantLanguage
import com.example.data.model.ParsedIntent
import com.example.data.model.PendingConfirmation
import com.example.data.model.RoseUserPreferences
import com.example.data.repository.RoseDataRepository
import java.util.UUID

sealed interface EngineOutcome {
    data class Executed(val result: ActionResult) : EngineOutcome
    data class NeedsConfirmation(val confirmation: PendingConfirmation, val spokenPrompt: String) : EngineOutcome
    data class WakeOnly(val greeting: String) : EngineOutcome
}

/**
 * Core orchestration pipeline:
 * Input -> Wake Stripping -> AI/Intent Parsing -> Confirmation Gate ->
 * Controlled ActionRouter -> Persistence -> Female Voice Response.
 */
class RoseAssistantEngine(
    private val aiRepository: AiRepository,
    private val intentParser: IntentParser,
    private val actionExecutor: AndroidActionExecutor,
    private val permissionManager: PermissionManager,
    private val repository: RoseDataRepository
) {
    suspend fun processCommand(
        rawInput: String,
        prefs: RoseUserPreferences,
        pendingConfirmation: PendingConfirmation?,
        fromWakeLoop: Boolean
    ): EngineOutcome {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return EngineOutcome.Executed(
                ActionResult(
                    success = false,
                    action = ActionType.CONVERSATION,
                    systemSummary = "Empty input",
                    spokenResponse = "I'm listening. Tell me what you'd like me to do."
                )
            )
        }

        // Handle pending confirmation by voice ("yes", "confirm", "no", "cancel")
        if (pendingConfirmation != null) {
            val lower = trimmed.lowercase()
            if (lower in setOf("yes", "yeah", "sure", "confirm", "ok", "okay", "do it", "হ্যাঁ", "हाँ")) {
                return confirmAndExecute(pendingConfirmation, prefs)
            }
            if (lower in setOf("no", "cancel", "stop", "don't", "না", "नहीं")) {
                return cancelConfirmation(pendingConfirmation, prefs)
            }
        }

        // Check wake phrase if coming from hands-free wake loop
        val (hasWake, remainder) = intentParser.stripWakePhrase(trimmed, prefs.wakePhrase)
        if (fromWakeLoop) {
            if (!hasWake) {
                return EngineOutcome.WakeOnly("")
            }
            if (remainder.isBlank()) {
                return EngineOutcome.WakeOnly("Yes, I'm listening.")
            }
        }

        val commandText = remainder.ifBlank { trimmed }
        val isOnline = permissionManager.isNetworkOnline()

        // Record User message in chat
        repository.addChatMessage(
            sender = "USER",
            content = commandText
        )

        val parsed = aiRepository.parseOrClassifyIntent(
            rawInput = commandText,
            wakePhrase = prefs.wakePhrase,
            preferredLanguage = prefs.preferredLanguage
        )

        // Enforce confirmation gate for sensitive actions (calling, messaging, clearing all memories)
        val confirmation = buildConfirmationIfSensitive(parsed, commandText)
        if (confirmation != null) {
            repository.addChatMessage(
                sender = "ROSE",
                content = confirmation.promptMessage,
                systemStatusText = "Confirmation required",
                status = ActionExecutionStatus.REQUIRES_CONFIRMATION,
                actionType = parsed.action
            )
            return EngineOutcome.NeedsConfirmation(confirmation, confirmation.promptMessage)
        }

        return executeParsedIntent(parsed, commandText, prefs, isOnline)
    }

    suspend fun confirmAndExecute(
        confirmation: PendingConfirmation,
        prefs: RoseUserPreferences
    ): EngineOutcome {
        val isOnline = permissionManager.isNetworkOnline()
        val parsed = ParsedIntent(
            action = confirmation.action,
            parameters = confirmation.parameters,
            detectedLanguage = confirmation.language,
            confidence = 1.0f,
            normalizedCommand = confirmation.title
        )
        val result = actionExecutor.execute(parsed, isOnline)
        persistOutcome(confirmation.title, result, prefs)
        return EngineOutcome.Executed(result)
    }

    suspend fun cancelConfirmation(
        confirmation: PendingConfirmation,
        prefs: RoseUserPreferences
    ): EngineOutcome {
        val result = ActionResult(
            success = true,
            action = confirmation.action,
            systemSummary = "Cancelled by user",
            spokenResponse = "Okay, cancelled."
        )
        repository.addChatMessage(
            sender = "ROSE",
            content = result.spokenResponse,
            systemStatusText = "Cancelled",
            status = ActionExecutionStatus.CANCELLED,
            actionType = confirmation.action
        )
        if (prefs.historyEnabled) {
            repository.recordHistory(
                command = confirmation.title,
                response = result.spokenResponse,
                actionName = confirmation.action.label,
                status = ActionExecutionStatus.CANCELLED
            )
        }
        return EngineOutcome.Executed(result)
    }

    private suspend fun executeParsedIntent(
        parsed: ParsedIntent,
        rawCommand: String,
        prefs: RoseUserPreferences,
        isOnline: Boolean
    ): EngineOutcome.Executed {
        val result = if (parsed.action == ActionType.CONVERSATION) {
            val memories = if (prefs.memoryEnabled) {
                repository.getAllMemoriesOnce().map { it.content }
            } else emptyList()
            val aiResp = aiRepository.generateConversationalResponse(
                userPrompt = parsed.parameters["prompt"] ?: rawCommand,
                language = parsed.detectedLanguage,
                responseStyle = prefs.responseStyle,
                memories = memories,
                isOnline = isOnline
            )
            ActionResult(
                success = true,
                action = ActionType.CONVERSATION,
                systemSummary = if (aiResp.isPlaceholder) {
                    "Placeholder AI response (Firebase AI Logic disabled)"
                } else {
                    "Offline local reply"
                },
                spokenResponse = aiResp.text,
                usedPlaceholderAi = aiResp.isPlaceholder
            )
        } else {
            if (!prefs.memoryEnabled && parsed.action == ActionType.REMEMBER_FACT) {
                ActionResult(
                    success = false,
                    action = ActionType.REMEMBER_FACT,
                    systemSummary = "✕ Memory is disabled in Privacy Settings",
                    spokenResponse = "Memory saving is currently turned off in Privacy settings."
                )
            } else {
                actionExecutor.execute(parsed, isOnline)
            }
        }

        persistOutcome(rawCommand, result, prefs)
        return EngineOutcome.Executed(result)
    }

    private suspend fun persistOutcome(
        command: String,
        result: ActionResult,
        prefs: RoseDataRepositoryPrefsAlias
    ) {
        val execStatus = if (result.success) ActionExecutionStatus.SUCCESS else ActionExecutionStatus.FAILED
        repository.addChatMessage(
            sender = "ROSE",
            content = result.spokenResponse,
            systemStatusText = result.systemSummary,
            status = execStatus,
            actionType = result.action,
            isPlaceholderAi = result.usedPlaceholderAi
        )
        if (prefs.historyEnabled) {
            repository.recordHistory(
                command = command,
                response = result.spokenResponse,
                actionName = result.action.label,
                status = execStatus
            )
        }
    }

    private fun buildConfirmationIfSensitive(
        parsed: ParsedIntent,
        rawCommand: String
    ): PendingConfirmation? {
        return when (parsed.action) {
            ActionType.MAKE_CALL -> {
                val target = parsed.parameters["contact"].orEmpty().ifBlank { "this contact" }
                val (resolvedName, _) = actionExecutor.resolveContactPhone(target)
                PendingConfirmation(
                    id = UUID.randomUUID().toString(),
                    action = ActionType.MAKE_CALL,
                    title = rawCommand,
                    promptMessage = "Do you want me to call $resolvedName?",
                    parameters = parsed.parameters,
                    language = parsed.detectedLanguage
                )
            }
            ActionType.SEND_MESSAGE -> {
                val recipient = parsed.parameters["recipient"].orEmpty().ifBlank { "this contact" }
                val msg = parsed.parameters["message"].orEmpty()
                PendingConfirmation(
                    id = UUID.randomUUID().toString(),
                    action = ActionType.SEND_MESSAGE,
                    title = rawCommand,
                    promptMessage = "I've prepared the message for $recipient: \"$msg\". Should I send it?",
                    parameters = parsed.parameters,
                    language = parsed.detectedLanguage
                )
            }
            ActionType.CLEAR_MEMORY -> PendingConfirmation(
                id = UUID.randomUUID().toString(),
                action = ActionType.CLEAR_MEMORY,
                title = rawCommand,
                promptMessage = "Are you sure you want me to clear all saved memories?",
                parameters = emptyMap(),
                language = parsed.detectedLanguage
            )
            else -> null
        }
    }
}

private typealias RoseDataRepositoryPrefsAlias = RoseUserPreferences
