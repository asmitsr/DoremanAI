package com.sneha.ai.ui

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sneha.ai.coding.TermuxRunner
import java.io.ByteArrayInputStream

@Composable
fun CodingScreen(vm: CodingViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val lang by vm.lang.collectAsStateWithLifecycle()
    val prompt by vm.prompt.collectAsStateWithLifecycle()
    val code by vm.code.collectAsStateWithLifecycle()
    val note by vm.note.collectAsStateWithLifecycle()
    val log by vm.log.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val pendingScript by vm.pendingScript.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val needPerm by vm.needPerm.collectAsStateWithLifecycle()
    var project by rememberSaveable { mutableStateOf("my-project") }
    var task by rememberSaveable { mutableStateOf("write") }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { g ->
        if (g) vm.requestRun() else vm.log.value = "Termux RUN_COMMAND permission was not granted — the code was not run."
    }
    LaunchedEffect(needPerm) {
        if (needPerm) { vm.needPerm.value = false; permLauncher.launch(TermuxRunner.PERM) }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { ctx.contentResolver.openOutputStream(it)?.use { o -> o.write(code.toByteArray()) } }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text("Coding Mode", style = MaterialTheme.typography.titleLarge)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            vm.langs.forEach { l -> FilterChip(selected = l == lang, onClick = { vm.lang.value = l }, label = { Text(l) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("write" to "Write", "explain" to "Explain", "fix" to "Fix").forEach { (k, l) ->
                FilterChip(selected = task == k, onClick = { task = k }, label = { Text(l) })
            }
        }
        OutlinedTextField(
            value = prompt, onValueChange = { vm.prompt.value = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text(if (task == "fix") "Problem / error (if blank, the last log will be used)" else "What do you want to build?") }
        )
        Button(onClick = { vm.ask(task) }, enabled = !busy) { Text(if (busy) "Please wait..." else "Ask Gemini") }
        if (note.isNotBlank()) Text(note, style = MaterialTheme.typography.bodyMedium)

        // v2.0 editor with line numbers. The whole screen scrolls (existing
        // verticalScroll), so the numbers scroll with the page; they are not
        // synced to the text field's own internal scroll — see CHANGELOG-2.0.
        SectionTitle("Editor")
        Row(Modifier.fillMaxWidth()) {
            val lineCount = code.lines().size
            Text(
                text = (1..lineCount).joinToString("\n"),
                modifier = Modifier.width(32.dp).padding(top = 52.dp, end = 4.dp),
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
            OutlinedTextField(
                value = code, onValueChange = { vm.code.value = it },
                modifier = Modifier.weight(1f).heightIn(min = 220.dp),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                label = { Text("Code ($lang)") }
            )
        }
        // v2.0 toolbar: Run / Stop / Save / Export are separate, labelled actions.
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.requestRun() }) { Text("▶ Run") }
            OutlinedButton(onClick = { vm.cancelRun() }, enabled = running) { Text("■ Stop") }
            OutlinedButton(onClick = { clip.setText(AnnotatedString(code)) }) { Text("Copy") }
            OutlinedButton(onClick = { vm.save(project) }) { Text("Save") }
            OutlinedButton(onClick = { export.launch(vm.fileName()) }) { Text("Export") }
        }
        OutlinedTextField(project, { project = it }, Modifier.fillMaxWidth(), label = { Text("Project name (for saving)") }, singleLine = true)
        Text(
            "HTML/CSS/JS: preview in a separate sandboxed WebView (network off). Python/C/C++/Java/Bash/Node: Termux is required, with your approval each time. Kotlin and others: only generated, not run.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (log.isNotBlank()) {
            SectionTitle("Output")
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                Text(log, Modifier.fillMaxWidth().padding(8.dp), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (running) OutlinedButton(onClick = { vm.cancelRun() }) { Text("■ Cancel wait") }
    }

    pendingScript?.let { sc ->
        AlertDialog(
            onDismissRequest = { vm.pendingScript.value = null },
            title = { Text("This command will run in Termux") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(sc, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp))
                    Spacer(Modifier.height(6.dp))
                    Text("It will run for at most 60 seconds. Files will be in ~/sneha_run. Approve only if you understand it.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = { vm.approveRun() }) { Text("Approve and run") } },
            dismissButton = { TextButton(onClick = { vm.pendingScript.value = null }) { Text("Cancel") } }
        )
    }

    preview?.let { html ->
        Dialog(onDismissRequest = { vm.preview.value = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sandbox preview", Modifier.padding(12.dp).weight(1f))
                        TextButton(onClick = { vm.preview.value = null }) { Text("Close ✕") }
                    }
                    SandboxWebView(html, Modifier.fillMaxSize())
                }
            }
        }
    }
}

/** JS runs, but: no Java bridge, file / content access is off, and network and navigation are blocked. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SandboxWebView(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { c ->
            WebView(c).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                settings.blockNetworkLoads = true
                settings.setSupportMultipleWindows(false)
                settings.javaScriptCanOpenWindowsAutomatically = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val sch = request?.url?.scheme
                        return if (sch == "http" || sch == "https")
                            WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0))) else null
                    }
                }
                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() }
    )
}
