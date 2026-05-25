package com.example.panapp

import android.app.Application
import androidx.lifecycle.*
import com.example.panapp.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PanViewModel(application: Application) : AndroidViewModel(application) {

    private val db          = PanDatabase.getInstance(application)
    private val semanaDao   = db.semanaDao()
    private val clienteDao  = db.clienteDao()
    private val itemDao     = db.itemPedidoDao()
    private val productoDao = db.productoDao()

    // ─── SEMANA ACTIVA ───────────────────────────────────────────────────────

    private val _semanaActualId = MutableStateFlow<Long?>(null)
    val semanaActualId: StateFlow<Long?> = _semanaActualId

    /** Objeto Semana activa (para mostrar la etiqueta en el header) */
    val semanaActual: Flow<Semana?> = _semanaActualId.flatMapLatest { id ->
        if (id != null) semanaDao.getById(id) else flowOf(null)
    }

    init {
        viewModelScope.launch {
            val ultima = semanaDao.getUltima()
            _semanaActualId.value = ultima?.id
        }
    }

    fun crearNuevaSemana(copiarDeSemanaId: Long? = null) = viewModelScope.launch {
        // Cerrar la semana activa antes de crear la nueva
        _semanaActualId.value?.let { id ->
            semanaDao.getById(id).first()?.let { semanaAnterior ->
                semanaDao.update(semanaAnterior.copy(fechaCierre = LocalDate.now().toString()))
            }
        }

        val hoy = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val semana = Semana(
            fechaInicio = hoy.toString(),
            etiqueta    = "Semana del ${hoy.format(fmt)}"
        )
        val id = semanaDao.insert(semana)
        if (copiarDeSemanaId != null) {
            clienteDao.getClientesDeSemanaOnce(copiarDeSemanaId).forEach { cc ->
                clienteDao.insertCliente(
                    Cliente(semanaId = id, nombre = cc.cliente.nombre, notas = cc.cliente.notas)
                )
            }
        }
        _semanaActualId.value = id
    }

    // ─── CLIENTES ────────────────────────────────────────────────────────────

    fun getClientesDeSemana(semanaId: Long): Flow<List<ClienteConItems>> =
        clienteDao.getClientesDeSemana(semanaId)

    fun agregarCliente(semanaId: Long, nombre: String, notas: String = "") =
        viewModelScope.launch {
            clienteDao.insertCliente(Cliente(semanaId = semanaId, nombre = nombre, notas = notas))
        }

    fun toggleEntregado(cliente: Cliente) = viewModelScope.launch {
        clienteDao.updateCliente(cliente.copy(entregado = !cliente.entregado))
    }

    fun togglePagado(cliente: Cliente) = viewModelScope.launch {
        clienteDao.updateCliente(cliente.copy(pagado = !cliente.pagado))
    }

    fun eliminarCliente(clienteConItems: ClienteConItems) = viewModelScope.launch {
        itemDao.deleteItemsDeCliente(clienteConItems.cliente.id)
        clienteDao.deleteCliente(clienteConItems.cliente)
    }

    fun actualizarNombreCliente(cliente: Cliente, nuevoNombre: String) = viewModelScope.launch {
        clienteDao.updateCliente(cliente.copy(nombre = nuevoNombre))
    }

    fun actualizarNotasCliente(cliente: Cliente, notas: String) = viewModelScope.launch {
        clienteDao.updateCliente(cliente.copy(notas = notas))
    }

    // ─── ITEMS DE PEDIDO ─────────────────────────────────────────────────────

    fun getItemsDeCliente(clienteId: Long): Flow<List<ItemPedido>> =
        itemDao.getItemsDeCliente(clienteId)

    /** Guarda el pedido completo de un cliente (reemplaza todo) */
    fun guardarPedido(clienteId: Long, cantidades: Map<Producto, Int>) =
        viewModelScope.launch {
            itemDao.deleteItemsDeCliente(clienteId)
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
            itemDao.insertAll(items)
        }

    // ─── RESUMEN DE PRODUCCIÓN ───────────────────────────────────────────────

    fun getResumenProduccion(semanaId: Long): Flow<List<ResumenItem>> =
        itemDao.getResumenProduccion(semanaId)

    // ─── HISTORIAL ───────────────────────────────────────────────────────────

    // Excluye la semana activa para que no aparezca duplicada en el historial
    val historial: Flow<List<SemanaConClientes>> = _semanaActualId.flatMapLatest { id ->
        if (id != null) clienteDao.getHistorialExcluyendo(id)
        else clienteDao.getHistorial()
    }

    // ─── CATÁLOGO DE PRODUCTOS ───────────────────────────────────────────────

    val productos: Flow<List<Producto>> = productoDao.getAll()

    fun agregarProducto(
        categoria: String,
        variante: String,
        precio: Double,
        emoji: String
    ) = viewModelScope.launch {
        val existentes = productoDao.getAllOnce()
        val maxOrden   = existentes.maxOfOrNull { it.orden } ?: 0
        productoDao.insert(
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
        productoDao.update(producto)
    }

    fun eliminarProducto(producto: Producto) = viewModelScope.launch {
        productoDao.delete(producto)
    }
}

class PanViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PanViewModel(application) as T
    }
}
