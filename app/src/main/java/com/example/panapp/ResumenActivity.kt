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
import com.example.panapp.database.ResumenItem
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
    val resumenItems by vm.getResumenProduccion(semanaId)
        .collectAsStateWithLifecycle(emptyList())

    val totalPiezas = resumenItems.sumOf { it.totalCantidad }
    val totalDinero = resumenItems.sumOf { it.totalPrecio }
    val porCategoria = resumenItems.groupBy { it.categoria }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📋 Resumen de Producción", fontWeight = FontWeight.Bold)
                        Text("Total: $totalPiezas piezas", fontSize = 13.sp)
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
                    IconButton(onClick = {
                        compartirResumen(context, resumenItems, totalPiezas, totalDinero)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir")
                    }
                }
            )
        }
    ) { innerPadding ->

        if (resumenItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍞", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Aún no hay pedidos esta semana")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Total general destacado
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🎯 TOTAL A PRODUCIR",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "$totalPiezas piezas",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "💰 $${"%.0f".format(totalDinero)}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Por categoría
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
                                // Encabezado de categoría
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
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text("$totalCat total", fontSize = 12.sp)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                // Items de la categoría
                                items.sortedByDescending { it.totalCantidad }.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            item.variante.ifBlank { item.categoria },
                                            fontSize = 15.sp
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Barra visual de cantidad
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

                // Botón compartir al final
                item {
                    Button(
                        onClick = { compartirResumen(context, resumenItems, totalPiezas, totalDinero) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Compartir por WhatsApp / Copiar")
                    }
                }
            }
        }
    }
}

fun compartirResumen(
    context: android.content.Context,
    items: List<ResumenItem>,
    totalPiezas: Int,
    totalDinero: Double
) {
    val sb = StringBuilder()
    sb.appendLine("🥖 *PEDIDO DE PRODUCCIÓN* 🥖")
    sb.appendLine("─────────────────────────")

    items.groupBy { it.categoria }.forEach { (cat, catItems) ->
        val totalCat = catItems.sumOf { it.totalCantidad }
        sb.appendLine("\n*$cat* (${totalCat} pzas)")
        catItems.sortedByDescending { it.totalCantidad }.forEach { item ->
            sb.appendLine("  • ${item.variante.ifBlank { item.categoria }}: ${item.totalCantidad}")
        }
    }

    sb.appendLine("─────────────────────────")
    sb.appendLine("*TOTAL: $totalPiezas piezas · \$${"%.0f".format(totalDinero)}*")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        // Intenta abrir directo en WhatsApp; si no está, abre el selector
        setPackage("com.whatsapp")
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // WhatsApp no instalado, abre selector general
        context.startActivity(Intent.createChooser(
            intent.also { it.setPackage(null) },
            "Compartir resumen"
        ))
    }
}
