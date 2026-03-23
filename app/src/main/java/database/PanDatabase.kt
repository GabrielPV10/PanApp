package com.example.panapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Semana::class, Cliente::class, ItemPedido::class],
    version = 1,
    exportSchema = false
)
abstract class PanDatabase : RoomDatabase() {
    abstract fun semanaDao(): SemanaDao
    abstract fun clienteDao(): ClienteDao
    abstract fun itemPedidoDao(): ItemPedidoDao

    companion object {
        @Volatile private var INSTANCE: PanDatabase? = null

        fun getInstance(context: Context): PanDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PanDatabase::class.java,
                    "pan_database"
                ).build().also { INSTANCE = it }
            }
    }
}
