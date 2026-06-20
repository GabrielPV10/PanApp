package com.example.panapp.database

import androidx.room.*

// ─── ENTIDADES ───────────────────────────────────────────────────────────────

@Entity(tableName = "semanas")
data class Semana(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fechaInicio: String,
    val fechaCierre: String? = null,
    val etiqueta: String
)

@Entity(
    tableName = "clientes",
    indices = [Index("semanaId")]
)
data class Cliente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semanaId: Long,
    val nombre: String,
    val entregado: Boolean = false,
    val pagado: Boolean = false,
    val notas: String = ""
)

@Entity(
    tableName = "items_pedido",
    indices = [Index("clienteId")]
)
data class ItemPedido(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
    val categoria: String,
    val variante: String,
    val cantidad: Int,
    val precioUnitario: Double
)

/**
 * Inventario extra de una semana: pan para venta en frío/espontánea.
 * No pertenece a ningún cliente; se suma a la orden de producción.
 */
@Entity(
    tableName = "items_extra",
    indices = [Index("semanaId")]
)
data class ItemExtra(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semanaId: Long,
    val categoria: String,
    val variante: String,
    val cantidad: Int,
    val precioUnitario: Double
)

/** Producto del catálogo — editable por el usuario */
@Entity(tableName = "productos_catalogo")
data class Producto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoria: String,
    val variante: String,
    val precioUnitario: Double,
    val emoji: String = "🍞",
    val orden: Int = 0
)

// ─── RELACIONES ──────────────────────────────────────────────────────────────

data class ClienteConItems(
    @Embedded val cliente: Cliente,
    @Relation(parentColumn = "id", entityColumn = "clienteId")
    val items: List<ItemPedido>
)

data class SemanaConClientes(
    @Embedded val semana: Semana,
    @Relation(
        parentColumn = "id",
        entityColumn = "semanaId",
        entity = Cliente::class
    )
    val clientes: List<ClienteConItems>
)
