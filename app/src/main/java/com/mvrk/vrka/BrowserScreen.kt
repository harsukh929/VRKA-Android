package com.mvrk.vrka

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

@Composable
fun BrowserScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val runtimeManager = remember { GeckoRuntimeManager.getInstance(context) }
    var address by remember { mutableStateOf("https://www.google.com") }
    var session by remember { mutableStateOf<GeckoSession?>(null) }
    var pageTitle by remember { mutableStateOf("Ready to browse") }

    fun navigate(input: String) {
        val value = input.trim()
        if (value.isBlank()) return
        val target = when {
            value.contains(" ") -> "https://www.google.com/search?q=" + Uri.encode(value)
            value.startsWith("https://", true) || value.startsWith("http://", true) -> value
            else -> "https://$value"
        }
        address = target
        session?.loadUri(target)
    }

    LaunchedEffect(Unit) {
        runCatching {
            runtimeManager.ensureExtensionsReady()
            val s = GeckoSession()
            s.open(runtimeManager.runtime)
            session = s
            s.loadUri("https://www.google.com")
        }.onFailure { pageTitle = "Browser could not start: ${it.message ?: "unknown error"}" }
    }

    DisposableEffect(session) {
        onDispose { session?.close() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search or enter website address") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { navigate(address) })
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { session?.goBack() }, modifier = Modifier.weight(1f)) { Text("Back") }
                    OutlinedButton(onClick = { session?.goForward() }, modifier = Modifier.weight(1f)) { Text("Forward") }
                    OutlinedButton(onClick = { session?.reload() }, modifier = Modifier.weight(1f)) { Text("Refresh") }
                    Button(onClick = { navigate(address) }, modifier = Modifier.weight(1f)) { Text("Go") }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val active = session
            if (active != null) {
                AndroidView(
                    factory = { ctx -> GeckoView(ctx).apply { setSession(active) } },
                    update = { it.setSession(active) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
