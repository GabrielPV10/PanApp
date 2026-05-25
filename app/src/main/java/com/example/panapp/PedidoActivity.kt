package com.example.panapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.panapp.database.Producto
import com.example.panapp.ui.theme.PanAppTheme
import kotlinx.coroutines.flow.first

class PedidoActivity : ComponentActivity() {
    private val vm: PanViewModel by viewModels { PanViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val clienteId     = intent.getLongExtra("CLIENTE_ID", -1L)
        val clienteNombre = intent.getStringExtra("CLIENTE_NOMBRE") ?: "Cliente"

        setContent {
            PanAppTheme {
                PedidoScreen(
                    vm               = vm,
                    clienteId        = clienteId,
                    clienteNombre    = clienteNombre,
                    onGuardarYVolver = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoScreen(
    vm: PanViewModel,
    clienteId: Long,
    clienteNombre: String,
    onGuardarYVolver: () -> Unit
) {
    val productos by vm.productos.collectAsStateWithLifecycle(emptyList())

    // key = "categoria||variante"
    val cantidades = remember { mutableStateMapOf<String, Int>() }

    // ── FIX: carga inicial UNA sola vez con first() ──────────────────────────
    // collectAsStateWithLifecycle empieza con emptyList() ANTES de que Room
    // responda. Usar first() suspende hasta la primera emisión real de la BD,
    // evitando que el flag "initialized" se active con la lista vacía de carga.
    LaunchedEffect(clienteId) {
        vm.getItemsDeCliente(clienteId).first().forEach { item ->
            cantidades["${item.categoria}||${item.variante}"] = item.cantidad
        }
    }

    // ── Búsqueda ──────────────────────────────────────────────────────────────
    var busqueda     by remember { mutableStateOf("") }
    var showBusqueda by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(showBusqueda) {
        if (showBusqueda) focusRequester.requestFocus()
    }

    // ── Totales ───────────────────────────────────────────────────────────────
    val totalPiezas = cantidades.values.sum()
    val totalDinero = cantidades.entries.sumOf { (key, cant) ->
        val parts  = key.split("||")
        val cat    = parts[0]
        val vari   = parts.getOrElse(1) { "" }
        val precio = productos.find { it.categoria == cat && it.variante == vari }?.precioUnitario ?: 0.0
        precio * cant
    }

    var showConfirmLimpiar by remember { mutableStateOf(false) }

    fun guardar() {
        val mapa = mutableMapOf<Producto, Int>()
        cantidades.forEach { (key, cant) ->
            val parts = key.split("||")
            val cat   = parts[0]
            val vari  = parts.getOrElse(1) { "" }
            val prod  = productos.find { it.categoria == cat && it.variante == vari }
            if (prod != null && cant > 0) mapa[prod] = cant
        }
        vm.guardarPedido(clienteId, mapa)
        onGuardarYVolver()
    }

    // ── Filtrado de productos ─────────────────────────────────────────────────
    val productosFiltrados = remember(productos, busqueda) {
        if (busqueda.isBlank()) productos
        else productos.filter {
            it.variante.contains(busqueda, ignoreCase = true) ||
            it.categoria.contains(busqueda, ignoreCase = true)
        }
    }
    val porCategoria    = productosFiltrados.groupBy { it.categoria }
    val categoriasOrden = productosFiltrados.map { it.categoria }.distinct()

    // ── Scaffold ──────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pedido de $clienteNombre", fontWeight = FontWeight.Bold)
                        Text(
                            "$totalPiezas piezas · $${"%.0f".format(totalDinero)}",
                            fontSize = 13.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { guardar() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Guardar y volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // Lupa / cerrar búsqueda
                    IconButton(onClick = {
                        showBusqueda = !showBusqueda
                        if (!showBusqueda) busqueda = ""
                    }) {
                        Icon(
                            if (showBusqueda) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (showBusqueda) "Cerrar búsqueda" else "Buscar producto"
                        )
                    }
                    // Limpiar y guardar solo cuando no hay búsqueda activa
                    if (!showBusqueda) {
                        if (totalPiezas > 0) {
                            IconButton(onClick = { showConfirmLimpiar = true }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar pedido")
                            }
                        }
                        IconButton(onClick = { guardar() }) {
                            Icon(Icons.Default.Save, contentDescription = "Guardar")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->

        // ── Diálogo limpiar ───────────────────────────────────────────────────
        if (showConfirmLimpiar) {
            AlertDialog(
                onDismissRequest = { showConfirmLimpiar = false },
                title  = { Text("¿Limpiar pedido?") },
                text   = { Text("Se borrarán todas las cantidades de este pedido.") },
                confirmButton = {
                    Button(
                        onClick = { cantidades.clear(); showConfirmLimpiar = false },
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Limpiar") }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmLimpiar = false }) { Text("Cancelar") }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Barra de búsqueda animada ─────────────────────────────────────
            AnimatedVisibility(
                visible = showBusqueda,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                OutlinedTextField(
                    value         = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder   = { Text("Ej. Dona, Chocolate, Bolillo…") },
                    leadingIcon   = { Icon(Icons.Default.Search, null) },
                    trailingIcon  = if (busqueda.isNotEmpty()) {
                        { IconButton(onClick = { busqueda = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                        } }
                    } else null,
                    singleLine    = true,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .focusRequester(focusRequester),
                    shape = MaterialTheme.shapes.large
                )
            }

            // ── Contenido principal ───────────────────────────────────────────
            when {
                productos.isEmpty() -> {
                    Box(
                        modifier         = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                productosFiltrados.isEmpty() -> {
                    Box(
                        modifier         = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 52.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Sin resultados para «$busqueda»",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Intenta con otro término",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier       = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        categoriasOrden.forEach { categoria ->
                            val prodsCategoria = porCategoria[categoria] ?: return@forEach

                            item(key = "header_$categoria") {
                                val totalCategoria = prodsCategoria.sumOf { prod ->
                                    cantidades["${prod.categoria}||${prod.variante}"] ?: 0
                                }
                                Row(
                                    modifier              = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${prodsCategoria.firstOrNull()?.emoji ?: "🍞"} $categoria",
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.primary
                                    )
                                    if (totalCategoria > 0) {
                                        Badge { Text("$totalCategoria") }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }

                            prodsCategoria.forEach { producto ->
                                item(key = producto.id) {
                                    val key      = "${producto.categoria}||${producto.variante}"
                                    val cantidad = cantidades[key] ?: 0
                                    FilaProducto(
                                        producto    = producto,
                                        cantidad    = cantidad,
                                        onIncrement = { cantidades[key] = cantidad + 1 },
                                        onDecrement = {
                                            if (cantidad > 1) cantidades[key] = cantidad - 1
                                            else cantidades.remove(key)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── FILA DE PRODUCTO ─────────────────────────────────────────────────────────

@Composable
fun FilaProducto(
    producto: Producto,
    cantidad: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "${producto.emoji} ${producto.variante.ifBlank { producto.categoria }}",
                fontWeight = if (cantidad > 0) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (cantidad > 0) MaterialTheme.colorScheme.onSurface
                             else MaterialTheme.colorScheme.outline
            )
            Text(
                text     = "$${producto.precioUnitario.toInt()} c/u",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.outline
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick  = onDecrement,
                enabled  = cantidad > 0,
                modifier = Modifier.size(32.dp),
                colors   = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
            }

            Text(
                text     = "$cantidad",
                modifier = Modifier
                    .width(40.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                color      = if (cantidad > 0) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.outline
            )

            FilledIconButton(
                onClick  = onIncrement,
                modifier = Modifier.size(32.dp),
                colors   = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
