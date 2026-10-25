package com.example.pulperia.modelo

// Precio en centavos: los cálculos monetarios no pierden precisión.
data class Producto(val id: Long = 0, val nombre: String, val precio: Long,
    val stock: Int, val codigo: String?, val minimo: Int) {
    val valor: Long get() = precio * stock
    val stockBajo: Boolean get() = stock <= minimo
}
data class Movimiento(val producto: String, val cantidad: Int, val tipo: String, val fecha: Long)
