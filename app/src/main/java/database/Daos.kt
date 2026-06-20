package com.example.panapp.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SemanaDao {
    @Query("SELECT * FROM semanas ORDER BY id DESC")
    fun getAll(): Flow<List<Semana>>

    @Query("SELECT * FROM semanas ORDER BY id DESC LIMIT 1")
    suspend fun getUltima(): Semana?

    @Query("SELECT * FROM semanas WHERE id = :id")
    fun getById(id: Long): Flow<Semana?>

    @Insert
    suspend fun insert(semana: Semana): Long

    @Update
    suspend fun update(semana: Semana)

    @Delete
    suspend fun delete(semana: Semana)
}

@Dao
interface ClienteDao {
    @Transaction
    @Query("SELECT * FROM clientes WHERE semanaId = :semanaId ORDER BY entregado ASC, nombre ASC")
    fun getClientesDeSemana(semanaId: Long): Flow<List<ClienteConItems>>

    @Transaction
    @Query("SELECT * FROM clientes WHERE semanaId = :semanaId ORDER BY nombre ASC")
    suspend fun getClientesDeSemanaOnce(semanaId: Long): List<ClienteConItems>

    @Transaction
    @Query("SELECT * FROM semanas ORDER BY id DESC")
    fun getHistorial(): Flow<List<SemanaConClientes>>

    @Transaction
    @Query("SELECT * FROM semanas WHERE id != :semanaActivaId ORDER BY id DESC")
    fun getHistorialExcluyendo(semanaActivaId: Long): Flow<List<SemanaConClientes>>

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

    // Suma de piezas Y precio por variante en una semana — usa precios históricos almacenados
    @Query("""
        SELECT ip.categoria, ip.variante,
               SUM(ip.cantidad) as totalCantidad,
               SUM(ip.cantidad * ip.precioUnitario) as totalPrecio
        FROM items_pedido ip
        INNER JOIN clientes c ON ip.clienteId = c.id
        WHERE c.semanaId = :semanaId
        GROUP BY ip.categoria, ip.variante
        ORDER BY ip.categoria, ip.variante
    """)
    fun getResumenProduccion(semanaId: Long): Flow<List<ResumenItem>>
}

@Dao
interface ItemExtraDao {
    @Query("SELECT * FROM items_extra WHERE semanaId = :semanaId ORDER BY categoria, variante")
    fun getExtrasDeSemana(semanaId: Long): Flow<List<ItemExtra>>

    @Insert
    suspend fun insertAll(items: List<ItemExtra>)

    @Query("DELETE FROM items_extra WHERE semanaId = :semanaId")
    suspend fun deleteAllDeSemana(semanaId: Long)
}

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos_catalogo ORDER BY orden ASC, categoria ASC, variante ASC")
    fun getAll(): Flow<List<Producto>>

    @Query("SELECT * FROM productos_catalogo ORDER BY orden ASC, categoria ASC, variante ASC")
    suspend fun getAllOnce(): List<Producto>

    @Insert
    suspend fun insert(producto: Producto): Long

    @Update
    suspend fun update(producto: Producto)

    @Delete
    suspend fun delete(producto: Producto)

    @Query("SELECT COUNT(*) FROM productos_catalogo")
    suspend fun count(): Int
}

// Resultado del resumen de producción (incluye precio histórico almacenado)
data class ResumenItem(
    val categoria: String,
    val variante: String,
    val totalCantidad: Int,
    val totalPrecio: Double = 0.0
)
