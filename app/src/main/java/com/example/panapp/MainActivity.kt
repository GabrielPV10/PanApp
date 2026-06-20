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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.panapp.database.Cliente
import com.example.panapp.database.ClienteConItems
import com.example.panapp.database.Producto
import com.example.panapp.database.Semana
import com.example.panapp.ui.UiState
import com.example.panapp.ui.asUiState
import com.example.panapp.ui.theme.PanAppTheme
import kotlinx.coroutines.flow.flowOf

class MainActivity : ComponentActivity() {
    private val vm: PanViewModel by viewModels { PanViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PanAppTheme {
                MainAppShell(
                    vm = vm,
                    onAbrirPedido = { id, nombre ->
                        startActivity(
                            Intent(this, PedidoActivity::class.java).apply {
                                putExtra("CLIENTE_ID", id)
                                putExtra("CLIENTE_NOMBRE", nombre)
                            }
                        )
                    },
                    onAbrirResumen = { semanaId ->
                        startActivity(
                            Intent(this, ResumenActivity::class.java).apply {
                                putExtra("SEMANA_ID", semanaId)
                            }
                        )
                    },
                    onAbrirReportes = {
                        startActivity(Intent(this, ReportesActivity::class.java))
                    }
                )
            }
        }
    }
}

// ─── SHELL PRINCIPAL CON BOTTOM NAV ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    vm: PanViewModel,
    onAbrirPedido: (Long, String) -> Unit,
    onAbrirResumen: (Long) -> Unit,
    onAbrirReportes: () -> Unit
) {
    val semanaId     by vm.semanaActualId.collectAsStateWithLifecycle()
    val semanaActual by vm.semanaActual.collectAsStateWithLifecycle(null)
    val clientesState by remember(semanaId) {
        semanaId?.let { vm.getClientesDeSemana(it).asUiState() } ?: flowOf(UiState.Empty)
    }.collectAsStateWithLifecycle(UiState.Loading)
    // Lista plana para los diálogos (copiar semana, validar duplicados)
    val clientes = (clientesState as? UiState.Success)?.data ?: emptyList()
    val productos by vm.productos.collectAsStateWithLifecycle(emptyList())

    var tabSeleccionado          by remember { mutableStateOf(0) }
    var showDialogNuevaSemana    by remember { mutableStateOf(false) }
    var showDialogAgregarCliente by remember { mutableStateOf(false) }
    var showDialogAgregarProduct by remember { mutableStateOf(false) }
    var copiarClientes           by remember { mutableStateOf(false) }

    // ── Filtro del catálogo ───────────────────────────────────────────────────
    var showFiltroCatalogo by remember { mutableStateOf(false) }
    var filtroCatalogo     by remember { mutableStateOf("") }

    // Resetear filtro al salir del tab de catálogo
    LaunchedEffect(tabSeleccionado) {
        if (tabSeleccionado != 1) {
            showFiltroCatalogo = false
            filtroCatalogo     = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            when (tabSeleccionado) {
                                1    -> "Catálogo de Productos"
                                2    -> "Historial"
                                else -> "Pedidos de Pan"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp
                        )
                        if (tabSeleccionado == 0 && semanaActual != null) {
                            Text(
                                semanaActual!!.etiqueta,
                                fontSize = 12.sp,
                                color    = MaterialTheme.colorScheme.onPrimaryContainer
                                    .copy(alpha = 0.75f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor   = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (tabSeleccionado == 0 && semanaId != null) {
                        IconButton(onClick = { onAbrirResumen(semanaId!!) }) {
                            Icon(
                                Icons.Default.Summarize,
                                contentDescription = "Ver resumen de producción"
                            )
                        }
                    }
                    if (tabSeleccionado == 1) {
                        IconButton(onClick = {
                            showFiltroCatalogo = !showFiltroCatalogo
                            if (!showFiltroCatalogo) filtroCatalogo = ""
                        }) {
                            Icon(
                                if (showFiltroCatalogo) Icons.Default.Close
                                else Icons.Default.Search,
                                contentDescription = if (showFiltroCatalogo) "Cerrar filtro"
                                                     else "Filtrar catálogo"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                NavigationBarItem(
                    selected = tabSeleccionado == 0,
                    onClick  = { tabSeleccionado = 0 },
                    icon     = { Icon(Icons.Default.Home, "Pedidos") },
                    label    = { Text("Pedidos") }
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 1,
                    onClick  = { tabSeleccionado = 1 },
                    icon     = { Icon(Icons.Default.Category, "Catálogo") },
                    label    = { Text("Catálogo") }
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 2,
                    onClick  = { tabSeleccionado = 2 },
                    icon     = { Icon(Icons.Default.DateRange, "Historial") },
                    label    = { Text("Historial") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick  = onAbrirReportes,
                    icon     = { Icon(Icons.Default.Assessment, "Reportes") },
                    label    = { Text("Reportes") }
                )
            }
        },
        floatingActionButton = {
            when (tabSeleccionado) {
                0 -> if (semanaId != null) {
                    FloatingActionButton(
                        onClick          = { showDialogAgregarCliente = true },
                        containerColor   = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.PersonAdd, "Agregar cliente", tint = Color.White)
                    }
                }
                1 -> FloatingActionButton(
                    onClick        = { showDialogAgregarProduct = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Agregar producto", tint = Color.White)
                }
                else -> {}
            }
        }
    ) { innerPadding ->
        when (tabSeleccionado) {
            0 -> PedidosTab(
                modifier            = Modifier.padding(innerPadding),
                semanaId            = semanaId,
                clientesState       = clientesState,
                onToggleEntregado   = { vm.toggleEntregado(it) },
                onTogglePagado      = { vm.togglePagado(it) },
                onEliminarCliente   = { vm.eliminarCliente(it) },
                onRenombrarCliente  = { c, n -> vm.actualizarNombreCliente(c, n) },
                onActualizarNotas   = { c, n -> vm.actualizarNotasCliente(c, n) },
                onAbrirPedido       = onAbrirPedido,
                onShowNuevaSemana   = { showDialogNuevaSemana = true }
            )
            1 -> CatalogoTab(
                modifier               = Modifier.padding(innerPadding),
                productos              = productos,
                filtro                 = filtroCatalogo,
                showFiltro             = showFiltroCatalogo,
                onFiltroChange         = { filtroCatalogo = it },
                onAgregarProducto      = { cat, vari, precio, emoji ->
                    vm.agregarProducto(cat, vari, precio, emoji)
                },
                onEditarProducto       = { vm.editarProducto(it) },
                onEliminarProducto     = { vm.eliminarProducto(it) },
                showDialogAgregar      = showDialogAgregarProduct,
                onDismissDialogAgregar = { showDialogAgregarProduct = false }
            )
            else -> HistorialContent(
                modifier     = Modifier.padding(innerPadding),
                vm           = vm,
                onVerResumen = onAbrirResumen
            )
        }
    }

    // ─── DIÁLOGO: NUEVA SEMANA ────────────────────────────────────────────
    if (showDialogNuevaSemana) {
        AlertDialog(
            onDismissRequest = { showDialogNuevaSemana = false; copiarClientes = false },
            title   = { Text("¿Iniciar nueva semana?") },
            text    = {
                Column {
                    Text("Los pedidos anteriores quedan guardados en el Historial.")
                    if (semanaId != null && clientes.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            shape  = RoundedCornerShape(10.dp),
                            color  = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier          = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked         = copiarClientes,
                                    onCheckedChange = { copiarClientes = it }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Copiar los ${clientes.size} clientes a la nueva semana",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.crearNuevaSemana(if (copiarClientes && semanaId != null) semanaId else null)
                    copiarClientes        = false
                    showDialogNuevaSemana = false
                }) { Text("Sí, nueva semana") }
            },
            dismissButton = {
                TextButton(onClick = { showDialogNuevaSemana = false; copiarClientes = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ─── DIÁLOGO: AGREGAR CLIENTE ─────────────────────────────────────────
    if (showDialogAgregarCliente) {
        DialogoAgregarCliente(
            clientesExistentes = clientes,
            onAgregar = { nombre, notas ->
                if (semanaId != null) vm.agregarCliente(semanaId!!, nombre, notas)
                showDialogAgregarCliente = false
            },
            onDismiss = { showDialogAgregarCliente = false }
        )
    }
}

// ─── DIÁLOGO AGREGAR CLIENTE ──────────────────────────────────────────────────

@Composable
fun DialogoAgregarCliente(
    clientesExistentes: List<ClienteConItems> = emptyList(),
    onAgregar: (nombre: String, notas: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var notas  by remember { mutableStateOf("") }

    val nombreDuplicado = nombre.isNotBlank() &&
        clientesExistentes.any { it.cliente.nombre.equals(nombre.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Agregar cliente") },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value         = nombre,
                    onValueChange = { nombre = it },
                    label         = { Text("Nombre del cliente") },
                    leadingIcon   = { Icon(Icons.Default.Person, null) },
                    isError       = nombreDuplicado,
                    supportingText = if (nombreDuplicado) {
                        { Text("Ya existe un cliente con este nombre") }
                    } else null,
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = notas,
                    onValueChange = { notas = it },
                    label         = { Text("Notas (opcional)") },
                    leadingIcon   = { Icon(Icons.Default.Note, null) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { if (nombre.isNotBlank() && !nombreDuplicado) onAgregar(nombre.trim(), notas.trim()) },
                enabled  = nombre.isNotBlank() && !nombreDuplicado,
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, null)
                Spacer(Modifier.width(6.dp))
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ─── TAB PEDIDOS ──────────────────────────────────────────────────────────────

@Composable
fun PedidosTab(
    modifier: Modifier = Modifier,
    semanaId: Long?,
    clientesState: UiState<List<ClienteConItems>>,
    onToggleEntregado: (Cliente) -> Unit,
    onTogglePagado: (Cliente) -> Unit,
    onEliminarCliente: (ClienteConItems) -> Unit,
    onRenombrarCliente: (Cliente, String) -> Unit,
    onActualizarNotas: (Cliente, String) -> Unit,
    onAbrirPedido: (Long, String) -> Unit,
    onShowNuevaSemana: () -> Unit
) {
    if (semanaId == null) {
        // Sin semana activa
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CalendarMonth, null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "No hay semana activa",
                    style    = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Inicia una semana para comenzar a registrar pedidos",
                    color    = MaterialTheme.colorScheme.outline,
                    style    = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onShowNuevaSemana,
                    shape   = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Iniciar semana")
                }
            }
        }
        return
    }

    // Cargando la primera emisión de Room
    if (clientesState is UiState.Loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (clientesState is UiState.Error) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ErrorOutline, null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(8.dp))
                Text("No se pudieron cargar los clientes")
                Text(clientesState.message, color = MaterialTheme.colorScheme.outline)
            }
        }
        return
    }

    val clientes = (clientesState as? UiState.Success)?.data ?: emptyList()

    Column(modifier = modifier.fillMaxSize()) {
        // ── Tarjeta resumen rápido ────────────────────────────────────
        ResumenRapido(clientes = clientes)

        // ── Encabezado lista de clientes ─────────────────────────────
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Text(
                "Clientes (${clientes.size})",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onShowNuevaSemana) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nueva semana")
            }
        }

        if (clientes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PersonOff, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Aún no hay clientes esta semana")
                    Text("Toca + para agregar uno", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier            = Modifier.fillMaxSize()
            ) {
                items(clientes, key = { it.cliente.id }) { cc ->
                    TarjetaCliente(
                        clienteConItems    = cc,
                        onClickEditar      = { onAbrirPedido(cc.cliente.id, cc.cliente.nombre) },
                        onToggleEntregado  = { onToggleEntregado(cc.cliente) },
                        onTogglePagado     = { onTogglePagado(cc.cliente) },
                        onEliminar         = { onEliminarCliente(cc) },
                        onRenombrar        = { n -> onRenombrarCliente(cc.cliente, n) },
                        onActualizarNotas  = { n -> onActualizarNotas(cc.cliente, n) },
                        esNombreDuplicado  = { nuevo ->
                            clientes.any {
                                it.cliente.id != cc.cliente.id &&
                                it.cliente.nombre.equals(nuevo, ignoreCase = true)
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─── TARJETA RESUMEN RÁPIDO ───────────────────────────────────────────────────

@Composable
fun ResumenRapido(clientes: List<ClienteConItems>) {
    val entregados  = clientes.count { it.cliente.entregado }
    val cobrados    = clientes.count { it.cliente.pagado }
    val totalPiezas = clientes.sumOf { c -> c.items.sumOf { it.cantidad } }
    val totalDinero = clientes.sumOf { c -> c.items.sumOf { it.cantidad * it.precioUnitario } }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DatoResumen(
                    icon     = Icons.Default.CheckCircle,
                    valor    = "$entregados/${clientes.size}",
                    etiqueta = "Entregados"
                )
                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color    = MaterialTheme.colorScheme.outlineVariant
                )
                DatoResumen(
                    icon     = Icons.Default.MonetizationOn,
                    valor    = "$cobrados/${clientes.size}",
                    etiqueta = "Cobrados"
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = MaterialTheme.colorScheme.outlineVariant
            )
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DatoResumen(
                    icon     = Icons.Default.BakeryDining,
                    valor    = "$totalPiezas",
                    etiqueta = "Piezas"
                )
                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color    = MaterialTheme.colorScheme.outlineVariant
                )
                DatoResumen(
                    icon     = Icons.Default.Payments,
                    valor    = "$${"%.0f".format(totalDinero)}",
                    etiqueta = "Total"
                )
            }
        }
    }
}

@Composable
fun DatoResumen(icon: androidx.compose.ui.graphics.vector.ImageVector, valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon, null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(Modifier.height(2.dp))
        Text(valor, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(
            etiqueta,
            fontSize = 11.sp,
            color    = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

// ─── TARJETA CLIENTE ─────────────────────────────────────────────────────────

@Composable
fun TarjetaCliente(
    clienteConItems:   ClienteConItems,
    onClickEditar:     () -> Unit,
    onToggleEntregado: () -> Unit,
    onTogglePagado:    () -> Unit,
    onEliminar:        () -> Unit,
    onRenombrar:       (String) -> Unit,
    onActualizarNotas: (String) -> Unit,
    esNombreDuplicado: (String) -> Boolean = { false }
) {
    val cliente    = clienteConItems.cliente
    val items      = clienteConItems.items
    val totalPiezas = items.sumOf { it.cantidad }
    val totalDinero = items.sumOf { it.cantidad * it.precioUnitario }

    var expandido          by remember { mutableStateOf(false) }
    var confirmDelete      by remember { mutableStateOf(false) }
    var showMenu           by remember { mutableStateOf(false) }
    var showRenombrar      by remember { mutableStateOf(false) }
    var showEditarNotas    by remember { mutableStateOf(false) }
    var nuevoNombreEdit    by remember { mutableStateOf("") }
    var nuevasNotasEdit    by remember { mutableStateOf("") }

    val entregado = cliente.entregado
    val pagado    = cliente.pagado

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(if (entregado) 0.dp else 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (entregado)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // ── Fila principal ────────────────────────────────────────
            Row(
                verticalAlignment   = Alignment.CenterVertically,
                modifier            = Modifier.fillMaxWidth()
            ) {
                // Avatar
                Box(
                    modifier         = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (entregado) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        cliente.nombre.first().uppercaseChar().toString(),
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Nombre + stats + notas
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        cliente.nombre,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 16.sp
                    )
                    if (items.isNotEmpty()) {
                        Text(
                            "$totalPiezas piezas · $${"%.0f".format(totalDinero)}",
                            fontSize = 13.sp,
                            color    = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Text(
                            "Sin productos — toca el carrito para agregar",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (cliente.notas.isNotBlank()) {
                        Text(
                            "📝 ${cliente.notas}",
                            fontSize   = 12.sp,
                            color      = MaterialTheme.colorScheme.secondary,
                            fontStyle  = FontStyle.Italic
                        )
                    }
                }

                // Botones de acción
                IconButton(onClick = onToggleEntregado) {
                    Icon(
                        if (entregado) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (entregado) "Marcar como pendiente" else "Marcar como entregado",
                        tint               = if (entregado) MaterialTheme.colorScheme.tertiary
                                             else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onTogglePagado) {
                    Icon(
                        Icons.Default.MonetizationOn,
                        contentDescription = if (pagado) "Marcar como no cobrado" else "Marcar como cobrado",
                        tint               = if (pagado) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onClickEditar) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Editar pedido",
                        tint               = MaterialTheme.colorScheme.primary
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(
                        expanded         = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text         = { Text("Renombrar") },
                            leadingIcon  = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                            onClick      = {
                                showMenu     = false
                                nuevoNombreEdit = cliente.nombre
                                showRenombrar = true
                            }
                        )
                        DropdownMenuItem(
                            text         = { Text("Editar notas") },
                            leadingIcon  = { Icon(Icons.Default.Note, null) },
                            onClick      = {
                                showMenu        = false
                                nuevasNotasEdit = cliente.notas
                                showEditarNotas = true
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text         = {
                                Text(
                                    "Eliminar cliente",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon  = {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick      = { showMenu = false; confirmDelete = true }
                        )
                    }
                }
            }

            // ── Detalle expandible ────────────────────────────────────
            if (items.isNotEmpty()) {
                TextButton(
                    onClick  = { expandido = !expandido },
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (expandido) "Ocultar detalle" else "Ver detalle")
                }

                if (expandido) {
                    Surface(
                        shape  = RoundedCornerShape(10.dp),
                        color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            items.forEach { item ->
                                Row(
                                    modifier              = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        item.variante.ifBlank { item.categoria },
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "×${item.cantidad}",
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── DIALOGS ──────────────────────────────────────────────────────────

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title   = { Text("¿Eliminar cliente?") },
            text    = { Text("Se eliminarán todos los pedidos de ${cliente.nombre}.") },
            confirmButton = {
                Button(
                    onClick = { onEliminar(); confirmDelete = false },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }

    if (showRenombrar) {
        val nombreDuplicado = nuevoNombreEdit.isNotBlank() &&
            esNombreDuplicado(nuevoNombreEdit.trim())
        AlertDialog(
            onDismissRequest = { showRenombrar = false },
            title   = { Text("Renombrar cliente") },
            text    = {
                OutlinedTextField(
                    value          = nuevoNombreEdit,
                    onValueChange  = { nuevoNombreEdit = it },
                    label          = { Text("Nuevo nombre") },
                    isError        = nombreDuplicado,
                    supportingText = if (nombreDuplicado) {
                        { Text("Ya existe otro cliente con este nombre") }
                    } else null,
                    singleLine     = true,
                    modifier       = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick  = {
                        if (nuevoNombreEdit.isNotBlank() && !nombreDuplicado) {
                            onRenombrar(nuevoNombreEdit.trim())
                            showRenombrar = false
                        }
                    },
                    enabled  = nuevoNombreEdit.isNotBlank() && !nombreDuplicado
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showRenombrar = false }) { Text("Cancelar") }
            }
        )
    }

    if (showEditarNotas) {
        AlertDialog(
            onDismissRequest = { showEditarNotas = false },
            title   = { Text("Notas de ${cliente.nombre}") },
            text    = {
                OutlinedTextField(
                    value         = nuevasNotasEdit,
                    onValueChange = { nuevasNotasEdit = it },
                    label         = { Text("Notas") },
                    placeholder   = { Text("Dirección, preferencias, alergias…") },
                    singleLine    = false,
                    maxLines      = 4,
                    modifier      = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onActualizarNotas(nuevasNotasEdit.trim())
                    showEditarNotas = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showEditarNotas = false }) { Text("Cancelar") }
            }
        )
    }
}
