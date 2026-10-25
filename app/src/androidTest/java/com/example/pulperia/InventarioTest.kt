package com.example.pulperia

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pulperia.basedatos.ConexionSQLite
import com.example.pulperia.basedatos.InventarioDAO
import com.example.pulperia.modelo.Producto
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventarioTest {
    @Test fun inventarioCompletoYTransacciones() {
        // La base se crea en el paquete de pruebas, separada de los datos del usuario.
        val context = InstrumentationRegistry.getInstrumentation().context
        context.deleteDatabase("pulperia.db")
        val conexion = ConexionSQLite(context)
        val dao = InventarioDAO(conexion)
        try {
            assertEquals(0L, dao.total())
            dao.guardar(Producto(nombre = "Arroz", precio = 1250, stock = 10, codigo = "00123", minimo = 5))
            val arroz = dao.productos().single()
            assertEquals(12500L, dao.total())
            assertEquals(1, dao.movimientos().size)
            assertTrue(dao.mover(arroz.id, 5, "SALIDA").stockBajo)
            val movimientosAntes = dao.movimientos().size
            try { dao.mover(arroz.id, 6, "SALIDA"); fail("Debe rechazar stock insuficiente") }
            catch (_: IllegalArgumentException) { }
            assertEquals(5, dao.productos().single().stock)
            assertEquals(movimientosAntes, dao.movimientos().size)
            try { dao.guardar(arroz.copy(id = 0, nombre = "Duplicado")); fail("Debe rechazar código duplicado") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertEquals(1, dao.productos().size)
            dao.guardar(arroz.copy(stock = 8))
            assertEquals(3, dao.movimientos().first().cantidad)
            for (i in 1..6) dao.guardar(Producto(nombre = "Producto $i", precio = i * 10000L, stock = 1, codigo = null, minimo = 0))
            assertEquals(5, dao.top().size)
            assertEquals("Producto 6", dao.top().first().nombre)
            assertEquals(220000L, dao.total())
            conexion.close()
            val reabierta = ConexionSQLite(context)
            try { assertEquals(7, InventarioDAO(reabierta).productos().size) } finally { reabierta.close() }
            dao.eliminar(arroz.id)
            assertFalse(dao.movimientos().any { it.producto == "Arroz" })
        } finally { conexion.close(); context.deleteDatabase("pulperia.db") }
    }
}
