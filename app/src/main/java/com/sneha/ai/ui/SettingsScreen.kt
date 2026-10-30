package com.sneha.ai.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sneha.ai.SnehaApp
import com.sneha.ai.apps.AppInfo
import com.sneha.ai.automation.SnehaAccessibilityService
import com.sneha.ai.coding.TermuxRunner
import com.sneha.ai.data.K
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class SettingSection(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val sections = listOf(
    SettingSection("ai", "AI Brain & Custom Gateway", "Third-party gateway URL, sk- token, Claude / GPT and Gemini", Icons.Default.Psychology),
    SettingSection("privacy", "Permission setup hub", "Call, contacts, SMS, microphone and Accessibility permissions", Icons.Default.Security),
    SettingSection("voice", "Voice and silent wake mode", "Bengali / English voices, silent listening and speed", Icons.Default.RecordVoiceOver),
    SettingSection("auto", "Screen automation and system control", "Game installs, WhatsApp sending, screen lock and power menu", Icons.Default.AccessibilityNew),
    SettingSection("apps", "All installed applications", "Choose favourite apps and launch them quickly", Icons.Default.Apps),
    SettingSection("coding", "Coding runtime", "WebView sandbox and Termux integration", Icons.Default.Code),
    SettingSection("memory", "Personal memory", "View, edit and export saved information", Icons.Default.Memory),
    SettingSection("look", "Theme and font", "Dark / light theme, text size and animation", Icons.Default.Palette),
    SettingSection("notif", "Notifications", "Automation status and STOP notifications", Icons.Default.Notifications),
    SettingSection("about", "About Doreman", "Version, real feature status and information", Icons.Default.Info)
)

@Composable
fun SettingsScreen(vm: MainViewModel, initial: String?, onBack: () -> Unit) {
    var page by rememberSaveable(initial) { mutableStateOf<String?>(initial) }
    BackHandler(page != null) { page = null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (page != null) page = null else onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = sections.firstOrNull { it.key == page }?.title ?: "Doreman Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(8.dp))

        when (page) {
            null -> Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // v2.0: landing page grouped under 6 headings (same pages, same keys).
                val groups = listOf(
                    "AI & Models" to listOf("ai"),
                    "Automation & Permissions" to listOf("auto", "privacy"),
                    "Voice & Accessibility" to listOf("voice"),
                    "Apps & Integrations" to listOf("apps", "coding"),
                    "Privacy & Security" to listOf("memory", "notif"),
                    "Appearance & About" to listOf("look", "about")
                )
                groups.forEach { (groupTitle, keys) ->
                    SectionTitle(groupTitle)
                    keys.mapNotNull { key -> sections.firstOrNull { it.key == key } }.forEach { sec ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { page = sec.key }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = sec.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(sec.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(sec.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                } // end groups.forEach
            }
            "ai" -> AiPage(vm)
            "voice" -> VoicePage(vm)
            "auto" -> AutoPage(vm)
            "apps" -> AppsPage(vm)
            "coding" -> CodingSettingsPage(vm)
            "memory" -> MemoryPage(vm)
            "privacy" -> PrivacyPage(vm)
            "look" -> LookPage(vm)
            "notif" -> NotifPage(vm)
            else -> AboutPage()
        }
    }
}

@Composable
fun SecCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun Note(t: String) = Text(
    t,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(vertical = 2.dp)
)

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked, onChange)
    }
}

@Composable
fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected, onClick)
        // v1.2.1: explicit onSurface colour — without it the model list
        // rendered near-black on the dark / Midnight background.
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun PrefSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, fmt: (Float) -> String, onDone: (Float) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value) }
    Text("$label: ${fmt(v)}", style = MaterialTheme.typography.bodyMedium)
    Slider(v, { v = it }, valueRange = range, onValueChangeFinished = { onDone(v) })
}

@Composable
fun resumeTick(): Int {
    val lo = LocalLifecycleOwner.current
    var t by remember { mutableIntStateOf(0) }
    DisposableEffect(lo) {
        val ob = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) t++ }
        lo.lifecycle.addObserver(ob)
        onDispose { lo.lifecycle.removeObserver(ob) }
    }
    return t
}

private fun granted(ctx: Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
private fun openIntent(ctx: Context, i: Intent, vm: MainViewModel) {
    try {
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        vm.banner.value = "Could not open the Settings screen"
    }
}

@Composable
fun AiPage(vm: MainViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val gemKeySet by vm.geminiKeySet.collectAsStateWithLifecycle()
    val gwKeySet by vm.gatewayKeySet.collectAsStateWithLifecycle()
    val conn by vm.conn.collectAsStateWithLifecycle()
    val testing by vm.testingConn.collectAsStateWithLifecycle()
    val models by vm.models.collectAsStateWithLifecycle()

    var baseUrlInput by remember(s.gatewayBaseUrl) { mutableStateOf(s.gatewayBaseUrl) }
    var apiKeyInput by remember { mutableStateOf("") }
    var customModelInput by remember(s.model) { mutableStateOf(s.model) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        SecCard("1. Connection Mode") {
            RadioRow(
                label = "Custom Gateway (third-party URL + sk-... token / Claude / GPT)",
                selected = s.providerMode == "gateway"
            ) { vm.set(K.providerMode, "gateway") }
            RadioRow(
                label = "Google Gemini Official API",
                selected = s.providerMode == "gemini"
            ) { vm.set(K.providerMode, "gemini") }
        }

        if (s.providerMode == "gateway") {
            SecCard("2. Gateway Credentials (video-style setup)") {
                Note("Enter the Base URL of any third-party website (for example https://tabitoken.com) and your API key (sk-...):")
                OutlinedTextField(
                    value = baseUrlInput,
                    onValueChange = {
                        baseUrlInput = it
                        vm.set(K.gatewayBaseUrl, it.trim())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Gateway Base URL (for example: https://tabitoken.com)") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (gwKeySet) "If you enter a new Gateway API key (sk-...), it will replace the previous one" else "Gateway API Key (sk-...)") },
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(8.dp))
                Text("Gateway Auth Scheme:", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = s.gatewayAuthScheme == "bearer",
                        onClick = { vm.set(K.gatewayAuthScheme, "bearer") },
                        label = { Text("Authorization: Bearer") }
                    )
                    FilterChip(
                        selected = s.gatewayAuthScheme == "x-api-key",
                        onClick = { vm.set(K.gatewayAuthScheme, "x-api-key") },
                        label = { Text("x-api-key (Claude)") }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Endpoint Format:", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = s.gatewayFormat == "auto",
                        onClick = { vm.set(K.gatewayFormat, "auto") },
                        label = { Text("Auto (OpenAI + Claude)") }
                    )
                    FilterChip(
                        selected = s.gatewayFormat == "openai",
                        onClick = { vm.set(K.gatewayFormat, "openai") },
                        label = { Text("/v1/chat/completions") }
                    )
                    FilterChip(
                        selected = s.gatewayFormat == "anthropic",
                        onClick = { vm.set(K.gatewayFormat, "anthropic") },
                        label = { Text("/v1/messages") }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            vm.set(K.gatewayBaseUrl, baseUrlInput.trim())
                            if (apiKeyInput.isNotBlank()) {
                                vm.saveGatewayKey(apiKeyInput)
                                apiKeyInput = ""
                            }
                        }
                    ) { Text("Save Gateway") }
                    Button(
                        onClick = { vm.testConnection() },
                        enabled = !testing
                    ) { Text(if (testing) "Testing..." else "Test Connection") }
                }
                Spacer(Modifier.height(6.dp))
                Text(conn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Note("Tip: the Base URL, Auth Scheme (Bearer / x-api-key), Format (OpenAI / Anthropic) and model name must all match your gateway. 401 means a wrong key, 404 means a wrong model / URL. Test Connection will not show a model list for a wrong key.")
            }
        } else {
            SecCard("2. Google Gemini API Key") {
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (gemKeySet) "If you enter a new Gemini API key, it will replace the previous one" else "Enter your Gemini API key") },
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.saveKey(apiKeyInput); apiKeyInput = "" },
                        enabled = apiKeyInput.isNotBlank()
                    ) { Text("Save Key") }
                    Button(
                        onClick = { vm.testConnection() },
                        enabled = gemKeySet && !testing
                    ) { Text(if (testing) "Testing..." else "Test Connection") }
                }
                Spacer(Modifier.height(6.dp))
                Text(conn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }

        SecCard("3. Choose a premium model or type any model name") {
            Text("Currently active model: ${s.model}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = customModelInput,
                onValueChange = {
                    customModelInput = it
                    if (it.isNotBlank()) vm.set(K.model, it.trim())
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Type any model ID (for example: claude-opus-4-6, gpt-4o)") }
            )
            if (models.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Choose from the list:", style = MaterialTheme.typography.bodySmall)
                models.forEach { m ->
                    RadioRow(m, m == s.model) {
                        customModelInput = m
                        vm.set(K.model, m)
                    }
                }
            } else {
                Note("Tapping 'Test Connection' will automatically list all premium models available on your token below.")
            }
        }
    }
}

@Composable
fun VoicePage(vm: MainViewModel) {
    val ctx = LocalContext.current
    val a = ctx.applicationContext as SnehaApp
    val s by vm.settings.collectAsStateWithLifecycle()
    val handsFree by vm.handsFree.collectAsStateWithLifecycle()
    var tick by remember { mutableIntStateOf(0) }
    val voices = remember(s.lang, tick) { a.tts.voices(s.lang) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        SecCard("Language") {
            RadioRow("Bengali", s.lang == "bn") { vm.set(K.lang, "bn") }
            RadioRow("English", s.lang == "en") { vm.set(K.lang, "en") }
            Note("Changing the language changes voice recognition (bn-BD / en-US), speech (TTS) and the language of the AI's replies.")
        }
        SecCard("Silent wake mode (no beep sound)") {
            SwitchRow("Read Doreman's replies aloud", s.autoSpeak) { vm.set(K.autoSpeak, it) }
            SwitchRow("Silently wait only for the 'Doreman' call", handsFree) { vm.toggleHandsFree(it) }
            Note("No 'ding / beep' sound will play when the microphone turns on or off, and the app will stay fully silent after you leave it.")
            PrefSlider("Speech speed", s.speechRate, 0.5f..2f, { "%.1fx".format(it) }) { vm.set(K.rate, it) }
            PrefSlider("Volume", s.volume, 0f..1f, { "${(it * 100).toInt()}%" }) { vm.set(K.volume, it) }
        }

        SecCard("Choose a voice") {
            RadioRow("System default", s.voiceName.isBlank()) { vm.set(K.voice, "") }
            voices.forEach { RadioRow(it, s.voiceName == it) { vm.set(K.voice, it) } }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    vm.speak("Hello, I am Doreman. How can I help you?") // v2.0: voice test is English
                }) { Text("Voice test") }
                OutlinedButton(onClick = { tick++ }) { Text("Refresh list") }
            }
        }
    }
}

@Composable
fun AutoPage(vm: MainViewModel) {
    val ctx = LocalContext.current
    val on by SnehaAccessibilityService.connected.collectAsStateWithLifecycle()
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SecCard("Accessibility service status") {
            Text(
                if (on) "✓ Accessibility service is on" else "✗ Accessibility service is off",
                style = MaterialTheme.typography.titleMedium,
                color = if (on) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { openIntent(ctx, Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS), vm) }) {
                Text("Open Android Accessibility Settings")
            }
        }
    }
}

@Composable
fun AppsPage(vm: MainViewModel) {
    val a = LocalContext.current.applicationContext as SnehaApp
    val s by vm.settings.collectAsStateWithLifecycle()
    var q by remember { mutableStateOf("") }
    val apps by produceState(emptyList<AppInfo>()) {
        value = withContext(Dispatchers.Default) { a.launcher.launchable() }
    }
    Column {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search installed apps") })
        LazyColumn(Modifier.weight(1f)) {
            items(apps.filter { it.label.contains(q, true) }, key = { it.pkg }) { ap ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ap.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { vm.openApp(ap.label) }) { Text("Open") }
                    Checkbox(
                        checked = ap.pkg in s.favorites,
                        onCheckedChange = { c -> vm.set(K.favorites, if (c) s.favorites + ap.pkg else s.favorites - ap.pkg) }
                    )
                }
            }
        }
    }
}

@Composable
fun CodingSettingsPage(vm: MainViewModel) {
    val a = LocalContext.current.applicationContext as SnehaApp
    val s by vm.settings.collectAsStateWithLifecycle()
    val tick = resumeTick()
    val installed = remember(tick) { a.termux.installed() }
    val perm = remember(tick) { a.termux.hasPermission() }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        SecCard("Termux code runtime") {
            SwitchRow("Run code with Termux", s.termuxEnabled) { vm.set(K.termux, it) }
            Text("Termux app: " + if (installed) "✓ Installed" else "✗ Not installed")
            Text("RUN_COMMAND permission: " + if (perm) "✓ Granted" else "✗ Not granted")
            Spacer(Modifier.height(6.dp))
            Button(onClick = { permLauncher.launch(TermuxRunner.PERM) }, enabled = installed && !perm) {
                Text("Allow RUN_COMMAND permission")
            }
        }
    }
}

@Composable
fun MemoryPage(vm: MainViewModel) {
    val a = LocalContext.current.applicationContext as SnehaApp
    val s by vm.settings.collectAsStateWithLifecycle()
    val items by vm.memory.collectAsStateWithLifecycle()
    var newText by remember { mutableStateOf("") }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        SecCard("Memory control") {
            SwitchRow("Keep Doreman's memory on", s.memoryEnabled) { vm.set(K.memory, it) }
            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Add new information") }
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    if (a.memory.add(newText) == null) newText = ""
                },
                enabled = newText.isNotBlank()
            ) { Text("Add") }
        }

        SecCard("Saved information (${items.size})") {
            items.forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { a.memory.delete(e.id) }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
fun PrivacyPage(vm: MainViewModel) {
    val ctx = LocalContext.current
    var permTick by remember { mutableIntStateOf(0) }
    val tick = resumeTick() + permTick
    val a11y by SnehaAccessibilityService.connected.collectAsStateWithLifecycle()
    val mic = remember(tick) { granted(ctx, Manifest.permission.RECORD_AUDIO) }
    val contacts = remember(tick) { granted(ctx, Manifest.permission.READ_CONTACTS) }
    val phone = remember(tick) { granted(ctx, Manifest.permission.CALL_PHONE) }
    val sms = remember(tick) { granted(ctx, Manifest.permission.SEND_SMS) }
    val notif = remember(tick) { Build.VERSION.SDK_INT < 33 || granted(ctx, Manifest.permission.POST_NOTIFICATIONS) }

    val multiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permTick++
    }

    fun yn(b: Boolean) = if (b) "✓ On" else "✗ Not granted"

    Column(Modifier.verticalScroll(rememberScrollState())) {
        SecCard("Turn on all permissions in 1 click") {
            Button(
                onClick = {
                    val list = mutableListOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS
                    )
                    if (Build.VERSION.SDK_INT >= 33) list.add(Manifest.permission.POST_NOTIFICATIONS)
                    multiLauncher.launch(list.toTypedArray())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow all permissions (microphone, call, contacts, SMS)")
            }
        }

        SecCard("Permission status") {
            Text("🎙️ Microphone: ${yn(mic)}")
            Text("👥 Contacts (searching for Baba): ${yn(contacts)}")
            Text("📞 Direct phone call: ${yn(phone)}")
            Text("💬 Direct SMS: ${yn(sms)}")
            Text("🔔 Notifications: ${yn(notif)}")
            Text("👆 Accessibility (game installs and WhatsApp sending): ${yn(a11y)}")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    openIntent(ctx, Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")), vm)
                }) { Text("App permissions") }
                OutlinedButton(onClick = { openIntent(ctx, Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS), vm) }) { Text("Turn on Accessibility") }
            }
        }
    }
}

@Composable
fun LookPage(vm: MainViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SecCard("Choose a theme") {
            RadioRow("Dark (Deep Cosmic Dark)", s.theme == "dark") { vm.set(K.theme, "dark") }
            RadioRow("Light (Clean Daylight)", s.theme == "light") { vm.set(K.theme, "light") }
            RadioRow("System default", s.theme == "system") { vm.set(K.theme, "system") }
            RadioRow("Midnight Indigo (Midnight Indigo #282B4A + Vanilla #EEEBDA)", s.theme == "midnight") { vm.set(K.theme, "midnight") }
        }
    }
}

@Composable
fun NotifPage(vm: MainViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SecCard("Automation notifications") {
            SwitchRow("Show a STOP notification while automation is running", s.notifyAutomation) { vm.set(K.notify, it) }
        }
    }
}

@Composable
fun AboutPage() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SecCard("Doreman AI — v2.0.0") {
            Text("Custom gateway (Claude / OpenAI / Gemini) and silent-wake voice assistant")
        }
    }
}
