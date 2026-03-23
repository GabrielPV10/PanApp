package com.example.panapp

import android.app.Application
import androidx.lifecycle.*
import com.example.panapp.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PanDatabase.getInstance(application)
    private val semanaDao = db.semanaDao()
    private val clienteDao = db.clienteDao()
    private val itemDao = db.itemPedidoDao()

    // ─── SEMANA ACTIVA ───────────────────────────────────────────────────────

    private val _semanaActualId = MutableStateFlow<Long?>(null)
    val semanaActualId: StateFlow<Long?> = _semanaActualId

    init {
        viewModelScope.launch {
            val ultima = semanaDao.getUltima()
            _semanaActualId.value = ultima?.id
        }
    }

    fun crearNuevaSemana() = viewModelScope.launch {
        val hoy = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val semana = Semana(
            fechaInicio = hoy.toString(),
            etiqueta = "Semana del ${hoy.format(fmt)}"
        )
        val id = semanaDao.insert(semana)
        _semanaActualId.value = id
    }

    // ─── CLIENTES ────────────────────────────────────────────────────────────

    fun getClientesDeSemana(semanaId: Long): Flow<List<ClienteConItems>> =
        clienteDao.getClientesDeSemana(semanaId)

    fun agregarCliente(semanaId: Long, nombre: String, notas: String = "") =
        viewModelScope.launch {
            clienteDao.insertCliente(
                Cliente(semanaId = semanaId, nombre = nombre, notas = notas)
            )
        }

    fun toggleEntregado(cliente: Cliente) = viewModelScope.launch {
        clienteDao.updateCliente(cliente.copy(entregado = !cliente.entregado))
    }

    fun eliminarCliente(clienteConItems: ClienteConItems) = viewModelScope.launch {
        itemDao.deleteItemsDeCliente(clienteConItems.cliente.id)
        clienteDao.deleteCliente(clienteConItems.cliente)
    }

    fun actualizarNotas(cliente: Cliente, notas: String) = viewModelScope.launch {
        clienteDao.updateCliente(cliente.copy(notas = notas))
    }

    // ─── ITEMS DE PEDIDO ─────────────────────────────────────────────────────

    fun getItemsDeCliente(clienteId: Long): Flow<List<ItemPedido>> =
        itemDao.getItemsDeCliente(clienteId)

    // Guarda el pedido completo de un cliente (reemplaza todo)
    fun guardarPedido(clienteId: Long, cantidades: Map<ProductoCatalogo, Int>) =
        viewModelScope.launch {
            itemDao.deleteItemsDeCliente(clienteId)
            val items = cantidades
                .filter { it.value > 0 }
                .map { (prod, cant) ->
                    ItemPedido(
                        clienteId = clienteId,
                        categoria = prod.categoria,
                        variante = prod.variante,
                        cantidad = cant,
                        precioUnitario = prod.precioUnitario
                    )
                }
            itemDao.insertAll(items)
        }

    // ─── RESUMEN DE PRODUCCIÓN ───────────────────────────────────────────────

    fun getResumenProduccion(semanaId: Long): Flow<List<ResumenItem>> =
        itemDao.getResumenProduccion(semanaId)

    // ─── HISTORIAL ───────────────────────────────────────────────────────────

    val historial: Flow<List<SemanaConClientes>> = clienteDao.getHistorial()
    val todasLasSemanas: Flow<List<Semana>> = semanaDao.getAll()
}

class PanViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PanViewModel(application) as T
    }
}