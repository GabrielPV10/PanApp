package com.example.panapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.panapp.database.SemanaConClientes
import com.example.panapp.ui.UiState
import com.example.panapp.ui.asUiState
import com.example.panapp.ui.theme.PanAppTheme

class HistorialActivity : ComponentActivity() {
    private val vm: PanViewModel by viewModels { PanViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PanAppTheme {
                HistorialScreen(vm = vm, onVolver = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(vm: PanViewModel, onVolver: () -> Unit) {
    val historialState by remember { vm.historial.asUiState() }
        .collectAsStateWithLifecycle(UiState.Loading)
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📅 Historial", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        HistorialLista(
            modifier      = Modifier.padding(innerPadding),
            historialState = historialState,
            onVerResumen  = { semanaId ->
                context.startActivity(
                    Intent(context, ResumenActivity::class.java).apply {
                        putExtra("SEMANA_ID", semanaId)
                    }
                )
            }
        )
    }
}

// ─── CONTENIDO SIN SCAFFOLD (para tab en MainActivity) ───────────────────────

@Composable
fun HistorialContent(
    modifier: Modifier = Modifier,
    vm: PanViewModel,
    onVerResumen: (Long) -> Unit
) {
    val historialState by remember { vm.historial.asUiState() }
        .collectAsStateWithLifecycle(UiState.Loading)

    HistorialLista(
        modifier       = modifier,
        historialState = historialState,
        onVerResumen   = onVerResumen
    )
}

// ─── LISTA DE HISTORIAL CON ESTADOS ──────────────────────────────────────────

@Composable
private fun HistorialLista(
    modifier: Modifier = Modifier,
    historialState: UiState<List<SemanaConClientes>>,
    onVerResumen: (Long) -> Unit
) {
    val base = modifier.fillMaxSize()
    when (val estado = historialState) {
        is UiState.Loading -> Box(base, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is UiState.Error -> Box(base, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text("No se pudo cargar el historial")
                Text(estado.message, color = MaterialTheme.colorScheme.outline)
            }
        }

        is UiState.Empty -> Box(base, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📅", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text("Aún no hay semanas anteriores", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Las semanas cerradas aparecerán aquí",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        is UiState.Success -> LazyColumn(
            modifier       = base,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(estado.data) { semanaConClientes ->
                TarjetaHistorial(
                    semanaConClientes = semanaConClientes,
                    onVerResumen      = onVerResumen
                )
            }
        }
    }
}

// ─── TARJETA DE SEMANA HISTÓRICA ─────────────────────────────────────────────

@Composable
fun TarjetaHistorial(
    semanaConClientes: SemanaConClientes,
    onVerResumen: ((Long) -> Unit)? = null
) {
    var expandida by remember { mutableStateOf(false) }
    val semana    = semanaConClientes.semana
    val clientes  = semanaConClientes.clientes
    val totalPiezas = clientes.sumOf { c -> c.items.sumOf { it.cantidad } }
    val totalDinero = clientes.sumOf { c -> c.items.sumOf { it.cantidad * it.precioUnitario } }
    val entregados  = clientes.count { it.cliente.entregado }

    val rangoFechas = buildString {
        append(semana.fechaInicio.toFechaCorta())
        semana.fechaCierre?.let { append(" – ${it.toFechaCorta()}") }
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Cabecera ──────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(semana.etiqueta, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        rangoFechas,
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "${clientes.size} clientes · $totalPiezas piezas · $${"%.0f".format(totalDinero)}",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.outline
                    )
                }
                Badge(
                    containerColor = if (entregados == clientes.size && clientes.isNotEmpty())
                        MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("$entregados/${clientes.size} ✓")
                }
            }

            // ── Botones de acción ─────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (clientes.isNotEmpty()) {
                    TextButton(onClick = { expandida = !expandida }) {
                        Text(if (expandida) "▲ Ocultar" else "▼ Ver clientes")
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (onVerResumen != null) {
                    TextButton(onClick = { onVerResumen(semana.id) }) {
                        Icon(
                            Icons.Default.Summarize, null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Ver resumen")
                    }
                }
            }

            // ── Lista de clientes expandible ──────────────────────────
            if (expandida) {
                clientes.forEach { clienteConItems ->
                    val c      = clienteConItems.cliente
                    val piezas = clienteConItems.items.sumOf { it.cantidad }
                    val dinero = clienteConItems.items.sumOf { it.cantidad * it.precioUnitario }
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (c.entregado) Icons.Default.CheckCircle
                                else Icons.Default.RadioButtonUnchecked,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = if (c.entregado) MaterialTheme.colorScheme.tertiary
                                       else MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(c.nombre, fontSize = 14.sp)
                        }
                        Text(
                            "$piezas pzas · $${"%.0f".format(dinero)}",
                            fontSize = 13.sp,
                            color    = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

private fun String.toFechaCorta(): String = try {
    val date = java.time.LocalDate.parse(this)
    val fmt  = java.time.format.DateTimeFormatter.ofPattern(
        "d MMM", java.util.Locale("es", "MX")
    )
    date.format(fmt).replaceFirstChar { it.uppercaseChar() }
} catch (_: Exception) { this }