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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.psygent.network.LLMService
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    var userInput     by remember { mutableStateOf("") }
    var chatHistory   by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    var showDisclaimer by remember { mutableStateOf(true) }
    var isLoading     by remember { mutableStateOf(false) }
    val scope         = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        chatHistory = emptyList()
    }

    // Disclaimer-Dialog über Scaffold legen
    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = { /* no-op */ },
            title = { Text("Wichtiger Hinweis") },
            text = {
                Text(
                    "Dieser Chatbot ist kein getestetes Medizinprodukt. Er ersetzt keinen Psychotherapeuten "
                            + "oder andere professionelle psychologische Hilfe. Er kann Fehler machen und versteht "
                            + "nicht alle Nuancen. Bei ernsten Anliegen wende dich bitte an eine Fachperson oder "
                            + "rufe 144 (Rettung) an."
                )
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimer = false }) {
                    Text("Verstanden")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "PsyGent",
                        style =      MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)              // Abstand unter der AppBar
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Chat-Verlauf
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatHistory) { (user, bot) ->
                    Text("Du: $user")
                    Text("PsyGent: $bot", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Eingabe-Zeile
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    placeholder = { Text("Schreib was…") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val reply = runCatching {
                                LLMService.generate(userInput)
                            }.getOrElse { ex ->
                                "Fehler: ${ex.localizedMessage}"
                            }
                            chatHistory = chatHistory + (userInput to reply)
                            userInput = ""
                            isLoading = false
                        }
                    },
                    enabled = !isLoading && userInput.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Senden")
                    }
                }
            }
        }
    }
}
