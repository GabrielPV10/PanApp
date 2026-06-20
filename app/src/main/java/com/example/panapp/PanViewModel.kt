package com.example.panapp

import android.app.Application
import androidx.lifecycle.*
import com.example.panapp.data.BackupSerializer
import com.example.panapp.data.PanRepository
import com.example.panapp.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PanViewModel(private val repo: PanRepository) : ViewModel() {

    // ─── SEMANA ACTIVA ───────────────────────────────────────────────────────

    private val _semanaActualId = MutableStateFlow<Long?>(null)
    val semanaActualId: StateFlow<Long?> = _semanaActualId

    /** Objeto Semana activa (para mostrar la etiqueta en el header) */
    val semanaActual: Flow<Semana?> = _semanaActualId.flatMapLatest { id ->
        if (id != null) repo.getSemana(id) else flowOf(null)
    }

    init {
        viewModelScope.launch {
            _semanaActualId.value = repo.getUltimaSemana()?.id
        }
    }

    fun crearNuevaSemana(copiarDeSemanaId: Long? = null) = viewModelScope.launch {
        // Cerrar la semana activa antes de crear la nueva
        _semanaActualId.value?.let { id ->
            repo.getSemana(id).first()?.let { semanaAnterior ->
                repo.updateSemana(semanaAnterior.copy(fechaCierre = LocalDate.now().toString()))
            }
        }

        val hoy = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val semana = Semana(
            fechaInicio = hoy.toString(),
            etiqueta    = "Semana del ${hoy.format(fmt)}"
        )
        val id = repo.insertSemana(semana)
        if (copiarDeSemanaId != null) {
            repo.getClientesDeSemanaOnce(copiarDeSemanaId).forEach { cc ->
                repo.insertCliente(
                    Cliente(semanaId = id, nombre = cc.cliente.nombre, notas = cc.cliente.notas)
                )
            }
        }
        _semanaActualId.value = id
    }

    // ─── CLIENTES ────────────────────────────────────────────────────────────

    fun getClientesDeSemana(semanaId: Long): Flow<List<ClienteConItems>> =
        repo.getClientesDeSemana(semanaId)

    fun agregarCliente(semanaId: Long, nombre: String, notas: String = "") =
        viewModelScope.launch {
            repo.insertCliente(Cliente(semanaId = semanaId, nombre = nombre, notas = notas))
        }

    fun toggleEntregado(cliente: Cliente) = viewModelScope.launch {
        repo.updateCliente(cliente.copy(entregado = !cliente.entregado))
    }

    fun togglePagado(cliente: Cliente) = viewModelScope.launch {
        repo.updateCliente(cliente.copy(pagado = !cliente.pagado))
    }

    fun eliminarCliente(clienteConItems: ClienteConItems) = viewModelScope.launch {
        repo.deleteItemsDeCliente(clienteConItems.cliente.id)
        repo.deleteCliente(clienteConItems.cliente)
    }

    fun actualizarNombreCliente(cliente: Cliente, nuevoNombre: String) = viewModelScope.launch {
        repo.updateCliente(cliente.copy(nombre = nuevoNombre))
    }

    fun actualizarNotasCliente(cliente: Cliente, notas: String) = viewModelScope.launch {
        repo.updateCliente(cliente.copy(notas = notas))
    }

    // ─── ITEMS DE PEDIDO ─────────────────────────────────────────────────────

    fun getItemsDeCliente(clienteId: Long): Flow<List<ItemPedido>> =
        repo.getItemsDeCliente(clienteId)

    /** Guarda el pedido completo de un cliente (reemplaza todo) */
    fun guardarPedido(clienteId: Long, cantidades: Map<Producto, Int>) =
        viewModelScope.launch {
            repo.deleteItemsDeCliente(clienteId)
            val items = cantidades
                .filter { it.value > 0 }
                .map { (prod, cant) ->
                    ItemPedido(
                        clienteId      = clienteId,
                        categoria      = prod.categoria,
                        variante       = prod.variante,
                        cantidad       = cant,
                        precioUnitario = prod.precioUnitario
                    )
                }
            repo.insertItems(items)
        }

    // ─── ORDEN DE PRODUCCIÓN (pedidos + inventario extra) ────────────────────

    /**
     * Combina los pedidos de clientes con el inventario extra de la semana en
     * una sola orden de producción consolidada, reactiva a ambas fuentes.
     */
    fun getOrdenProduccion(semanaId: Long): Flow<OrdenProduccion> =
        repo.getResumenProduccion(semanaId)
            .combine(repo.getExtrasDeSemana(semanaId)) { pedidos, extras ->
                val consolidado   = consolidar(pedidos, extras)
                val piezasPedidos = pedidos.sumOf { it.totalCantidad }
                val piezasExtra   = extras.sumOf { it.cantidad }
                OrdenProduccion(
                    consolidado   = consolidado,
                    extras        = extras,
                    piezasPedidos = piezasPedidos,
                    piezasExtra   = piezasExtra,
                    totalPiezas   = piezasPedidos + piezasExtra,
                    totalDinero   = consolidado.sumOf { it.totalPrecio }
                )
            }

    // ─── INVENTARIO EXTRA (venta en frío) ────────────────────────────────────

    /** Guarda el inventario extra de la semana (reemplaza todo) */
    fun guardarExtras(semanaId: Long, cantidades: Map<Producto, Int>) =
        viewModelScope.launch {
            repo.deleteExtrasDeSemana(semanaId)
            val items = cantidades
                .filter { it.value > 0 }
                .map { (prod, cant) ->
                    ItemExtra(
                        semanaId       = semanaId,
                        categoria      = prod.categoria,
                        variante       = prod.variante,
                        cantidad       = cant,
                        precioUnitario = prod.precioUnitario
                    )
                }
            repo.insertExtras(items)
        }

    // ─── HISTORIAL ───────────────────────────────────────────────────────────

    // Excluye la semana activa para que no aparezca duplicada en el historial
    val historial: Flow<List<SemanaConClientes>> = _semanaActualId.flatMapLatest { id ->
        if (id != null) repo.getHistorialExcluyendo(id)
        else repo.getHistorial()
    }

    // ─── CATÁLOGO DE PRODUCTOS ───────────────────────────────────────────────

    val productos: Flow<List<Producto>> = repo.getProductos()

    fun agregarProducto(
        categoria: String,
        variante: String,
        precio: Double,
        emoji: String
    ) = viewModelScope.launch {
        val existentes = repo.getProductosOnce()
        val maxOrden   = existentes.maxOfOrNull { it.orden } ?: 0
        repo.insertProducto(
            Producto(
                categoria      = categoria.trim(),
                variante       = variante.trim(),
                precioUnitario = precio,
                emoji          = emoji.trim().ifBlank { "🍞" },
                orden          = maxOrden + 1
            )
        )
    }

    fun editarProducto(producto: Producto) = viewModelScope.launch {
        repo.updateProducto(producto)
    }

    fun eliminarProducto(producto: Producto) = viewModelScope.launch {
        repo.deleteProducto(producto)
    }

    // ─── RESPALDO ────────────────────────────────────────────────────────────

    /** Genera el JSON del respaldo completo (serialización fuera del hilo UI). */
    suspend fun construirRespaldoJson(): String = withContext(Dispatchers.Default) {
        BackupSerializer.serializar(repo.exportar())
    }

    /** Restaura la base desde el JSON de un respaldo. Lanza excepción si es inválido. */
    suspend fun restaurarRespaldo(json: String) = withContext(Dispatchers.Default) {
        repo.importar(BackupSerializer.parsear(json))
    }
}

class PanViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = PanRepository(PanDatabase.getInstance(application))
        @Suppress("UNCHECKED_CAST")
        return PanViewModel(repo) as T
    }
}

// ─── ORDEN DE PRODUCCIÓN ──────────────────────────────────────────────────────

/** Orden de producción consolidada: pedidos de clientes + inventario extra. */
data class OrdenProduccion(
    val consolidado: List<ResumenItem>,
    val extras: List<ItemExtra>,
    val piezasPedidos: Int,
    val piezasExtra: Int,
    val totalPiezas: Int,
    val totalDinero: Double
)

/** Combina los pedidos de clientes con el inventario extra de la semana. */
private fun consolidar(
    pedidos: List<ResumenItem>,
    extras: List<ItemExtra>
): List<ResumenItem> {
    val mapa = LinkedHashMap<Pair<String, String>, ResumenItem>()
    pedidos.forEach { mapa[it.categoria to it.variante] = it }
    extras.forEach { ex ->
        val key  = ex.categoria to ex.variante
        val prev = mapa[key]
        mapa[key] = if (prev != null) {
            prev.copy(
                totalCantidad = prev.totalCantidad + ex.cantidad,
                totalPrecio   = prev.totalPrecio + ex.cantidad * ex.precioUnitario
            )
        } else {
            ResumenItem(ex.categoria, ex.variante, ex.cantidad, ex.cantidad * ex.precioUnitario)
        }
    }
    return mapa.values.toList()
}
