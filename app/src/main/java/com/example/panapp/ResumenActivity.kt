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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.panapp.database.ItemExtra
import com.example.panapp.database.Producto
import com.example.panapp.database.ResumenItem
import com.example.panapp.ui.UiState
import com.example.panapp.ui.asUiState
import com.example.panapp.ui.theme.PanAppTheme

class ResumenActivity : ComponentActivity() {
    private val vm: PanViewModel by viewModels { PanViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val semanaId = intent.getLongExtra("SEMANA_ID", -1L)

        setContent {
            PanAppTheme {
                ResumenScreen(
                    vm = vm,
                    semanaId = semanaId,
                    onVolver = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenScreen(
    vm: PanViewModel,
    semanaId: Long,
    onVolver: () -> Unit
) {
    val context = LocalContext.current

    val ordenState by remember(semanaId) {
        vm.getOrdenProduccion(semanaId).asUiState { it.consolidado.isEmpty() }
    }.collectAsStateWithLifecycle(UiState.Loading)
    val productos by vm.productos.collectAsStateWithLifecycle(emptyList())

    var showEditorExtra by remember { mutableStateOf(false) }
    val orden = (ordenState as? UiState.Success)?.data

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📋 Orden de Producción", fontWeight = FontWeight.Bold)
                        Text("Total: ${orden?.totalPiezas ?: 0} piezas", fontSize = 13.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { showEditorExtra = true }) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Inventario extra")
                    }
                    if (orden != null) {
                        IconButton(onClick = {
                            compartirResumen(
                                context, orden.consolidado,
                                orden.totalPiezas, orden.totalDinero, orden.piezasExtra
                            )
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->

        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (val estado = ordenState) {
            is UiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is UiState.Error -> Box(contentModifier, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No se pudo cargar la orden")
                    Text(estado.message, color = MaterialTheme.colorScheme.outline)
                }
            }

            is UiState.Empty -> Box(contentModifier, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍞", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Aún no hay nada que producir")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Agrega pedidos o inventario extra",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { showEditorExtra = true },
                        shape   = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar pan extra")
                    }
                }
            }

            is UiState.Success -> OrdenProduccionContenido(
                modifier         = contentModifier,
                orden            = estado.data,
                onEditarExtra    = { showEditorExtra = true },
                onCompartir      = {
                    val o = estado.data
                    compartirResumen(context, o.consolidado, o.totalPiezas, o.totalDinero, o.piezasExtra)
                }
            )
        }
    }

    // ─── EDITOR DE INVENTARIO EXTRA ───────────────────────────────────────────
    if (showEditorExtra) {
        DialogoInventarioExtra(
            productos      = productos,
            extrasActuales = orden?.extras ?: emptyList(),
            onGuardar      = { cantidades ->
                vm.guardarExtras(semanaId, cantidades)
                showEditorExtra = false
            },
            onDismiss      = { showEditorExtra = false }
        )
    }
}

@Composable
private fun OrdenProduccionContenido(
    modifier: Modifier,
    orden: OrdenProduccion,
    onEditarExtra: () -> Unit,
    onCompartir: () -> Unit
) {
    val porCategoria = orden.consolidado.groupBy { it.categoria }
    val extrasPorVariante = remember(orden.extras) {
        orden.extras.associate { (it.categoria to it.variante) to it.cantidad }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Total general con desglose ────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "🎯 TOTAL A PRODUCIR",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 16.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    DesgloseLinea("Pedidos de clientes", "${orden.piezasPedidos} pzas")
                    DesgloseLinea("Inventario extra", "${orden.piezasExtra} pzas")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${orden.totalPiezas} piezas",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 24.sp,
                                color      = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "💰 $${"%.0f".format(orden.totalDinero)}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 15.sp,
                                color      = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // ── Inventario extra ──────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Inventory2, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Inventario extra",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                        TextButton(onClick = onEditarExtra) {
                            Icon(
                                if (orden.extras.isEmpty()) Icons.Default.Add else Icons.Default.Edit,
                                null, modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (orden.extras.isEmpty()) "Agregar" else "Editar")
                        }
                    }
                    if (orden.extras.isEmpty()) {
                        Text(
                            "Sin pan extra para venta en frío",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                        orden.extras.forEach { ex ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    ex.variante.ifBlank { ex.categoria },
                                    fontSize = 14.sp
                                )
                                Text(
                                    "×${ex.cantidad}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Por categoría (consolidado) ───────────────────────────
        porCategoria.forEach { (categoria, items) ->
            val totalCat = items.sumOf { it.totalCantidad }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                categoria,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("$totalCat total", fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        items.sortedByDescending { it.totalCantidad }.forEach { item ->
                            val extraDe = extrasPorVariante[item.categoria to item.variante] ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.variante.ifBlank { item.categoria },
                                        fontSize = 15.sp
                                    )
                                    if (extraDe > 0) {
                                        Text(
                                            "incluye $extraDe extra",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val maxCant = items.maxOf { it.totalCantidad }
                                    val ratio = item.totalCantidad.toFloat() / maxCant
                                    Box(
                                        modifier = Modifier
                                            .width((80 * ratio).dp)
                                            .height(8.dp)
                                            .padding(end = 8.dp)
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { ratio },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Text(
                                        "${item.totalCantidad} pzas",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onCompartir,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Compartir por WhatsApp / Copiar")
            }
        }
    }
}

@Composable
private fun DesgloseLinea(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(etiqueta, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(valor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─── DIÁLOGO EDITOR DE INVENTARIO EXTRA ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoInventarioExtra(
    productos: List<Producto>,
    extrasActuales: List<ItemExtra>,
    onGuardar: (Map<Producto, Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val cantidades = remember {
        mutableStateMapOf<String, Int>().apply {
            extrasActuales.forEach { put("${it.categoria}||${it.variante}", it.cantidad) }
        }
    }

    val totalPiezas = cantidades.values.sum()
    val porCategoria = productos.groupBy { it.categoria }

    fun guardar() {
        val mapa = mutableMapOf<Producto, Int>()
        cantidades.forEach { (key, cant) ->
            val parts = key.split("||")
            val prod = productos.find {
                it.categoria == parts[0] && it.variante == parts.getOrElse(1) { "" }
            }
            if (prod != null && cant > 0) mapa[prod] = cant
        }
        onGuardar(mapa)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("Inventario extra", fontWeight = FontWeight.Bold)
                                Text("$totalPiezas piezas para venta en frío", fontSize = 13.sp)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        actions = {
                            TextButton(onClick = { guardar() }) {
                                Icon(Icons.Default.Save, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Guardar")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                if (productos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        porCategoria.forEach { (categoria, prods) ->
                            item(key = "h_$categoria") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${prods.firstOrNull()?.emoji ?: "🍞"} $categoria",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            prods.forEach { producto ->
                                item(key = producto.id) {
                                    val key = "${producto.categoria}||${producto.variante}"
                                    val cantidad = cantidades[key] ?: 0
                                    FilaProductoExtra(
                                        producto = producto,
                                        cantidad = cantidad,
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

@Composable
private fun FilaProductoExtra(
    producto: Producto,
    cantidad: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${producto.emoji} ${producto.variante.ifBlank { producto.categoria }}",
                fontWeight = if (cantidad > 0) FontWeight.SemiBold else FontWeight.Normal,
                color = if (cantidad > 0) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline
            )
            Text(
                "$${producto.precioUnitario.toInt()} c/u",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = onDecrement,
                enabled = cantidad > 0,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
            }
            Text(
                "$cantidad",
                modifier = Modifier
                    .width(40.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (cantidad > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
            )
            FilledIconButton(
                onClick = onIncrement,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

fun compartirResumen(
    context: android.content.Context,
    items: List<ResumenItem>,
    totalPiezas: Int,
    totalDinero: Double,
    piezasExtra: Int = 0
) {
    val sb = StringBuilder()
    sb.appendLine("🥖 *ORDEN DE PRODUCCIÓN* 🥖")
    sb.appendLine("─────────────────────────")

    items.groupBy { it.categoria }.forEach { (cat, catItems) ->
        val totalCat = catItems.sumOf { it.totalCantidad }
        sb.appendLine("\n*$cat* (${totalCat} pzas)")
        catItems.sortedByDescending { it.totalCantidad }.forEach { item ->
            sb.appendLine("  • ${item.variante.ifBlank { item.categoria }}: ${item.totalCantidad}")
        }
    }

    sb.appendLine("─────────────────────────")
    if (piezasExtra > 0) {
        sb.appendLine("(incluye $piezasExtra extra para venta en frío)")
    }
    sb.appendLine("*TOTAL: $totalPiezas piezas · \$${"%.0f".format(totalDinero)}*")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        setPackage("com.whatsapp")
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(Intent.createChooser(
            intent.also { it.setPackage(null) },
            "Compartir resumen"
        ))
    }
}
