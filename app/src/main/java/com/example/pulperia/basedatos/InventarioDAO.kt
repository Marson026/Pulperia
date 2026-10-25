package com.example.pulperia.basedatos

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.pulperia.modelo.Producto
import com.example.pulperia.modelo.Movimiento

class InventarioDAO(private val conexion: ConexionSQLite) {
    private fun leer(c: Cursor) = Producto(c.getLong(0), c.getString(1), c.getLong(2), c.getInt(3), if (c.isNull(4)) null else c.getString(4), c.getInt(5))
    fun productos(): List<Producto> = conexion.readableDatabase.rawQuery(
        "SELECT id,name,price,stock,barcode,min_stock FROM products ORDER BY name COLLATE NOCASE", null
    ).use { c -> buildList { while (c.moveToNext()) add(leer(c)) } }
    private fun obtener(db: SQLiteDatabase, id: Long): Producto = db.rawQuery(
        "SELECT id,name,price,stock,barcode,min_stock FROM products WHERE id=?", arrayOf(id.toString())
    ).use { c -> check(c.moveToFirst()) { "El producto ya no existe" }; leer(c) }

    // El stock y su movimiento se confirman juntos o se revierten juntos.
    fun guardar(p: Producto) {
        require(p.nombre.isNotBlank()) { "Escribe el nombre" }
        require(p.precio in 0..100000000L) { "Precio fuera de rango" }
        require(p.stock in 0..1000000 && p.minimo in 0..1000000) { "Stock fuera de rango" }
        val db = conexion.writableDatabase
        db.beginTransaction()
        try {
            val anterior = if (p.id == 0L) 0 else obtener(db, p.id).stock
            val valores = ContentValues().apply {
                put("name", p.nombre.trim()); put("price", p.precio); put("stock", p.stock)
                put("barcode", p.codigo?.trim()?.takeIf { it.isNotEmpty() }); put("min_stock", p.minimo)
            }
            val id = if (p.id == 0L) db.insertOrThrow("products", null, valores) else {
                db.update("products", valores, "id=?", arrayOf(p.id.toString())); p.id
            }
            val diferencia = p.stock - anterior
            if (diferencia != 0) insertarMovimiento(db, id, kotlin.math.abs(diferencia), if (diferencia > 0) "ENTRADA" else "SALIDA")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    private fun insertarMovimiento(db: SQLiteDatabase, id: Long, cantidad: Int, tipo: String) {
        db.insertOrThrow("movements", null, ContentValues().apply {
            put("product_id", id); put("qty", cantidad); put("type", tipo); put("date", System.currentTimeMillis())
        })
    }
    fun mover(id: Long, cantidad: Int, tipo: String): Producto {
        require(cantidad in 1..1000000) { "La cantidad debe estar entre 1 y 1000000" }
        require(tipo == "ENTRADA" || tipo == "SALIDA")
        val db = conexion.writableDatabase
        db.beginTransaction()
        try {
            val p = obtener(db, id)
            val stock = p.stock + if (tipo == "ENTRADA") cantidad else -cantidad
            require(stock >= 0) { "Stock insuficiente. Disponible: ${p.stock}" }
            require(stock <= 1000000) { "El stock máximo es 1000000" }
            db.update("products", ContentValues().apply { put("stock", stock) }, "id=?", arrayOf(id.toString()))
            insertarMovimiento(db, id, cantidad, tipo)
            db.setTransactionSuccessful()
            return p.copy(stock = stock)
        } finally { db.endTransaction() }
    }
    fun eliminar(id: Long) { conexion.writableDatabase.delete("products", "id=?", arrayOf(id.toString())) }
    fun total(): Long = conexion.readableDatabase.rawQuery("SELECT COALESCE(SUM(price * stock),0) FROM products", null)
        .use { it.moveToFirst(); it.getLong(0) }
    fun top(): List<Producto> = conexion.readableDatabase.rawQuery(
        "SELECT id,name,price,stock,barcode,min_stock FROM products ORDER BY price * stock DESC,name COLLATE NOCASE,id LIMIT 5", null
    ).use { c -> buildList { while (c.moveToNext()) add(leer(c)) } }
    fun movimientos(): List<Movimiento> = conexion.readableDatabase.rawQuery(
        "SELECT p.name,m.qty,m.type,m.date FROM movements m JOIN products p ON p.id=m.product_id ORDER BY m.date DESC,m.id DESC", null
    ).use { c -> buildList { while (c.moveToNext()) add(Movimiento(c.getString(0), c.getInt(1), c.getString(2), c.getLong(3))) } }
}
