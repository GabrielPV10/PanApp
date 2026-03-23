package com.example.panapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.panapp.database.Cliente
import com.example.panapp.database.ClienteConItems
import com.example.panapp.ui.theme.PanAppTheme

class MainActivity : ComponentActivity() {
    private val vm: PanViewModel by viewModels { PanViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PanAppTheme {

                // Observamos el estado aquí y lo pasamos hacia abajo
                val semanaId by vm.semanaActualId.collectAsStateWithLifecycle()
                val clientes by remember(semanaId) {
                    semanaId?.let { vm.getClientesDeSemana(it) }
                        ?: kotlinx.coroutines.flow.flowOf(emptyList())
                }.collectAsStateWithLifecycle(emptyList())

                MainScreen(
                    semanaId = semanaId,
                    clientes = clientes,
                    onNuevaSemana = { vm.crearNuevaSemana() },
                    onAgregarCliente = { nombre -> vm.agregarCliente(semanaId!!, nombre) },
                    onToggleEntregado = { cliente -> vm.toggleEntregado(cliente) },
                    onEliminarCliente = { clienteConItems -> vm.eliminarCliente(clienteConItems) },
                    onAbrirPedido = { clienteId, clienteNombre ->
                        startActivity(
                            Intent(this, PedidoActivity::class.java).apply {
                                putExtra("CLIENTE_ID", clienteId)
                                putExtra("CLIENTE_NOMBRE", clienteNombre)
                            }
                        )
                    },
                    onAbrirResumen = { id ->
                        startActivity(
                            Intent(this, ResumenActivity::class.java).apply {
                                putExtra("SEMANA_ID", id)
                            }
                        )
                    },
                    onAbrirHistorial = {
                        startActivity(Intent(this, HistorialActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    semanaId: Long?,
    clientes: List<ClienteConItems>,
    onNuevaSemana: () -> Unit,
    onAgregarCliente: (String) -> Unit,
    onToggleEntregado: (Cliente) -> Unit,
    onEliminarCliente: (ClienteConItems) -> Unit,
    onAbrirPedido: (Long, String) -> Unit,
    onAbrirResumen: (Long) -> Unit,
    onAbrirHistorial: () -> Unit
) {
    var showDialogNuevaSemana by remember { mutableStateOf(false) }
    var showDialogAgregarCliente by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🥖 Pedidos de Pan", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = onAbrirHistorial) {
                        Icon(Icons.Default.History, contentDescription = "Historial")
                    }
                    if (semanaId != null) {
                        IconButton(onClick = { onAbrirResumen(semanaId) }) {
                            Icon(Icons.Default.Summarize, contentDescription = "Resumen")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (semanaId != null) {
                FloatingActionButton(onClick = { showDialogAgregarCliente = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Agregar cliente")
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (semanaId == null) {
                // Sin semana activa
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍞", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("No hay semana activa", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { showDialogNuevaSemana = true }) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Iniciar nueva semana")
                        }
                    }
                }
            } else {
                ResumenRapido(clientes = clientes)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Clientes (${clientes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { showDialogNuevaSemana = true }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nueva semana")
                    }
                }

                if (clientes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👤", fontSize = 48.sp)
                            Text("Aún no hay clientes esta semana")
                            Text("Toca ＋ para agregar uno", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(clientes, key = { it.cliente.id }) { clienteConItems ->
                            TarjetaCliente(
                                clienteConItems = clienteConItems,
                                onClickEditar = {
                                    onAbrirPedido(
                                        clienteConItems.cliente.id,
                                        clienteConItems.cliente.nombre
                                    )
                                },
                                onToggleEntregado = { onToggleEntregado(clienteConItems.cliente) },
                                onEliminar = { onEliminarCliente(clienteConItems) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // ─── DIÁLOGOS ────────────────────────────────────────────────────────────

    if (showDialogNuevaSemana) {
        AlertDialog(
            onDismissRequest = { showDialogNuevaSemana = false },
            title = { Text("¿Iniciar nueva semana?") },
            text = { Text("Se creará una semana nueva. Los pedidos anteriores se guardan en el historial.") },
            confirmButton = {
                Button(onClick = {
                    onNuevaSemana()
                    showDialogNuevaSemana = false
                }) { Text("Sí, nueva semana") }
            },
            dismissButton = {
                TextButton(onClick = { showDialogNuevaSemana = false }) { Text("Cancelar") }
            }
        )
    }

    if (showDialogAgregarCliente) {
        AlertDialog(
            onDismissRequest = { showDialogAgregarCliente = false; nuevoNombre = "" },
            title = { Text("Agregar cliente") },
            text = {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    label = { Text("Nombre del cliente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nuevoNombre.isNotBlank()) {
                            onAgregarCliente(nuevoNombre.trim())
                            nuevoNombre = ""
                            showDialogAgregarCliente = false
                        }
                    },
                    enabled = nuevoNombre.isNotBlank()
                ) { Text("Agregar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialogAgregarCliente = false
                    nuevoNombre = ""
                }) { Text("Cancelar") }
            }
        )
    }
}

// ─── COMPONENTES ─────────────────────────────────────────────────────────────

@Composable
fun ResumenRapido(clientes: List<ClienteConItems>) {
    val entregados = clientes.count { it.cliente.entregado }
    val totalPiezas = clientes.sumOf { c -> c.items.sumOf { it.cantidad } }
    val totalDinero = clientes.sumOf { c -> c.items.sumOf { it.cantidad * it.precioUnitario } }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DatoResumen("👥", "$entregados/${clientes.size}", "Entregados")
            DatoResumen("🍞", "$totalPiezas", "Piezas")
            DatoResumen("💰", "$${"%.0f".format(totalDinero)}", "Total")
        }
    }
}

@Composable
fun DatoResumen(emoji: String, valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(etiqueta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
fun TarjetaCliente(
    clienteConItems: ClienteConItems,
    onClickEditar: () -> Unit,
    onToggleEntregado: () -> Unit,
    onEliminar: () -> Unit
) {
    val cliente = clienteConItems.cliente
    val items = clienteConItems.items
    val totalPiezas = items.sumOf { it.cantidad }
    val totalDinero = items.sumOf { it.cantidad * it.precioUnitario }
    var expandido by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cliente.entregado)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (cliente.entregado) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.primary
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        cliente.nombre.first().uppercaseChar().toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(cliente.nombre, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "$totalPiezas piezas · $${"%.0f".format(totalDinero)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onToggleEntregado) {
                    Icon(
                        if (cliente.entregado) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Entregado",
                        tint = if (cliente.entregado) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onClickEditar) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar pedido")
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (items.isNotEmpty()) {
                TextButton(
                    onClick = { expandido = !expandido },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(if (expandido) "▲ Ocultar detalle" else "▼ Ver detalle")
                }
                if (expandido) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${item.categoria}${if (item.variante.isNotBlank()) " - ${item.variante}" else ""}",
                                fontSize = 13.sp
                            )
                            Text("×${item.cantidad}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                Text(
                    "Sin productos aún · toca ✏️ para agregar",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar cliente?") },
            text = { Text("Se eliminarán todos los pedidos de ${cliente.nombre}.") },
            confirmButton = {
                Button(
                    onClick = { onEliminar(); confirmDelete = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    PanAppTheme {
        MainScreen(
            semanaId = 1L,
            clientes = emptyList(),
            onNuevaSemana = {},
            onAgregarCliente = {},
            onToggleEntregado = {},
            onEliminarCliente = {},
            onAbrirPedido = { _, _ -> },
            onAbrirResumen = {},
            onAbrirHistorial = {}
        )
    }
}