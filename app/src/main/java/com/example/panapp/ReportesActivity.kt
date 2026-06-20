package com.example.panapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.panapp.ui.theme.PanAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val PREFS_REPORTE    = "reporte_prefs"
private const val KEY_TOKEN        = "jwt_token"
private const val KEY_USUARIO_ID   = "usuario_id"
private const val KEY_ULTIMO_ENVIO = "ultimo_envio"
private const val BASE_URL         = "https://laraza.mooo.com/api"

class ReportesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PanAppTheme {
                ReportesScreen(onVolver = { finish() })
            }
        }
    }
}

// ─── PANTALLA PRINCIPAL ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(onVolver: () -> Unit) {
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences(PREFS_REPORTE, Context.MODE_PRIVATE) }

    var token      by remember { mutableStateOf(prefs.getString(KEY_TOKEN, "") ?: "") }
    var usuarioId  by remember { mutableStateOf(prefs.getString(KEY_USUARIO_ID, "") ?: "") }
    var ultimoEnvio by remember { mutableStateOf(prefs.getString(KEY_ULTIMO_ENVIO, null)) }

    var enviando    by remember { mutableStateOf(false) }
    var showConfig  by remember { mutableStateOf(usuarioId.isBlank()) }
    var showToken   by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope    = rememberCoroutineScope()

    val ahora     = remember { LocalDate.now() }
    val mesNombre = remember {
        ahora.month.getDisplayName(TextStyle.FULL, Locale("es", "MX"))
            .replaceFirstChar { it.uppercaseChar() }
    }
    val periodo = "$mesNombre ${ahora.year}"

    fun enviarReporte() {
        if (usuarioId.isBlank()) {
            scope.launch { snackbar.showSnackbar("Configura el ID de usuario primero") }
            showConfig = true
            return
        }
        enviando = true
        scope.launch {
            val codigo = withContext(Dispatchers.IO) {
                runCatching {
                    val conn = (URL("$BASE_URL/reportes/enviar-reporte").openConnection()
                            as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
                        doOutput      = true
                        connectTimeout = 15_000
                        readTimeout    = 30_000
                    }
                    conn.outputStream.bufferedWriter().use {
                        it.write("""{"usuario_id": $usuarioId}""")
                    }
                    conn.responseCode
                }.getOrElse { -1 }
            }
            enviando = false
            if (codigo == 200) {
                val ts = "${ahora.dayOfMonth} de $mesNombre ${ahora.year}"
                prefs.edit().putString(KEY_ULTIMO_ENVIO, ts).apply()
                ultimoEnvio = ts
                snackbar.showSnackbar("✅ Reporte enviado al administrador")
            } else {
                snackbar.showSnackbar(
                    if (codigo == -1) "❌ Sin conexión al servidor"
                    else "❌ Error al enviar el reporte (código $codigo)"
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title        = { Text("Reportes", fontWeight = FontWeight.Bold) },
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

            // ── Cabecera con gradiente ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Assessment,
                        contentDescription = null,
                        tint     = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Reporte de Campo",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 20.sp
                        )
                        Text(
                            periodo,
                            color    = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                        Text(
                            "La Raza Semillas y Agroinsumos",
                            color    = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ── Qué incluye este reporte ──────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Este reporte incluye",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        Icons.Default.LocationOn to "Visitas del mes con detalle por productor",
                        Icons.Default.Eco        to "Cultivos atendidos y frecuencia",
                        Icons.Default.Inventory2 to "Productos más recomendados",
                        Icons.Default.Email      to "Se envía directamente al administrador"
                    ).forEach { (icon, texto) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.padding(vertical = 5.dp)
                        ) {
                            Icon(
                                icon, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                texto,
                                fontSize = 13.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Último envío ──────────────────────────────────────────
            if (ultimoEnvio != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier          = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Último envío",
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 13.sp
                            )
                            Text(
                                ultimoEnvio!!,
                                fontSize = 12.sp,
                                color    = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // ── Configuración ─────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Settings, null,
                                modifier = Modifier.size(18.dp),
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Configuración", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        TextButton(onClick = { showConfig = !showConfig }) {
                            Text(if (showConfig) "Ocultar" else "Editar")
                        }
                    }

                    if (!showConfig) {
                        Text(
                            text  = if (usuarioId.isNotBlank()) "Usuario ID $usuarioId configurado ✓"
                                    else "Sin configurar — toca Editar",
                            fontSize = 13.sp,
                            color    = if (usuarioId.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                       else MaterialTheme.colorScheme.error
                        )
                    } else {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value         = usuarioId,
                            onValueChange = { usuarioId = it },
                            label         = { Text("ID de usuario") },
                            leadingIcon   = { Icon(Icons.Default.Person, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value         = token,
                            onValueChange = { token = it },
                            label         = { Text("Token JWT") },
                            leadingIcon   = { Icon(Icons.Default.Key, null) },
                            trailingIcon  = {
                                IconButton(onClick = { showToken = !showToken }) {
                                    Icon(
                                        if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (showToken) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            singleLine   = true,
                            modifier     = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                prefs.edit()
                                    .putString(KEY_TOKEN, token.trim())
                                    .putString(KEY_USUARIO_ID, usuarioId.trim())
                                    .apply()
                                showConfig = false
                                scope.launch { snackbar.showSnackbar("Configuración guardada") }
                            },
                            enabled  = usuarioId.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Guardar")
                        }
                    }
                }
            }

            // ── Botón enviar ──────────────────────────────────────────
            Button(
                onClick  = { enviarReporte() },
                enabled  = !enviando && usuarioId.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (enviando) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Enviando...", color = Color.White, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Default.Send, null, tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("Enviar Reporte", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
