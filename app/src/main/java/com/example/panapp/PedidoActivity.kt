package com.example.panapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.panapp.ui.theme.PanAppTheme

class PedidoActivity : ComponentActivity() {
    private val vm: PanViewModel by viewModels { PanViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val clienteId = intent.getLongExtra("CLIENTE_ID", -1L)
        val clienteNombre = intent.getStringExtra("CLIENTE_NOMBRE") ?: "Cliente"

        setContent {
            PanAppTheme {
                PedidoScreen(
                    vm = vm,
                    clienteId = clienteId,
                    clienteNombre = clienteNombre,
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
    val itemsExistentes by vm.getItemsDeCliente(clienteId)
        .collectAsStateWithLifecycle(emptyList())

    val cantidades = remember { mutableStateMapOf<String, Int>() }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(itemsExistentes) {
        if (!initialized && itemsExistentes.isNotEmpty()) {
            itemsExistentes.forEach { item ->
                val key = "${item.categoria}||${item.variante}"
                cantidades[key] = item.cantidad
            }
            initialized = true
        } else if (!initialized) {
            initialized = true
        }
    }

    val totalPiezas = cantidades.values.sum()
    val totalDinero = cantidades.entries.sumOf { (key, cant) ->
        val parts = key.split("||")
        val cat = parts[0]
        val vari = if (parts.size > 1) parts[1] else ""
        (Catalogo.find(cat, vari)?.precioUnitario ?: 0.0) * cant
    }

    // Función para guardar — construye el mapa sin nullables
    fun guardar() {
        val mapa = mutableMapOf<ProductoCatalogo, Int>()
        cantidades.forEach { (key, cant) ->
            val parts = key.split("||")
            val cat = parts[0]
            val vari = if (parts.size > 1) parts[1] else ""
            val prod = Catalogo.find(cat, vari)
            if (prod != null && cant > 0) {
                mapa[prod] = cant
            }
        }
        vm.guardarPedido(clienteId, mapa)
        onGuardarYVolver()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pedido de $clienteNombre", fontWeight = FontWeight.Bold)
                        Text("$totalPiezas piezas · $${"%.0f".format(totalDinero)}", fontSize = 13.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { guardar() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { guardar() }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar")
                    }
                }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            Catalogo.categorias.forEach { categoria ->
                item {
                    val totalCategoria = Catalogo.porCategoria(categoria).sumOf { prod ->
                        cantidades["${prod.categoria}||${prod.variante}"] ?: 0
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            categoria,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (totalCategoria > 0) {
                            Badge { Text("$totalCategoria") }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                Catalogo.porCategoria(categoria).forEach { producto ->
                    item {
                        val key = "${producto.categoria}||${producto.variante}"
                        val cantidad = cantidades[key] ?: 0

                        FilaProducto(
                            producto = producto,
                            cantidad = cantidad,
                            onIncrement = { cantidades[key] = cantidad + 1 },
                            onDecrement = {
                                if (cantidad > 1) {
                                    cantidades[key] = cantidad - 1
                                } else {
                                    cantidades.remove(key)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilaProducto(
    producto: ProductoCatalogo,
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
                text = "${producto.emoji} ${producto.variante.ifBlank { producto.categoria }}",
                fontWeight = if (cantidad > 0) FontWeight.SemiBold else FontWeight.Normal,
                color = if (cantidad > 0) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.outline
            )
            Text(
                text = "$${producto.precioUnitario.toInt()} c/u",
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
                text = "$cantidad",
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