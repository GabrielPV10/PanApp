package com.example.panapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Semana::class, Cliente::class, ItemPedido::class, Producto::class, ItemExtra::class],
    version = 6,
    exportSchema = false
)
abstract class PanDatabase : RoomDatabase() {
    abstract fun semanaDao(): SemanaDao
    abstract fun clienteDao(): ClienteDao
    abstract fun itemPedidoDao(): ItemPedidoDao
    abstract fun productoDao(): ProductoDao
    abstract fun itemExtraDao(): ItemExtraDao

    companion object {
        @Volatile private var INSTANCE: PanDatabase? = null

        /** Migración 1 → 2: crea la tabla del catálogo de productos */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `productos_catalogo` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoria` TEXT NOT NULL,
                        `variante` TEXT NOT NULL,
                        `precioUnitario` REAL NOT NULL,
                        `emoji` TEXT NOT NULL DEFAULT '🍞',
                        `orden` INTEGER NOT NULL DEFAULT 0
                    )"""
                )
            }
        }

        /** Migración 2 → 3: agrega fecha de cierre a semanas */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE semanas ADD COLUMN fechaCierre TEXT")
            }
        }

        /** Migración 3 → 4: agrega campo de cobro independiente al cliente */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE clientes ADD COLUMN pagado INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** Migración 4 → 5: crea la tabla de inventario extra por semana */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `items_extra` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `semanaId` INTEGER NOT NULL,
                        `categoria` TEXT NOT NULL,
                        `variante` TEXT NOT NULL,
                        `cantidad` INTEGER NOT NULL,
                        `precioUnitario` REAL NOT NULL
                    )"""
                )
            }
        }

        /**
         * Migración 5 → 6: índices en las claves foráneas más consultadas para
         * acelerar los filtros por semana y por cliente. Los nombres siguen la
         * convención de Room (index_<tabla>_<columna>) para pasar la validación.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_clientes_semanaId` ON `clientes` (`semanaId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_items_pedido_clienteId` ON `items_pedido` (`clienteId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_items_extra_semanaId` ON `items_extra` (`semanaId`)"
                )
            }
        }

        fun getInstance(context: Context): PanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PanDatabase::class.java,
                    "pan_database"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6
                    )
                    .build()
                    .also { db ->
                        INSTANCE = db
                        // Poblar catálogo en primera instalación/migración
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = db.productoDao()
                            if (dao.count() == 0) {
                                catalogoSemilla.forEach { dao.insert(it) }
                            }
                        }
                    }
            }
        }

        // ── Datos semilla del catálogo ────────────────────────────────────
        private val catalogoSemilla = listOf(
            Producto(categoria = "Dona",      variante = "Chocolate",          precioUnitario = 12.0, emoji = "🍩", orden = 10),
            Producto(categoria = "Dona",      variante = "Rellena crema",      precioUnitario = 12.0, emoji = "🍩", orden = 11),
            Producto(categoria = "Dona",      variante = "Glaseada",           precioUnitario = 10.0, emoji = "🍩", orden = 12),
            Producto(categoria = "Dona",      variante = "Azúcar",             precioUnitario = 10.0, emoji = "🍩", orden = 13),
            Producto(categoria = "Dona",      variante = "Canela",             precioUnitario = 10.0, emoji = "🍩", orden = 14),
            Producto(categoria = "Empanada",  variante = "Manzana",            precioUnitario = 15.0, emoji = "🥧", orden = 20),
            Producto(categoria = "Empanada",  variante = "Piña",               precioUnitario = 15.0, emoji = "🥧", orden = 21),
            Producto(categoria = "Empanada",  variante = "Cajeta",             precioUnitario = 15.0, emoji = "🥧", orden = 22),
            Producto(categoria = "Empanada",  variante = "Queso",              precioUnitario = 15.0, emoji = "🥧", orden = 23),
            Producto(categoria = "Empanada",  variante = "Frijol",             precioUnitario = 14.0, emoji = "🥧", orden = 24),
            Producto(categoria = "Pan dulce", variante = "Concha vainilla",    precioUnitario = 10.0, emoji = "🍞", orden = 30),
            Producto(categoria = "Pan dulce", variante = "Concha chocolate",   precioUnitario = 10.0, emoji = "🍞", orden = 31),
            Producto(categoria = "Pan dulce", variante = "Polvorón",           precioUnitario = 8.0,  emoji = "🍞", orden = 32),
            Producto(categoria = "Pan dulce", variante = "Cuernito",           precioUnitario = 8.0,  emoji = "🥐", orden = 33),
            Producto(categoria = "Pan dulce", variante = "Oreja",              precioUnitario = 9.0,  emoji = "🍞", orden = 34),
            Producto(categoria = "Pan salado",variante = "Bolillo",            precioUnitario = 5.0,  emoji = "🥖", orden = 40),
            Producto(categoria = "Pan salado",variante = "Telera",             precioUnitario = 5.0,  emoji = "🥖", orden = 41),
            Producto(categoria = "Pan salado",variante = "Baguette",           precioUnitario = 18.0, emoji = "🥖", orden = 42),
            Producto(categoria = "Pastel",    variante = "Porción tres leches",precioUnitario = 35.0, emoji = "🎂", orden = 50),
            Producto(categoria = "Pastel",    variante = "Porción chocolate",  precioUnitario = 35.0, emoji = "🎂", orden = 51),
            Producto(categoria = "Galleta",   variante = "Chispas choco",      precioUnitario = 8.0,  emoji = "🍪", orden = 60),
            Producto(categoria = "Galleta",   variante = "Mantequilla",        precioUnitario = 8.0,  emoji = "🍪", orden = 61),
            Producto(categoria = "Galleta",   variante = "Avena",              precioUnitario = 8.0,  emoji = "🍪", orden = 62),
            Producto(categoria = "Otro",      variante = "Cupcake",            precioUnitario = 20.0, emoji = "🧁", orden = 70),
            Producto(categoria = "Otro",      variante = "Brownie",            precioUnitario = 22.0, emoji = "🍫", orden = 71),
            Producto(categoria = "Otro",      variante = "Rollo canela",       precioUnitario = 25.0, emoji = "🌀", orden = 72),
        )
    }
}
