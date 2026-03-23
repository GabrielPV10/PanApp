package com.example.panapp.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SemanaDao {
    @Query("SELECT * FROM semanas ORDER BY id DESC")
    fun getAll(): Flow<List<Semana>>

    @Query("SELECT * FROM semanas ORDER BY id DESC LIMIT 1")
    suspend fun getUltima(): Semana?

    @Insert
    suspend fun insert(semana: Semana): Long

    @Delete
    suspend fun delete(semana: Semana)
}

@Dao
interface ClienteDao {
    @Transaction
    @Query("SELECT * FROM clientes WHERE semanaId = :semanaId ORDER BY nombre ASC")
    fun getClientesDeSemana(semanaId: Long): Flow<List<ClienteConItems>>

    @Transaction
    @Query("SELECT * FROM semanas ORDER BY id DESC")
    fun getHistorial(): Flow<List<SemanaConClientes>>

    @Insert
    suspend fun insertCliente(cliente: Cliente): Long

    @Update
    suspend fun updateCliente(cliente: Cliente)

    @Delete
    suspend fun deleteCliente(cliente: Cliente)

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun getById(id: Long): Cliente?
}

@Dao
interface ItemPedidoDao {
    @Query("SELECT * FROM items_pedido WHERE clienteId = :clienteId")
    fun getItemsDeCliente(clienteId: Long): Flow<List<ItemPedido>>

    @Query("DELETE FROM items_pedido WHERE clienteId = :clienteId")
    suspend fun deleteItemsDeCliente(clienteId: Long)

    @Insert
    suspend fun insertAll(items: List<ItemPedido>)

    @Insert
    suspend fun insert(item: ItemPedido)

    // Total de cada variante en una semana (para producción)
    @Query("""
        SELECT ip.categoria, ip.variante, SUM(ip.cantidad) as totalCantidad
        FROM items_pedido ip
        INNER JOIN clientes c ON ip.clienteId = c.id
        WHERE c.semanaId = :semanaId
        GROUP BY ip.categoria, ip.variante
        ORDER BY ip.categoria, ip.variante
    """)
    fun getResumenProduccion(semanaId: Long): Flow<List<ResumenItem>>
}

// Clase auxiliar para el resumen de producción
data class ResumenItem(
    val categoria: String,
    val variante: String,
    val totalCantidad: Int
)
