package com.example.panapp

data class ProductoCatalogo(
    val categoria: String,
    val variante: String,
    val precioUnitario: Double,
    val emoji: String = "🍞"
) {
    val nombreCompleto: String get() =
        if (variante.isBlank()) categoria else "$categoria - $variante"
}

object Catalogo {

    val productos: List<ProductoCatalogo> = listOf(

        // ── DONAS ──────────────────────────────────────────────────────────
        ProductoCatalogo("Dona", "Chocolate",       12.0, "🍩"),
        ProductoCatalogo("Dona", "Rellena crema",   12.0, "🍩"),
        ProductoCatalogo("Dona", "Glaseada",         10.0, "🍩"),
        ProductoCatalogo("Dona", "Azúcar",           10.0, "🍩"),
        ProductoCatalogo("Dona", "Canela",           10.0, "🍩"),

        // ── EMPANADAS ──────────────────────────────────────────────────────
        ProductoCatalogo("Empanada", "Manzana",      15.0, "🥧"),
        ProductoCatalogo("Empanada", "Piña",         15.0, "🥧"),
        ProductoCatalogo("Empanada", "Cajeta",       15.0, "🥧"),
        ProductoCatalogo("Empanada", "Queso",        15.0, "🥧"),
        ProductoCatalogo("Empanada", "Frijol",       14.0, "🥧"),

        // ── PAN DULCE ──────────────────────────────────────────────────────
        ProductoCatalogo("Pan dulce", "Concha vainilla",  10.0, "🍞"),
        ProductoCatalogo("Pan dulce", "Concha chocolate", 10.0, "🍞"),
        ProductoCatalogo("Pan dulce", "Polvorón",          8.0, "🍞"),
        ProductoCatalogo("Pan dulce", "Cuernito",          8.0, "🥐"),
        ProductoCatalogo("Pan dulce", "Oreja",             9.0, "🍞"),

        // ── PAN SALADO ─────────────────────────────────────────────────────
        ProductoCatalogo("Pan salado", "Bolillo",    5.0, "🥖"),
        ProductoCatalogo("Pan salado", "Telera",     5.0, "🥖"),
        ProductoCatalogo("Pan salado", "Baguette",  18.0, "🥖"),

        // ── PASTELES / PORCIONES ───────────────────────────────────────────
        ProductoCatalogo("Pastel", "Porción tres leches", 35.0, "🎂"),
        ProductoCatalogo("Pastel", "Porción chocolate",   35.0, "🎂"),

        // ── GALLETAS ───────────────────────────────────────────────────────
        ProductoCatalogo("Galleta", "Chispas choco",  8.0, "🍪"),
        ProductoCatalogo("Galleta", "Mantequilla",    8.0, "🍪"),
        ProductoCatalogo("Galleta", "Avena",          8.0, "🍪"),

        // ── OTROS ──────────────────────────────────────────────────────────
        ProductoCatalogo("Otro", "Cupcake",      20.0, "🧁"),
        ProductoCatalogo("Otro", "Brownie",      22.0, "🍫"),
        ProductoCatalogo("Otro", "Rollo canela", 25.0, "🌀")
    )

    // Devuelve las categorías únicas en orden
    val categorias: List<String> get() =
        productos.map { it.categoria }.distinct()

    // Devuelve productos de una categoría
    fun porCategoria(cat: String): List<ProductoCatalogo> =
        productos.filter { it.categoria == cat }

    // Busca un producto exacto
    fun find(categoria: String, variante: String): ProductoCatalogo? =
        productos.find { it.categoria == categoria && it.variante == variante }
}