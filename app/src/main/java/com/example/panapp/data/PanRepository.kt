package com.example.panapp.data

import androidx.room.withTransaction
import com.example.panapp.database.*
import kotlinx.coroutines.flow.Flow

/**
 * Única fuente de acceso a datos de la app. Envuelve los DAOs de Room para que
 * el ViewModel no dependa directamente de la base de datos (más testeable y
 * preparado para sumar mañana una fuente remota).
 */
class PanRepository(private val db: PanDatabase) {

    private val semanaDao    = db.semanaDao()
    private val clienteDao   = db.clienteDao()
    private val itemDao      = db.itemPedidoDao()
    private val productoDao  = db.productoDao()
    private val itemExtraDao = db.itemExtraDao()

    // ─── Semanas ──────────────────────────────────────────────────────────────
    suspend fun getUltimaSemana(): Semana?              = semanaDao.getUltima()
    fun getSemana(id: Long): Flow<Semana?>             = semanaDao.getById(id)
    suspend fun insertSemana(semana: Semana): Long      = semanaDao.insert(semana)
    suspend fun updateSemana(semana: Semana)            = semanaDao.update(semana)

    // ─── Clientes ─────────────────────────────────────────────────────────────
    fun getClientesDeSemana(semanaId: Long): Flow<List<ClienteConItems>> =
        clienteDao.getClientesDeSemana(semanaId)

    suspend fun getClientesDeSemanaOnce(semanaId: Long): List<ClienteConItems> =
        clienteDao.getClientesDeSemanaOnce(semanaId)

    fun getHistorial(): Flow<List<SemanaConClientes>> = clienteDao.getHistorial()

    fun getHistorialExcluyendo(semanaActivaId: Long): Flow<List<SemanaConClientes>> =
        clienteDao.getHistorialExcluyendo(semanaActivaId)

    suspend fun insertCliente(cliente: Cliente): Long = clienteDao.insertCliente(cliente)
    suspend fun updateCliente(cliente: Cliente)       = clienteDao.updateCliente(cliente)
    suspend fun deleteCliente(cliente: Cliente)       = clienteDao.deleteCliente(cliente)

    // ─── Items de pedido ────────────────────────────────────────────────────────
    fun getItemsDeCliente(clienteId: Long): Flow<List<ItemPedido>> =
        itemDao.getItemsDeCliente(clienteId)

    suspend fun deleteItemsDeCliente(clienteId: Long) = itemDao.deleteItemsDeCliente(clienteId)
    suspend fun insertItems(items: List<ItemPedido>)  = itemDao.insertAll(items)

    fun getResumenProduccion(semanaId: Long): Flow<List<ResumenItem>> =
        itemDao.getResumenProduccion(semanaId)

    // ─── Inventario extra ───────────────────────────────────────────────────────
    fun getExtrasDeSemana(semanaId: Long): Flow<List<ItemExtra>> =
        itemExtraDao.getExtrasDeSemana(semanaId)

    suspend fun insertExtras(items: List<ItemExtra>)   = itemExtraDao.insertAll(items)
    suspend fun deleteExtrasDeSemana(semanaId: Long)   = itemExtraDao.deleteAllDeSemana(semanaId)

    // ─── Catálogo de productos ──────────────────────────────────────────────────
    fun getProductos(): Flow<List<Producto>>           = productoDao.getAll()
    suspend fun getProductosOnce(): List<Producto>     = productoDao.getAllOnce()
    suspend fun insertProducto(producto: Producto): Long = productoDao.insert(producto)
    suspend fun updateProducto(producto: Producto)     = productoDao.update(producto)
    suspend fun deleteProducto(producto: Producto)     = productoDao.delete(producto)

    // ─── Respaldo / restauración ────────────────────────────────────────────────

    /** Instantánea completa de la base para exportar. */
    suspend fun exportar(): BackupData = BackupData(
        semanas   = semanaDao.getAllOnce(),
        clientes  = clienteDao.getAllOnce(),
        items     = itemDao.getAllOnce(),
        extras    = itemExtraDao.getAllOnce(),
        productos = productoDao.getAllOnce()
    )

    /**
     * Reemplaza toda la base con el contenido del respaldo. Todo dentro de una
     * transacción: si algo falla, no queda la base a medias.
     */
    suspend fun importar(data: BackupData) = db.withTransaction {
        itemDao.deleteAll()
        itemExtraDao.deleteAll()
        clienteDao.deleteAll()
        semanaDao.deleteAll()
        productoDao.deleteAll()

        semanaDao.insertAll(data.semanas)
        clienteDao.insertAll(data.clientes)
        itemDao.insertAll(data.items)
        itemExtraDao.insertAll(data.extras)
        productoDao.insertAll(data.productos)
    }
}
