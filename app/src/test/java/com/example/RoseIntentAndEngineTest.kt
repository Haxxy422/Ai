package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.IntentParser
import com.example.ai.LocalPlaceholderAiRepository
import com.example.data.local.RoseDatabase
import com.example.data.local.RosePreferencesStore
import com.example.data.model.ActionExecutionStatus
import com.example.data.model.ActionType
import com.example.data.model.AssistantLanguage
import com.example.data.model.ResponseStyle
import com.example.data.repository.RoseDataRepository
import com.example.security.KeystoreVault
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoseIntentAndEngineTest {

    private lateinit var parser: IntentParser
    private lateinit var aiRepository: LocalPlaceholderAiRepository
    private lateinit var database: RoseDatabase
    private lateinit var repository: RoseDataRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        parser = IntentParser()
        aiRepository = LocalPlaceholderAiRepository(parser)
        database = Room.inMemoryDatabaseBuilder(context, RoseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoseDataRepository(database, RosePreferencesStore(context, KeystoreVault()))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `parses english and bengali mixed app open commands`() {
        val en = parser.parse("Hey Rose, open YouTube")
        assertEquals(ActionType.OPEN_APP, en.action)
        assertEquals("YouTube", en.parameters["appName"])

        val mixedBn = parser.parse("Rose, YouTube khol")
        assertEquals(ActionType.OPEN_APP, mixedBn.action)
        assertEquals("YouTube", mixedBn.parameters["appName"])
        assertEquals(AssistantLanguage.BENGALI, mixedBn.detectedLanguage)

        val mixedBn2 = parser.parse("Rose, Chrome open করো")
        assertEquals(ActionType.OPEN_APP, mixedBn2.action)
        assertEquals("Chrome", mixedBn2.parameters["appName"])
        assertEquals(AssistantLanguage.BENGALI, mixedBn2.detectedLanguage)
    }

    @Test
    fun `parses bengali music and youtube search commands`() {
        val bnMusic = parser.parse("Rose, গান চালাও")
        assertEquals(ActionType.PLAY_MEDIA, bnMusic.action)
        assertEquals(AssistantLanguage.BENGALI, bnMusic.detectedLanguage)

        val ytSearch = parser.parse("Hey Rose, search YouTube for Arijit Singh")
        assertEquals(ActionType.SEARCH_YOUTUBE, ytSearch.action)
        assertEquals("Arijit Singh", ytSearch.parameters["query"])
    }

    @Test
    fun `parses alarm call message and accessibility actions`() {
        val alarm = parser.parse("Hey Rose, set an alarm for 6:30 PM")
        assertEquals(ActionType.CREATE_ALARM, alarm.action)
        assertEquals("18", alarm.parameters["hour"])
        assertEquals("30", alarm.parameters["minute"])

        val call = parser.parse("Hey Rose, call Mom")
        assertEquals(ActionType.MAKE_CALL, call.action)
        assertEquals("Mom", call.parameters["contact"])

        val msg = parser.parse("Send a WhatsApp message to Rahul saying I'll be late")
        assertEquals(ActionType.SEND_MESSAGE, msg.action)
        assertEquals("Rahul", msg.parameters["recipient"])
        assertEquals("I'll be late", msg.parameters["message"])
        assertEquals("whatsapp", msg.parameters["channel"])

        val scroll = parser.parse("Hey Rose, scroll down")
        assertEquals(ActionType.SCROLL_DOWN, scroll.action)

        val back = parser.parse("Hey Rose, go back")
        assertEquals(ActionType.GO_BACK, back.action)
    }

    @Test
    fun `placeholder ai repository marks open ended responses as placeholder`() = runTest {
        val onlineReply = aiRepository.generateConversationalResponse(
            userPrompt = "Explain quantum physics",
            language = AssistantLanguage.ENGLISH,
            responseStyle = ResponseStyle.BALANCED,
            memories = emptyList(),
            isOnline = true
        )
        assertTrue(onlineReply.isPlaceholder)
        assertTrue(onlineReply.text.contains("Placeholder AI"))

        val offlineReply = aiRepository.generateConversationalResponse(
            userPrompt = "Explain quantum physics",
            language = AssistantLanguage.ENGLISH,
            responseStyle = ResponseStyle.BALANCED,
            memories = emptyList(),
            isOnline = false
        )
        assertFalse(offlineReply.isPlaceholder)
        assertTrue(offlineReply.text.contains("Offline Mode"))
    }

    @Test
    fun `room repository persists memories and command history`() = runTest {
        repository.addMemory("Prefers Bengali responses", "Language")
        repository.recordHistory(
            command = "Open YouTube",
            response = "Sure. Opening YouTube.",
            actionName = "Open App",
            status = ActionExecutionStatus.SUCCESS
        )

        val memories = repository.memoriesFlow.first()
        val history = repository.commandHistoryFlow.first()
        assertEquals(1, memories.size)
        assertEquals("Prefers Bengali responses", memories.first().content)
        assertEquals(1, history.size)
        assertEquals("Open YouTube", history.first().command)
    }
}
