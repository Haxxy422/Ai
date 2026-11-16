package com.example.automation

import android.Manifest
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import com.example.data.model.ActionResult
import com.example.data.model.ActionType
import com.example.data.model.AssistantLanguage
import com.example.data.model.ParsedIntent
import com.example.data.repository.RoseDataRepository
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Controlled Action Router & real Android executor.
 * Validates parameters, enforces permissions & confirmations, and never fakes success.
 */
class AndroidActionExecutor(
    private val context: Context,
    private val permissionManager: PermissionManager,
    private val repository: RoseDataRepository
) {
    companion object {
        private val APP_PACKAGE_MAP = mapOf(
            "youtube" to listOf("com.google.android.youtube"),
            "chrome" to listOf("com.android.chrome", "com.chrome.beta"),
            "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "instagram" to listOf("com.instagram.android"),
            "telegram" to listOf("org.telegram.messenger", "org.thunderdog.challegram"),
            "spotify" to listOf("com.spotify.music"),
            "music" to listOf(
                "com.google.android.apps.youtube.music",
                "com.spotify.music",
                "com.sec.android.app.music",
                "com.apple.android.music"
            ),
            "youtube music" to listOf("com.google.android.apps.youtube.music"),
            "maps" to listOf("com.google.android.apps.maps"),
            "google maps" to listOf("com.google.android.apps.maps"),
            "camera" to listOf("com.google.android.GoogleCamera", "com.android.camera2", "com.sec.android.app.camera"),
            "calculator" to listOf("com.google.android.calculator", "com.android.calculator2", "com.sec.android.app.popupcalculator"),
            "clock" to listOf("com.google.android.deskclock", "com.android.deskclock", "com.sec.android.app.clockpackage"),
            "calendar" to listOf("com.google.android.calendar", "com.android.calendar"),
            "gmail" to listOf("com.google.android.gm"),
            "contacts" to listOf("com.google.android.contacts", "com.android.contacts"),
            "settings" to listOf("com.android.settings")
        )
    }

    suspend fun execute(intent: ParsedIntent, isOnline: Boolean): ActionResult {
        val lang = intent.detectedLanguage
        return when (intent.action) {
            ActionType.OPEN_APP -> executeOpenApp(intent.parameters["appName"].orEmpty(), lang)
            ActionType.SEARCH_WEB -> executeSearchWeb(intent.parameters["query"].orEmpty(), isOnline, lang)
            ActionType.SEARCH_YOUTUBE -> executeSearchYouTube(intent.parameters["query"].orEmpty(), isOnline, lang)
            ActionType.PLAY_MEDIA -> executeMediaAction(KeyEvent.KEYCODE_MEDIA_PLAY, "play", intent.parameters["query"], lang)
            ActionType.PAUSE_MEDIA -> executeMediaAction(KeyEvent.KEYCODE_MEDIA_PAUSE, "pause", null, lang)
            ActionType.NEXT_MEDIA -> executeMediaAction(KeyEvent.KEYCODE_MEDIA_NEXT, "next", null, lang)
            ActionType.PREVIOUS_MEDIA -> executeMediaAction(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "previous", null, lang)
            ActionType.STOP_MEDIA -> executeMediaAction(KeyEvent.KEYCODE_MEDIA_STOP, "stop", null, lang)
            ActionType.VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE, lang)
            ActionType.VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER, lang)
            ActionType.GO_BACK -> executeAccessibilityBack(lang)
            ActionType.GO_HOME -> executeAccessibilityHome(lang)
            ActionType.SCROLL_DOWN -> executeAccessibilityScroll(true, lang)
            ActionType.SCROLL_UP -> executeAccessibilityScroll(false, lang)
            ActionType.CLICK -> executeAccessibilityClick(intent.parameters["target"].orEmpty(), lang)
            ActionType.TYPE_TEXT -> executeAccessibilityType(intent.parameters["text"].orEmpty(), lang)
            ActionType.CREATE_ALARM -> executeCreateAlarm(intent.parameters, lang)
            ActionType.CREATE_REMINDER -> executeCreateReminder(intent.parameters, lang)
            ActionType.OPEN_SETTINGS -> launchSystemIntent(Intent(Settings.ACTION_SETTINGS), "Settings", lang)
            ActionType.OPEN_CAMERA -> launchCamera(lang)
            ActionType.OPEN_MAPS -> executeOpenMaps(lang)
            ActionType.NAVIGATE_MAPS -> executeNavigateMaps(intent.parameters["destination"].orEmpty(), isOnline, lang)
            ActionType.OPEN_DIALER -> launchSystemIntent(Intent(Intent.ACTION_DIAL), "Dialer", lang)
            ActionType.OPEN_CONTACTS -> launchSystemIntent(
                Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI),
                "Contacts",
                lang
            )
            ActionType.OPEN_CALENDAR -> launchSystemIntent(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR),
                "Calendar",
                lang
            )
            ActionType.MAKE_CALL -> executeMakeCall(intent.parameters["contact"].orEmpty(), lang)
            ActionType.SEND_MESSAGE -> executeSendMessage(
                recipient = intent.parameters["recipient"].orEmpty(),
                message = intent.parameters["message"].orEmpty(),
                channel = intent.parameters["channel"] ?: "auto",
                lang = lang
            )
            ActionType.GET_TIME -> executeGetTime(lang)
            ActionType.GET_DEVICE_INFO -> executeGetDeviceInfo(isOnline, lang)
            ActionType.REMEMBER_FACT -> executeRememberFact(intent.parameters["fact"].orEmpty(), lang)
            ActionType.RECALL_MEMORY -> executeRecallMemory(lang)
            ActionType.FORGET_MEMORY -> executeForgetMemory(intent.parameters["keyword"].orEmpty(), lang)
            ActionType.CLEAR_MEMORY -> executeClearMemory(lang)
            ActionType.CONVERSATION -> ActionResult(
                success = true,
                action = ActionType.CONVERSATION,
                systemSummary = "Conversational response",
                spokenResponse = ""
            )
        }
    }

    fun resolveContactPhone(query: String): Pair<String, String?> {
        val clean = query.trim()
        if (clean.isBlank()) return "" to null
        if (clean.any { it.isDigit() } && clean.count { it.isDigit() } >= 4) {
            return clean to clean
        }
        if (!permissionManager.hasPermission(Manifest.permission.READ_CONTACTS)) {
            return clean to null
        }
        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(
                uri,
                projection,
                "LOWER(${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}) LIKE ?",
                arrayOf("%${clean.lowercase()}%"),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0) ?: clean
                    val number = cursor.getString(1)
                    name to number
                } else {
                    clean to null
                }
            } ?: (clean to null)
        } catch (_: Exception) {
            clean to null
        }
    }

    private fun executeOpenApp(rawName: String, lang: AssistantLanguage): ActionResult {
        val cleanName = rawName.trim().lowercase()
        if (cleanName.isBlank()) {
            return failed(ActionType.OPEN_APP, "No application name specified", "Please tell me which app you want to open.")
        }

        // Special intents for core system categories
        when (cleanName) {
            "camera" -> return launchCamera(lang)
            "settings" -> return launchSystemIntent(Intent(Settings.ACTION_SETTINGS), "Settings", lang)
            "calculator" -> {
                val calcIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
                if (tryStartActivity(calcIntent)) {
                    return ActionResult(
                        success = true,
                        action = ActionType.OPEN_APP,
                        systemSummary = "✓ Calculator launched",
                        spokenResponse = localized(lang, "Opening Calculator.", "ক্যালকুলেটর খুলছি।", "कैलकुलेटर खोल रही हूँ।")
                    )
                }
            }
            "music" -> {
                val musicIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
                if (tryStartActivity(musicIntent)) {
                    return ActionResult(
                        success = true,
                        action = ActionType.OPEN_APP,
                        systemSummary = "✓ Music app launched",
                        spokenResponse = localized(lang, "Opening Music.", "মিউজিক খুলছি।", "म्यूज़िक खोल रही हूँ।")
                    )
                }
            }
        }

        val pm = context.packageManager
        val mappedPackages = APP_PACKAGE_MAP[cleanName].orEmpty()
        for (pkg in mappedPackages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null && tryStartActivity(launchIntent)) {
                val pretty = rawName.trim().replaceFirstChar { it.uppercase() }
                return ActionResult(
                    success = true,
                    action = ActionType.OPEN_APP,
                    systemSummary = "✓ $pretty launched",
                    spokenResponse = localized(
                        lang,
                        "Sure. Opening $pretty.",
                        "অবশ্যই, $pretty খুলছি।",
                        "ज़रूर, $pretty खोल रही हूँ।"
                    )
                )
            }
        }

        // Dynamic search across installed launcher apps
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(mainIntent, 0)
        val matched = resolved.firstOrNull { info ->
            val label = info.loadLabel(pm).toString().lowercase()
            label == cleanName || label.contains(cleanName)
        }
        if (matched != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matched.activityInfo.packageName)
            if (launchIntent != null && tryStartActivity(launchIntent)) {
                val label = matched.loadLabel(pm).toString()
                return ActionResult(
                    success = true,
                    action = ActionType.OPEN_APP,
                    systemSummary = "✓ $label launched",
                    spokenResponse = localized(
                        lang,
                        "Opening $label.",
                        "$label খুলছি।",
                        "$label खोल रही हूँ।"
                    )
                )
            }
        }

        val display = rawName.trim().replaceFirstChar { it.uppercase() }
        return failed(
            ActionType.OPEN_APP,
            "✕ $display is not installed",
            localized(
                lang,
                "That application isn't installed on your phone.",
                "আপনার ফোনে $display অ্যাপটি ইনস্টল করা নেই।",
                "आपके फोन में $display इंस्टॉल नहीं है।"
            )
        )
    }

    private fun launchCamera(lang: AssistantLanguage): ActionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        return if (tryStartActivity(intent)) {
            ActionResult(
                success = true,
                action = ActionType.OPEN_CAMERA,
                systemSummary = "✓ Camera launched",
                spokenResponse = localized(lang, "Opening Camera.", "ক্যামেরা খুলছি।", "कैमरा खोल रही हूँ।")
            )
        } else {
            executeOpenApp("camera", lang)
        }
    }

    private fun executeOpenMaps(lang: AssistantLanguage): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
        return if (tryStartActivity(intent)) {
            ActionResult(
                success = true,
                action = ActionType.OPEN_MAPS,
                systemSummary = "✓ Maps launched",
                spokenResponse = localized(lang, "Opening Maps.", "ম্যাপ খুলছি।", "मैप्स खोल रही हूँ।")
            )
        } else {
            executeOpenApp("maps", lang)
        }
    }

    private fun executeNavigateMaps(destination: String, isOnline: Boolean, lang: AssistantLanguage): ActionResult {
        if (destination.isBlank()) return executeOpenMaps(lang)
        if (!isOnline) {
            return failed(
                ActionType.NAVIGATE_MAPS,
                "✕ Internet connection unavailable for navigation",
                "I couldn't start navigation because the internet connection is unavailable."
            )
        }
        val encoded = URLEncoder.encode(destination.trim(), "UTF-8")
        val navIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$encoded"))
        if (tryStartActivity(navIntent)) {
            return ActionResult(
                success = true,
                action = ActionType.NAVIGATE_MAPS,
                systemSummary = "✓ Navigation started to $destination",
                spokenResponse = "Starting navigation to $destination."
            )
        }
        val mapWeb = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded"))
        return if (tryStartActivity(mapWeb)) {
            ActionResult(
                success = true,
                action = ActionType.NAVIGATE_MAPS,
                systemSummary = "✓ Opened Maps for $destination",
                spokenResponse = "Opening route to $destination."
            )
        } else {
            failed(ActionType.NAVIGATE_MAPS, "✕ No Maps application found", "No suitable Maps app was found on your device.")
        }
    }

    private fun executeSearchWeb(query: String, isOnline: Boolean, lang: AssistantLanguage): ActionResult {
        if (query.isBlank()) {
            return failed(ActionType.SEARCH_WEB, "✕ Empty search query", "What would you like me to search the web for?")
        }
        if (!isOnline) {
            return failed(
                ActionType.SEARCH_WEB,
                "✕ Internet connection unavailable",
                "I couldn't search the web because your internet connection is unavailable."
            )
        }
        val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        if (tryStartActivity(webSearchIntent)) {
            return ActionResult(
                success = true,
                action = ActionType.SEARCH_WEB,
                systemSummary = "✓ Web search opened for \"$query\"",
                spokenResponse = localized(
                    lang,
                    "Searching the web for $query.",
                    "ওয়েবে $query খুঁজছি।",
                    "वेब पर $query खोज रही हूँ।"
                )
            )
        }
        val encoded = URLEncoder.encode(query, "UTF-8")
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded"))
        return if (tryStartActivity(browserIntent)) {
            ActionResult(
                success = true,
                action = ActionType.SEARCH_WEB,
                systemSummary = "✓ Browser search opened for \"$query\"",
                spokenResponse = localized(lang, "Searching Google for $query.", "Google-এ $query খুঁজছি।", "Google पर $query खोज रही हूँ।")
            )
        } else {
            failed(ActionType.SEARCH_WEB, "✕ No browser available", "No suitable browser app was found.")
        }
    }

    private fun executeSearchYouTube(query: String, isOnline: Boolean, lang: AssistantLanguage): ActionResult {
        if (query.isBlank()) return executeOpenApp("youtube", lang)
        if (!isOnline) {
            return failed(
                ActionType.SEARCH_YOUTUBE,
                "✕ Internet connection unavailable",
                "I couldn't search YouTube because your internet connection is unavailable."
            )
        }
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val ytAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
            setPackage("com.google.android.youtube")
        }
        if (tryStartActivity(ytAppIntent)) {
            return ActionResult(
                success = true,
                action = ActionType.SEARCH_YOUTUBE,
                systemSummary = "✓ YouTube search: \"$query\"",
                spokenResponse = localized(
                    lang,
                    "Searching YouTube for $query.",
                    "YouTube-এ $query খুঁজছি।",
                    "YouTube पर $query खोज रही हूँ।"
                )
            )
        }
        val fallbackBrowser = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded"))
        return if (tryStartActivity(fallbackBrowser)) {
            ActionResult(
                success = true,
                action = ActionType.SEARCH_YOUTUBE,
                systemSummary = "✓ Opened YouTube web search for \"$query\"",
                spokenResponse = "Opening YouTube results for $query."
            )
        } else {
            failed(ActionType.SEARCH_YOUTUBE, "✕ Could not open YouTube", "No YouTube or browser app was found.")
        }
    }

    private fun executeMediaAction(
        keyCode: Int,
        verb: String,
        playQuery: String?,
        lang: AssistantLanguage
    ): ActionResult {
        // If user asked to play a specific artist/song, open music/YouTube search cleanly
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY && !playQuery.isNullOrBlank()) {
            val searchMediaIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                putExtra(SearchManager.QUERY, playQuery)
            }
            if (tryStartActivity(searchMediaIntent)) {
                return ActionResult(
                    success = true,
                    action = ActionType.PLAY_MEDIA,
                    systemSummary = "✓ Playing \"$playQuery\" via media app",
                    spokenResponse = localized(
                        lang,
                        "Playing $playQuery.",
                        "$playQuery চালাচ্ছি।",
                        "$playQuery चला रही हूँ।"
                    )
                )
            }
            return executeSearchYouTube(playQuery, permissionManager.isNetworkOnline(), lang)
        }

        // Try active MediaSession transport controls if NotificationListener is authorized
        if (permissionManager.isNotificationListenerEnabled()) {
            try {
                val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                val controllers: List<MediaController> = msm?.getActiveSessions(
                    ComponentName(context, RoseNotificationListenerService::class.java)
                ).orEmpty()
                val controller = controllers.firstOrNull()
                if (controller != null) {
                    val controls = controller.transportControls
                    when (keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY -> controls.play()
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> controls.pause()
                        KeyEvent.KEYCODE_MEDIA_NEXT -> controls.skipToNext()
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> controls.skipToPrevious()
                        KeyEvent.KEYCODE_MEDIA_STOP -> controls.stop()
                    }
                    return ActionResult(
                        success = true,
                        action = actionForMediaKey(keyCode),
                        systemSummary = "✓ MediaSession $verb sent to ${controller.packageName}",
                        spokenResponse = mediaResponse(verb, lang)
                    )
                }
            } catch (_: SecurityException) {
                // Fall back to AudioManager media key dispatch
            }
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return failed(actionForMediaKey(keyCode), "✕ Audio service unavailable", "Android Audio service is unavailable.")
        val eventTime = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))

        return ActionResult(
            success = true,
            action = actionForMediaKey(keyCode),
            systemSummary = "✓ Media $verb command dispatched",
            spokenResponse = mediaResponse(verb, lang)
        )
    }

    private fun actionForMediaKey(keyCode: Int): ActionType = when (keyCode) {
        KeyEvent.KEYCODE_MEDIA_PLAY -> ActionType.PLAY_MEDIA
        KeyEvent.KEYCODE_MEDIA_PAUSE -> ActionType.PAUSE_MEDIA
        KeyEvent.KEYCODE_MEDIA_NEXT -> ActionType.NEXT_MEDIA
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> ActionType.PREVIOUS_MEDIA
        else -> ActionType.STOP_MEDIA
    }

    private fun mediaResponse(verb: String, lang: AssistantLanguage): String = when (verb) {
        "play" -> localized(lang, "Resuming playback.", "প্লেব্যাক চালু করছি।", "प्लेबैक शुरू कर रही हूँ।")
        "pause" -> localized(lang, "Paused.", "পজ করা হয়েছে।", "पॉज़ कर दिया है।")
        "next" -> localized(lang, "Skipping to next track.", "পরের গানে যাচ্ছি।", "अगले ट्रैक पर जा रही हूँ।")
        "previous" -> localized(lang, "Playing previous track.", "আগের গান চালাচ্ছি।", "पिछला ट्रैक चला रही हूँ।")
        else -> localized(lang, "Media stopped.", "মিডিয়া বন্ধ করা হয়েছে।", "मीडिया रोक दिया है।")
    }

    private fun adjustVolume(direction: Int, lang: AssistantLanguage): ActionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return failed(ActionType.VOLUME_UP, "✕ AudioManager unavailable", "Unable to adjust volume.")
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
        val isUp = direction == AudioManager.ADJUST_RAISE
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return ActionResult(
            success = true,
            action = if (isUp) ActionType.VOLUME_UP else ActionType.VOLUME_DOWN,
            systemSummary = "✓ Media volume ${if (isUp) "increased" else "decreased"} ($current/$max)",
            spokenResponse = if (isUp) {
                localized(lang, "Volume increased.", "ভলিউম বাড়িয়েছি।", "वॉल्यूम बढ़ा दिया है।")
            } else {
                localized(lang, "Volume decreased.", "ভলিউম কমিয়েছি।", "वॉल्यूम कम कर दिया है।")
            }
        )
    }

    private fun executeAccessibilityBack(lang: AssistantLanguage): ActionResult {
        val service = RoseAccessibilityService.getInstance()
            ?: return accessibilityRequired(ActionType.GO_BACK)
        return if (service.performBack()) {
            ActionResult(
                success = true,
                action = ActionType.GO_BACK,
                systemSummary = "✓ Performed Back action",
                spokenResponse = localized(lang, "Going back.", "পিছনে যাচ্ছি।", "पीछे जा रही हूँ।")
            )
        } else {
            failed(ActionType.GO_BACK, "✕ Back action failed", "Android did not allow the Back action right now.")
        }
    }

    private fun executeAccessibilityHome(lang: AssistantLanguage): ActionResult {
        val service = RoseAccessibilityService.getInstance()
            ?: return accessibilityRequired(ActionType.GO_HOME)
        return if (service.performHome()) {
            ActionResult(
                success = true,
                action = ActionType.GO_HOME,
                systemSummary = "✓ Returned to Home screen",
                spokenResponse = localized(lang, "Going home.", "হোম স্ক্রিনে যাচ্ছি।", "होम स्क्रीन पर जा रही हूँ।")
            )
        } else {
            failed(ActionType.GO_HOME, "✕ Home action failed", "Could not navigate home.")
        }
    }

    private fun executeAccessibilityScroll(down: Boolean, lang: AssistantLanguage): ActionResult {
        val action = if (down) ActionType.SCROLL_DOWN else ActionType.SCROLL_UP
        val service = RoseAccessibilityService.getInstance() ?: return accessibilityRequired(action)
        return if (service.performScroll(down)) {
            ActionResult(
                success = true,
                action = action,
                systemSummary = "✓ Scrolled ${if (down) "down" else "up"}",
                spokenResponse = if (down) "Scrolling down." else "Scrolling up."
            )
        } else {
            failed(action, "✕ Could not scroll this screen", "No scrollable view was found on this screen.")
        }
    }

    private fun executeAccessibilityClick(target: String, lang: AssistantLanguage): ActionResult {
        if (target.isBlank()) {
            return failed(ActionType.CLICK, "✕ Missing click target", "Tell me which button or element to tap.")
        }
        val service = RoseAccessibilityService.getInstance() ?: return accessibilityRequired(ActionType.CLICK)
        return if (service.clickNodeByQuery(target)) {
            ActionResult(
                success = true,
                action = ActionType.CLICK,
                systemSummary = "✓ Tapped \"$target\"",
                spokenResponse = "Tapped $target."
            )
        } else {
            failed(ActionType.CLICK, "✕ Element \"$target\" not found", "I couldn't find \"$target\" on the screen.")
        }
    }

    private fun executeAccessibilityType(text: String, lang: AssistantLanguage): ActionResult {
        if (text.isBlank()) {
            return failed(ActionType.TYPE_TEXT, "✕ No text provided", "Tell me what text you want me to type.")
        }
        val service = RoseAccessibilityService.getInstance() ?: return accessibilityRequired(ActionType.TYPE_TEXT)
        return if (service.typeText(text)) {
            ActionResult(
                success = true,
                action = ActionType.TYPE_TEXT,
                systemSummary = "✓ Typed \"$text\"",
                spokenResponse = "Typed $text."
            )
        } else {
            failed(ActionType.TYPE_TEXT, "✕ No active input field found", "I couldn't find an editable text field on the current screen.")
        }
    }

    private fun accessibilityRequired(action: ActionType): ActionResult {
        return failed(
            action,
            "✕ Accessibility Service not enabled",
            "Permission is required. Please enable ROSE AI Screen Automation in the Permission Center first."
        )
    }

    private fun executeCreateAlarm(params: Map<String, String>, lang: AssistantLanguage): ActionResult {
        val hour = params["hour"]?.toIntOrNull()
        val minute = params["minute"]?.toIntOrNull() ?: 0
        val label = params["label"]?.ifBlank { "ROSE AI Alarm" } ?: "ROSE AI Alarm"
        if (hour == null || hour !in 0..23 || minute !in 0..59) {
            return failed(
                ActionType.CREATE_ALARM,
                "✕ Invalid alarm time",
                "Please specify a valid alarm time, like 6 AM or 7:30 PM."
            )
        }
        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        }
        return if (tryStartActivity(alarmIntent)) {
            val formatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            ActionResult(
                success = true,
                action = ActionType.CREATE_ALARM,
                systemSummary = "✓ Alarm set for $formatted",
                spokenResponse = localized(
                    lang,
                    "Alarm set for $formatted.",
                    "$formatted-এর জন্য অ্যালার্ম সেট করেছি।",
                    "$formatted का अलार्म सेट कर दिया है।"
                )
            )
        } else {
            failed(ActionType.CREATE_ALARM, "✕ No Clock app supports alarms", "No compatible Clock app was found on this device.")
        }
    }

    private suspend fun executeCreateReminder(params: Map<String, String>, lang: AssistantLanguage): ActionResult {
        val title = params["title"]?.trim()?.ifBlank { "Reminder" } ?: "Reminder"
        val triggerMillis = params["triggerMillis"]?.toLongOrNull()
            ?: (System.currentTimeMillis() + 60 * 60 * 1000L)
        repository.addReminder(title = title, triggerAtMillis = triggerMillis)
        val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(triggerMillis))

        // Also offer calendar event if possible? Store natively in Rose reminders and notify proactively!
        return ActionResult(
            success = true,
            action = ActionType.CREATE_REMINDER,
            systemSummary = "✓ Reminder saved: \"$title\" at $formattedTime",
            spokenResponse = localized(
                lang,
                "I've set a reminder for $title at $formattedTime.",
                "$formattedTime-এ \"$title\" রিমাইন্ডার সেট করেছি।",
                "$formattedTime के लिए \"$title\" रिमाइंडर सेट कर दिया है।"
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun executeMakeCall(contactQuery: String, lang: AssistantLanguage): ActionResult {
        val (displayName, number) = resolveContactPhone(contactQuery)
        if (number.isNullOrBlank()) {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            tryStartActivity(dialIntent)
            val reason = if (!permissionManager.hasPermission(Manifest.permission.READ_CONTACTS)) {
                "Contacts permission is required to look up $displayName. I've opened the dialer instead."
            } else {
                "I couldn't find a phone number for $displayName. I've opened the dialer."
            }
            return ActionResult(
                success = false,
                action = ActionType.MAKE_CALL,
                systemSummary = "✕ Number not found for $displayName (opened Dialer)",
                spokenResponse = reason,
                errorReason = reason
            )
        }

        val uri = Uri.parse("tel:${Uri.encode(number)}")
        return if (permissionManager.hasPermission(Manifest.permission.CALL_PHONE)) {
            val callIntent = Intent(Intent.ACTION_CALL, uri)
            if (tryStartActivity(callIntent)) {
                ActionResult(
                    success = true,
                    action = ActionType.MAKE_CALL,
                    systemSummary = "✓ Calling $displayName ($number)",
                    spokenResponse = localized(lang, "Calling $displayName.", "$displayName-কে কল করছি।", "$displayName को कॉल कर रही हूँ।")
                )
            } else {
                failed(ActionType.MAKE_CALL, "✕ Unable to start call", "Android did not allow placing the call.")
            }
        } else {
            val dialIntent = Intent(Intent.ACTION_DIAL, uri)
            if (tryStartActivity(dialIntent)) {
                ActionResult(
                    success = true,
                    action = ActionType.MAKE_CALL,
                    systemSummary = "✓ Opened Dialer for $displayName ($number)",
                    spokenResponse = "Phone call permission isn't granted, so I've prepared the call to $displayName in your dialer."
                )
            } else {
                failed(ActionType.MAKE_CALL, "✕ No Dialer app found", "No phone dialer app was found.")
            }
        }
    }

    private fun executeSendMessage(
        recipient: String,
        message: String,
        channel: String,
        lang: AssistantLanguage
    ): ActionResult {
        val (displayName, number) = resolveContactPhone(recipient)
        val encodedText = URLEncoder.encode(message, "UTF-8")

        // Prefer WhatsApp if explicitly requested or installed
        val useWhatsApp = channel == "whatsapp" || isPackageInstalled("com.whatsapp")
        if (useWhatsApp) {
            val waIntent = if (!number.isNullOrBlank()) {
                val digits = number.filter { it.isDigit() }
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=$encodedText")).apply {
                    setPackage("com.whatsapp")
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    setPackage("com.whatsapp")
                }
            }
            if (tryStartActivity(waIntent)) {
                return ActionResult(
                    success = true,
                    action = ActionType.SEND_MESSAGE,
                    systemSummary = "✓ Opened WhatsApp message for $displayName",
                    spokenResponse = localized(
                        lang,
                        "Opening WhatsApp to send your message to $displayName.",
                        "$displayName-কে মেসেজ পাঠাতে WhatsApp খুলছি।",
                        "$displayName को संदेश भेजने के लिए WhatsApp खोल रही हूँ।"
                    )
                )
            }
        }

        // Fallback to SMS composer (zero-permission ACTION_SENDTO)
        val smsUri = if (!number.isNullOrBlank()) Uri.parse("smsto:${Uri.encode(number)}") else Uri.parse("smsto:")
        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", message)
        }
        return if (tryStartActivity(smsIntent)) {
            ActionResult(
                success = true,
                action = ActionType.SEND_MESSAGE,
                systemSummary = "✓ Message composer opened for $displayName",
                spokenResponse = "I've opened your messaging app with the text prepared for $displayName."
            )
        } else {
            failed(ActionType.SEND_MESSAGE, "✕ No messaging app found", "No suitable messaging app was found on this device.")
        }
    }

    private fun executeGetTime(lang: AssistantLanguage): ActionResult {
        val now = Calendar.getInstance().time
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
        val dateStr = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now)
        return ActionResult(
            success = true,
            action = ActionType.GET_TIME,
            systemSummary = "✓ Local time: $timeStr · $dateStr",
            spokenResponse = localized(
                lang,
                "It's $timeStr on $dateStr.",
                "এখন সময় $timeStr, $dateStr।",
                "अभी $timeStr हो रहे हैं, $dateStr।"
            )
        )
    }

    private fun executeGetDeviceInfo(isOnline: Boolean, lang: AssistantLanguage): ActionResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val battery = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val batteryText = if (battery in 0..100) "$battery%" else "Unknown"
        val netText = if (isOnline) "Online" else "Offline Mode"
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        return ActionResult(
            success = true,
            action = ActionType.GET_DEVICE_INFO,
            systemSummary = "✓ $deviceName · Battery $batteryText · $netText",
            spokenResponse = "Your $deviceName is at $batteryText battery and currently in $netText."
        )
    }

    private suspend fun executeRememberFact(fact: String, lang: AssistantLanguage): ActionResult {
        if (fact.isBlank()) {
            return failed(ActionType.REMEMBER_FACT, "✕ Nothing to remember", "Tell me what you'd like me to remember.")
        }
        repository.addMemory(content = fact, category = "User Memory")
        return ActionResult(
            success = true,
            action = ActionType.REMEMBER_FACT,
            systemSummary = "✓ Saved to memory: \"$fact\"",
            spokenResponse = localized(
                lang,
                "Got it. I'll remember that.",
                "ঠিক আছে, আমি এটা মনে রাখব।",
                "ठीक है, मैं यह याद रखूँगी।"
            )
        )
    }

    private suspend fun executeRecallMemory(lang: AssistantLanguage): ActionResult {
        val memories = repository.getAllMemoriesOnce()
        if (memories.isEmpty()) {
            return ActionResult(
                success = true,
                action = ActionType.RECALL_MEMORY,
                systemSummary = "✓ Memory store is empty",
                spokenResponse = "I don't have any saved memories about you yet."
            )
        }
        val summary = memories.take(5).joinToString("; ") { it.content }
        return ActionResult(
            success = true,
            action = ActionType.RECALL_MEMORY,
            systemSummary = "✓ Recalled ${memories.size} memories",
            spokenResponse = "Here is what I remember: $summary."
        )
    }

    private suspend fun executeForgetMemory(keyword: String, lang: AssistantLanguage): ActionResult {
        if (keyword.isBlank()) {
            val latest = repository.getAllMemoriesOnce().firstOrNull()
            if (latest != null) {
                repository.deleteMemory(latest.id)
                return ActionResult(
                    success = true,
                    action = ActionType.FORGET_MEMORY,
                    systemSummary = "✓ Forgot \"${latest.content}\"",
                    spokenResponse = "Okay, I've forgotten that."
                )
            }
            return ActionResult(
                success = true,
                action = ActionType.FORGET_MEMORY,
                systemSummary = "✓ No memories to forget",
                spokenResponse = "You don't have any saved memories right now."
            )
        }
        val removed = repository.forgetMatchingMemory(keyword)
        return if (removed > 0) {
            ActionResult(
                success = true,
                action = ActionType.FORGET_MEMORY,
                systemSummary = "✓ Removed $removed memory item(s)",
                spokenResponse = "Done. I've forgotten anything matching $keyword."
            )
        } else {
            ActionResult(
                success = false,
                action = ActionType.FORGET_MEMORY,
                systemSummary = "✕ No matching memory found",
                spokenResponse = "I couldn't find any saved memory about $keyword."
            )
        }
    }

    private suspend fun executeClearMemory(lang: AssistantLanguage): ActionResult {
        repository.clearMemories()
        return ActionResult(
            success = true,
            action = ActionType.CLEAR_MEMORY,
            systemSummary = "✓ All memories cleared",
            spokenResponse = "I've cleared all your saved memories."
        )
    }

    private fun launchSystemIntent(intent: Intent, label: String, lang: AssistantLanguage): ActionResult {
        return if (tryStartActivity(intent)) {
            ActionResult(
                success = true,
                action = ActionType.OPEN_APP,
                systemSummary = "✓ $label opened",
                spokenResponse = localized(lang, "Opening $label.", "$label খুলছি।", "$label खोल रही हूँ।")
            )
        } else {
            failed(ActionType.OPEN_APP, "✕ Could not open $label", "No suitable app was found for $label.")
        }
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun tryStartActivity(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun failed(action: ActionType, summary: String, spoken: String): ActionResult {
        return ActionResult(
            success = false,
            action = action,
            systemSummary = summary,
            spokenResponse = spoken,
            errorReason = spoken
        )
    }

    private fun localized(lang: AssistantLanguage, en: String, bn: String, hi: String): String {
        return when (lang) {
            AssistantLanguage.BENGALI -> bn
            AssistantLanguage.HINDI -> hi
            else -> en
        }
    }
}
