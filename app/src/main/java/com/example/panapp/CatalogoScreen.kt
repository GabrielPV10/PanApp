package com.example.panapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.panapp.database.Producto
import com.example.panapp.ui.InicialChip

// ─── TAB CATÁLOGO ─────────────────────────────────────────────────────────────

@Composable
fun CatalogoTab(
    modifier: Modifier = Modifier,
    productos: List<Producto>,
    // ── Filtro pasado desde MainAppShell ──────────────────────────────────────
    filtro: String = "",
    showFiltro: Boolean = false,
    onFiltroChange: (String) -> Unit = {},
    // ── Callbacks CRUD ────────────────────────────────────────────────────────
    onAgregarProducto: (categoria: String, variante: String, precio: Double, emoji: String) -> Unit,
    onEditarProducto: (Producto) -> Unit,
    onEliminarProducto: (Producto) -> Unit,
    showDialogAgregar: Boolean,
    onDismissDialogAgregar: () -> Unit
) {
    val categoriasOrden    = remember(productos) { productos.map { it.categoria }.distinct() }
    var productoAEditar    by remember { mutableStateOf<Producto?>(null) }
    val focusRequester     = remember { FocusRequester() }

    // Auto-enfocar el campo de filtro cuando se abre
    LaunchedEffect(showFiltro) {
        if (showFiltro) focusRequester.requestFocus()
    }

    // ── Productos filtrados ───────────────────────────────────────────────────
    val productosFiltrados = remember(productos, filtro) {
        if (filtro.isBlank()) productos
        else productos.filter {
            it.variante.contains(filtro, ignoreCase = true) ||
            it.categoria.contains(filtro, ignoreCase = true)
        }
    }
    val porCategoria        = remember(productosFiltrados) { productosFiltrados.groupBy { it.categoria } }
    val categoriasFiltradas = remember(productosFiltrados) { productosFiltrados.map { it.categoria }.distinct() }

    // ── Diálogos ─────────────────────────────────────────────────────────────
    if (showDialogAgregar || productoAEditar != null) {
        DialogoProducto(
            productoExistente    = productoAEditar,
            categoriasExistentes = categoriasOrden,
            onGuardar            = { cat, vari, precio, emoji ->
                val pe = productoAEditar
                if (pe != null) {
                    onEditarProducto(pe.copy(
                        categoria      = cat,
                        variante       = vari,
                        precioUnitario = precio,
                        emoji          = emoji
                    ))
                    productoAEditar = null
                } else {
                    onAgregarProducto(cat, vari, precio, emoji)
                    onDismissDialogAgregar()
                }
            },
            onDismiss = {
                productoAEditar = null
                onDismissDialogAgregar()
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ── Barra de filtro animada ───────────────────────────────────────────
        AnimatedVisibility(
            visible = showFiltro,
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            OutlinedTextField(
                value         = filtro,
                onValueChange = onFiltroChange,
                placeholder   = { Text("Ej. Dona, Chocolate, Pastel…") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = if (filtro.isNotEmpty()) {
                    { IconButton(onClick = { onFiltroChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar filtro")
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

        // ── Chip de resultados cuando hay filtro activo ───────────────────────
        if (filtro.isNotBlank()) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FilterList,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint     = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${productosFiltrados.size} resultado${if (productosFiltrados.size != 1) "s" else ""} para «$filtro»",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ── Contenido principal ───────────────────────────────────────────────
        when {
            productos.isEmpty() -> {
                Box(
                    modifier         = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Storefront, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Catálogo vacío", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Toca ＋ para añadir tu primer producto",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            productosFiltrados.isEmpty() -> {
                Box(
                    modifier         = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff, null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Sin resultados para «$filtro»",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Intenta con otra palabra",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier       = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    categoriasFiltradas.forEach { categoria ->
                        val prods = porCategoria[categoria] ?: return@forEach

                        // ── Cabecera de categoría ──────────────────────────────
                        item(key = "cat_$categoria", contentType = "header_categoria") {
                            Surface(
                                color    = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier          = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    InicialChip(
                                        texto    = categoria,
                                        size     = 32.dp,
                                        fontSize = 14.sp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        categoria,
                                        fontWeight = FontWeight.Bold,
                                        style      = MaterialTheme.typography.titleMedium,
                                        color      = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier   = Modifier.weight(1f)
                                    )
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("${prods.size} producto${if (prods.size != 1) "s" else ""}")
                                    }
                                }
                            }
                        }

                        // ── Productos ──────────────────────────────────────────
                        items(prods, key = { it.id }, contentType = { "producto" }) { prod ->
                            TarjetaProducto(
                                producto   = prod,
                                busqueda   = filtro,
                                onEditar   = { productoAEditar = prod },
                                onEliminar = { onEliminarProducto(prod) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color    = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── TARJETA PRODUCTO ─────────────────────────────────────────────────────────

@Composable
fun TarjetaProducto(
    producto: Producto,
    busqueda: String = "",
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    var confirmEliminar by remember { mutableStateOf(false) }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InicialChip(
            texto    = producto.variante.ifBlank { producto.categoria },
            size     = 36.dp,
            fontSize = 15.sp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                producto.variante.ifBlank { producto.categoria },
                fontSize = 15.sp
            )
            Text(
                "$${producto.precioUnitario.toInt()} por pieza",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.outline
            )
        }
        IconButton(onClick = onEditar) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Editar",
                tint               = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = { confirmEliminar = true }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint               = MaterialTheme.colorScheme.error
            )
        }
    }

    if (confirmEliminar) {
        AlertDialog(
            onDismissRequest = { confirmEliminar = false },
            title  = { Text("¿Eliminar producto?") },
            text   = {
                Text(
                    "Se eliminará '${producto.variante.ifBlank { producto.categoria }}' del catálogo.\n" +
                    "Los pedidos ya guardados no se ven afectados."
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEliminar(); confirmEliminar = false },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEliminar = false }) { Text("Cancelar") }
            }
        )
    }
}

// ─── DIÁLOGO AGREGAR / EDITAR PRODUCTO ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoProducto(
    productoExistente: Producto?,
    categoriasExistentes: List<String>,
    onGuardar: (categoria: String, variante: String, precio: Double, emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    val emoji          = productoExistente?.emoji ?: "🍞"
    var categoria      by remember { mutableStateOf(productoExistente?.categoria  ?: "") }
    var nuevaCategoria by remember { mutableStateOf("") }
    var variante       by remember { mutableStateOf(productoExistente?.variante   ?: "") }
    var precio         by remember {
        mutableStateOf(
            productoExistente?.precioUnitario?.let { "%.0f".format(it) } ?: ""
        )
    }
    var catExpanded    by remember { mutableStateOf(false) }

    val esNueva   = categoria == "__nueva__"
    val catFinal  = if (esNueva) nuevaCategoria.trim() else categoria
    val precioVal = precio.toDoubleOrNull() ?: 0.0
    val valido    = catFinal.isNotBlank() && variante.isNotBlank() && precioVal > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (productoExistente != null) "Editar producto" else "Nuevo producto",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Selector de categoría ──────────────────────────────────────
                ExposedDropdownMenuBox(
                    expanded         = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value         = when {
                            categoria.isBlank() -> ""
                            esNueva             -> "Nueva categoría"
                            else                -> categoria
                        },
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Categoría") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                        modifier      = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded         = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categoriasExistentes.forEach { cat ->
                            DropdownMenuItem(
                                text    = { Text(cat) },
                                onClick = { categoria = cat; catExpanded = false }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text    = {
                                Text(
                                    "+ Nueva categoría",
                                    color      = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            onClick = { categoria = "__nueva__"; catExpanded = false }
                        )
                    }
                }

                // Campo para nueva categoría
                if (esNueva) {
                    OutlinedTextField(
                        value         = nuevaCategoria,
                        onValueChange = { nuevaCategoria = it },
                        label         = { Text("Nombre de la categoría") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }

                // ── Variante / nombre ──────────────────────────────────────────
                OutlinedTextField(
                    value         = variante,
                    onValueChange = { variante = it },
                    label         = { Text("Nombre / variante") },
                    placeholder   = { Text("Ej. Chocolate, Manzana…") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                // ── Precio ────────────────────────────────────────────────────
                OutlinedTextField(
                    value           = precio,
                    onValueChange   = { precio = it.filter { c -> c.isDigit() || c == '.' } },
                    label           = { Text("Precio unitario") },
                    leadingIcon     = { Text("$", modifier = Modifier.padding(start = 12.dp)) },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier        = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (valido) onGuardar(catFinal, variante.trim(), precioVal, emoji) },
                enabled = valido,
                shape   = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(6.dp))
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
