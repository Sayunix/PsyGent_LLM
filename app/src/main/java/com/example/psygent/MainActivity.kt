package com.example.psygent

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.compose.foundation.layout.*
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            import androidx.compose.material3.*
            import androidx.compose.runtime.*
            import androidx.compose.ui.Alignment
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.text.font.FontWeight
            import androidx.compose.ui.unit.dp
            import com.example.psygent.network.LLMService   // ← hier dein Import
            import com.example.psygent.ui.theme.PsyGentTheme
            import kotlinx.coroutines.launch

    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                PsyGentTheme {
                    ChatScreen()
                }
            }
        }
    }

    @Composable

    fun ChatScreen() {
        var userInput by remember { mutableStateOf("") }
        var chatHistory by remember { mutableStateOf(emptyList<Pair<String,String>>()) }
        val scope = rememberCoroutineScope()

    // setzt beim ersten Composable-Passchat den Verlauf zurück
    LaunchedEffect(Unit) {
        chatHistory = emptyList()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Anzeige des bisherigen Verlaufs
        LazyColumn(Modifier.weight(1f)) {
            items(chatHistory) { (user, bot) ->
                Text("Du: $user")
                Text("PsyGent: $bot", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
        }

        // Eingabezeile und Senden‑Button
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Schreib was…") }
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (userInput.isNotBlank()) {
                    scope.launch {
                        // Der runCatching‑Block fängt jetzt alle Fehler ab
                        val reply = runCatching {
                            LLMService.generate(userInput)
                        }.getOrElse { ex ->
                            "Fehler: ${ex.localizedMessage}"
                        }
                        chatHistory = chatHistory + (userInput to reply)
                        userInput = ""
                    }
                }
            }) {
                Text("Senden")
            }
            // ─────────────────────────────────────────────────────────
        }
    }
}