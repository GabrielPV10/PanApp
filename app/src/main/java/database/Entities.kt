package com.example.panapp.database

import androidx.room.*

// ─── ENTIDADES ───────────────────────────────────────────────────────────────

@Entity(tableName = "semanas")
data class Semana(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fechaInicio: String,
    val etiqueta: String
)

@Entity(tableName = "clientes")
data class Cliente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semanaId: Long,
    val nombre: String,
    val entregado: Boolean = false,
    val notas: String = ""
)

@Entity(tableName = "items_pedido")
data class ItemPedido(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
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
