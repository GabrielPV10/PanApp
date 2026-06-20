package com.example.panapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.panapp.ui.theme.PanAppTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

class RespaldoActivity : ComponentActivity() {
    private val vm: PanViewModel by viewModels { PanViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PanAppTheme {
                RespaldoScreen(vm = vm, onVolver = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RespaldoScreen(vm: PanViewModel, onVolver: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var ocupado          by remember { mutableStateOf(false) }
    var jsonPorImportar  by remember { mutableStateOf<String?>(null) }

    // ── Exportar: el sistema pide dónde guardar el archivo .json ──────────────
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            ocupado = true
            val ok = runCatching {
                val json = vm.construirRespaldoJson()
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray())
                } ?: error("No se pudo escribir el archivo")
            }.isSuccess
            ocupado = false
            snackbar.showSnackbar(
                if (ok) "✅ Respaldo guardado" else "❌ No se pudo exportar"
            )
        }
    }

    // ── Importar: el sistema deja elegir un archivo de respaldo ───────────────
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val contenido = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (contenido.isNullOrBlank()) {
                snackbar.showSnackbar("❌ No se pudo leer el archivo")
            } else {
                jsonPorImportar = contenido   // dispara el diálogo de confirmación
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Respaldo de datos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Explicación ───────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudUpload, null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Protege tu información",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 16.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Los datos viven solo en este teléfono. Exporta un respaldo " +
                        "y guárdalo en Google Drive o envíalo por WhatsApp para no " +
                        "perderlo si cambias o pierdes el equipo.",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Exportar ──────────────────────────────────────────────
            Button(
                onClick  = {
                    exportLauncher.launch("respaldo_pan_${LocalDate.now()}.json")
                },
                enabled  = !ocupado,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                if (ocupado) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Exportar respaldo", fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Importar ──────────────────────────────────────────────
            OutlinedButton(
                onClick  = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                enabled  = !ocupado,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Restore, null)
                Spacer(Modifier.width(8.dp))
                Text("Importar respaldo", fontWeight = FontWeight.SemiBold)
            }

            Text(
                "Importar reemplaza todos los datos actuales por los del archivo.",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.outline
            )
        }
    }

    // ── Confirmación de importación ───────────────────────────────────────────
    val json = jsonPorImportar
    if (json != null) {
        AlertDialog(
            onDismissRequest = { jsonPorImportar = null },
            icon  = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Restaurar este respaldo?") },
            text  = {
                Text(
                    "Se borrarán todos los datos actuales (clientes, pedidos, " +
                    "catálogo) y se reemplazarán por los del archivo. Esta acción " +
                    "no se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        jsonPorImportar = null
                        scope.launch {
                            ocupado = true
                            val ok = runCatching { vm.restaurarRespaldo(json) }.isSuccess
                            ocupado = false
                            if (ok) {
                                // Reinicia la app para recargar el estado desde la base restaurada
                                val intent = Intent(context, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                            } else {
                                snackbar.showSnackbar("❌ Archivo de respaldo inválido")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { jsonPorImportar = null }) { Text("Cancelar") }
            }
        )
    }
}
