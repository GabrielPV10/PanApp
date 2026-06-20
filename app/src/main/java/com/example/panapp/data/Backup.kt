package com.example.panapp.data

import com.example.panapp.database.Cliente
import com.example.panapp.database.ItemExtra
import com.example.panapp.database.ItemPedido
import com.example.panapp.database.Producto
import com.example.panapp.database.Semana
import org.json.JSONArray
import org.json.JSONObject

/** Instantánea completa de la base de datos para respaldo/restauración. */
data class BackupData(
    val semanas: List<Semana>,
    val clientes: List<Cliente>,
    val items: List<ItemPedido>,
    val extras: List<ItemExtra>,
    val productos: List<Producto>
) {
    val totalRegistros: Int
        get() = semanas.size + clientes.size + items.size + extras.size + productos.size
}

/** Serializa/parsea un [BackupData] a JSON usando org.json (sin dependencias). */
object BackupSerializer {

    const val VERSION = 1

    fun serializar(data: BackupData): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportadoEn", System.currentTimeMillis())

        root.put("semanas", JSONArray().apply {
            data.semanas.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("fechaInicio", s.fechaInicio)
                    put("fechaCierre", s.fechaCierre ?: JSONObject.NULL)
                    put("etiqueta", s.etiqueta)
                })
            }
        })
        root.put("clientes", JSONArray().apply {
            data.clientes.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("semanaId", c.semanaId)
                    put("nombre", c.nombre)
                    put("entregado", c.entregado)
                    put("pagado", c.pagado)
                    put("notas", c.notas)
                })
            }
        })
        root.put("items", JSONArray().apply {
            data.items.forEach { i ->
                put(JSONObject().apply {
                    put("id", i.id)
                    put("clienteId", i.clienteId)
                    put("categoria", i.categoria)
                    put("variante", i.variante)
                    put("cantidad", i.cantidad)
                    put("precioUnitario", i.precioUnitario)
                })
            }
        })
        root.put("extras", JSONArray().apply {
            data.extras.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("semanaId", e.semanaId)
                    put("categoria", e.categoria)
                    put("variante", e.variante)
                    put("cantidad", e.cantidad)
                    put("precioUnitario", e.precioUnitario)
                })
            }
        })
        root.put("productos", JSONArray().apply {
            data.productos.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("categoria", p.categoria)
                    put("variante", p.variante)
                    put("precioUnitario", p.precioUnitario)
                    put("emoji", p.emoji)
                    put("orden", p.orden)
                })
            }
        })
        return root.toString(2)
    }

    /** @throws org.json.JSONException si el archivo no tiene el formato esperado. */
    fun parsear(json: String): BackupData {
        val root = JSONObject(json)
        return BackupData(
            semanas = root.getJSONArray("semanas").mapObjects { o ->
                Semana(
                    id          = o.getLong("id"),
                    fechaInicio = o.getString("fechaInicio"),
                    fechaCierre = if (o.isNull("fechaCierre")) null else o.getString("fechaCierre"),
                    etiqueta    = o.getString("etiqueta")
                )
            },
            clientes = root.getJSONArray("clientes").mapObjects { o ->
                Cliente(
                    id        = o.getLong("id"),
                    semanaId  = o.getLong("semanaId"),
                    nombre    = o.getString("nombre"),
                    entregado = o.getBoolean("entregado"),
                    pagado    = o.optBoolean("pagado", false),
                    notas     = o.optString("notas", "")
                )
            },
            items = root.getJSONArray("items").mapObjects { o ->
                ItemPedido(
                    id             = o.getLong("id"),
                    clienteId      = o.getLong("clienteId"),
                    categoria      = o.getString("categoria"),
                    variante       = o.getString("variante"),
                    cantidad       = o.getInt("cantidad"),
                    precioUnitario = o.getDouble("precioUnitario")
                )
            },
            extras = root.getJSONArray("extras").mapObjects { o ->
                ItemExtra(
                    id             = o.getLong("id"),
                    semanaId       = o.getLong("semanaId"),
                    categoria      = o.getString("categoria"),
                    variante       = o.getString("variante"),
                    cantidad       = o.getInt("cantidad"),
                    precioUnitario = o.getDouble("precioUnitario")
                )
            },
            productos = root.getJSONArray("productos").mapObjects { o ->
                Producto(
                    id             = o.getLong("id"),
                    categoria      = o.getString("categoria"),
                    variante       = o.getString("variante"),
                    precioUnitario = o.getDouble("precioUnitario"),
                    emoji          = o.optString("emoji", "🍞"),
                    orden          = o.getInt("orden")
                )
            }
        )
    }

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }
}
