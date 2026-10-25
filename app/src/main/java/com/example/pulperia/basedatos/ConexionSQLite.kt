package com.example.pulperia.basedatos

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ConexionSQLite(contexto: Context) : SQLiteOpenHelper(contexto, "pulperia.db", null, 1) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE products (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL CHECK(length(trim(name)) > 0),
            price INTEGER NOT NULL CHECK(price BETWEEN 0 AND 100000000),
            stock INTEGER NOT NULL CHECK(stock BETWEEN 0 AND 1000000),
            barcode TEXT UNIQUE,
            min_stock INTEGER NOT NULL DEFAULT 5 CHECK(min_stock BETWEEN 0 AND 1000000))""")
        db.execSQL("""CREATE TABLE movements (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
            qty INTEGER NOT NULL CHECK(qty > 0),
            type TEXT NOT NULL CHECK(type IN ('ENTRADA','SALIDA')),
            date INTEGER NOT NULL)""")
        db.execSQL("CREATE INDEX movements_product ON movements(product_id)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Se requiere una migración para conservar los datos")
    }
}
