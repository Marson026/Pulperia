package com.example.pulperia.controlador

import android.os.Bundle
import android.text.InputType
import android.widget.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pulperia.R
import com.example.pulperia.basedatos.*
import com.example.pulperia.modelo.Producto
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

class InventarioActivity : AppCompatActivity() {
    private lateinit var conexion: ConexionSQLite
    private lateinit var dao: InventarioDAO
    private lateinit var contenido: LinearLayout
    private var pantalla = 0
    private fun dinero(centavos: Long) = "L " + BigDecimal.valueOf(centavos, 2).toPlainString()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actividad_inventario)
        val main = findViewById<View>(R.id.main)
        val margen = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(margen + barras.left, margen + barras.top, margen + barras.right, margen + barras.bottom)
            insets
        }
        conexion = ConexionSQLite(this); dao = InventarioDAO(conexion)
        contenido = findViewById(R.id.contenido)
        pantalla = savedInstanceState?.getInt("pantalla") ?: 0
        listOf(R.id.productos, R.id.movimientos, R.id.resumen).forEachIndexed { index, id ->
            findViewById<Button>(id).setOnClickListener { pantalla = index; mostrar() }
        }
        mostrar()
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putInt("pantalla", pantalla); super.onSaveInstanceState(outState) }
    override fun onDestroy() { conexion.close(); super.onDestroy() }
    private fun texto(parent: LinearLayout, valor: String, grande: Boolean = false) {
        parent.addView(TextView(this).apply {
            text = valor; textSize = if (grande) 21f else 16f
            setPadding(8, 14, 8, 14)
        })
    }
    private fun boton(parent: LinearLayout, titulo: String, accion: () -> Unit) {
        parent.addView(Button(this).apply { text = titulo; setOnClickListener { accion() } })
    }
    private fun tarjeta(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(12, 12, 12, 20)
        contenido.addView(this)
    }
    private fun mostrar() {
        contenido.removeAllViews()
        when (pantalla) {
            0 -> {
                texto(contenido, "Productos", true)
                boton(contenido, "+ Registrar producto") { editar(null) }
                val productos = dao.productos()
                if (productos.isEmpty()) texto(contenido, "Aún no tienes productos. Registra el primero para comenzar.")
                productos.forEach { p ->
                    val fila = tarjeta()
                    texto(fila, p.nombre, true)
                    texto(fila, "${dinero(p.precio)} por unidad · Stock: ${p.stock}\nValor: ${dinero(p.valor)}\nCódigo: ${p.codigo ?: "Sin código"} · Mínimo: ${p.minimo}" +
                        if (p.stockBajo) "\n⚠ Stock mínimo: necesitas reponer este producto." else "")
                    boton(fila, "Editar") { editar(p) }
                    boton(fila, "Eliminar") {
                        AlertDialog.Builder(this).setTitle("Eliminar ${p.nombre}")
                            .setMessage("Se eliminarán el producto y todos sus movimientos. Esta acción no se puede deshacer.")
                            .setNegativeButton("Cancelar", null).setPositiveButton("Eliminar") { _, _ -> dao.eliminar(p.id); mostrar() }.show()
                    }
                }
            }
            1 -> {
                texto(contenido, "Entradas y salidas", true)
                boton(contenido, "+ Registrar movimiento") { movimiento() }
                val lista = dao.movimientos()
                if (lista.isEmpty()) texto(contenido, "No hay movimientos registrados.")
                val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                lista.forEach { m -> texto(tarjeta(), "${m.producto}\n${m.tipo} · ${m.cantidad} unidades\n${formato.format(java.util.Date(m.fecha))}") }
            }
            else -> {
                texto(contenido, "Resumen del inventario", true)
                texto(contenido, "Valor total\n${dinero(dao.total())}", true)
                texto(contenido, "Los 5 productos con mayor valor", true)
                val top = dao.top()
                if (top.isEmpty()) texto(contenido, "Registra productos para ver el resumen.")
                top.forEachIndexed { i, p -> texto(tarjeta(), "${i + 1}. ${p.nombre}\n${dinero(p.precio)} × ${p.stock} = ${dinero(p.valor)}") }
                val bajos = dao.productos().filter { it.stockBajo }
                texto(contenido, "Stock mínimo (${bajos.size})", true)
                if (bajos.isEmpty()) texto(contenido, "No hay productos pendientes de reposición.")
                bajos.forEach { texto(contenido, "⚠ ${it.nombre}: ${it.stock} unidades · Mínimo: ${it.minimo}") }
            }
        }
    }
    private fun formulario() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 12, 32, 12) }
    private fun campo(parent: LinearLayout, etiqueta: String, valor: String, tipo: Int = InputType.TYPE_CLASS_TEXT): EditText {
        texto(parent, etiqueta)
        return EditText(this).apply {
            inputType = tipo; setText(valor); contentDescription = etiqueta; setSingleLine(true)
            parent.addView(this)
        }
    }
    private fun dialogo(titulo: String, form: LinearLayout, guardar: () -> Unit) {
        val scroll = ScrollView(this).apply { addView(form) }
        val error = TextView(this).apply { setTextColor(android.graphics.Color.rgb(180, 30, 30)); accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        form.addView(error)
        val dialog = AlertDialog.Builder(this).setTitle(titulo).setView(scroll)
            .setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try { guardar(); dialog.dismiss(); mostrar() }
                catch (e: android.database.sqlite.SQLiteConstraintException) { error.text = "No se pudo guardar. Verifica que el código de barras no esté repetido." }
                catch (e: IllegalArgumentException) { error.text = e.message ?: "Verifica los datos ingresados" }
                catch (e: IllegalStateException) { error.text = e.message }
            }
        }
        dialog.show()
    }
    private fun entero(campo: EditText, nombre: String): Int {
        val n = campo.text.toString().toIntOrNull()
        require(n != null && n in 0..1000000) { "$nombre debe ser un entero entre 0 y 1000000" }
        return n
    }
    private fun editar(p: Producto?) {
        val form = formulario()
        val nombre = campo(form, "Nombre", p?.nombre ?: "")
        val precio = campo(form, "Precio unitario (L)", p?.let { BigDecimal.valueOf(it.precio, 2).toPlainString() } ?: "", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val stock = campo(form, if (p == null) "Stock inicial" else "Stock (los cambios generan un movimiento)", p?.stock?.toString() ?: "0", InputType.TYPE_CLASS_NUMBER)
        val codigo = campo(form, "Código de barras manual (opcional)", p?.codigo ?: "")
        val minimo = campo(form, "Stock mínimo", p?.minimo?.toString() ?: "5", InputType.TYPE_CLASS_NUMBER)
        dialogo(if (p == null) "Nuevo producto" else "Editar producto", form) {
            val decimal = precio.text.toString().replace(',', '.').toBigDecimalOrNull()
            require(decimal != null && decimal >= BigDecimal.ZERO && decimal <= BigDecimal("1000000") && decimal.stripTrailingZeros().scale() <= 2) { "Ingresa un precio entre 0 y 1000000 con máximo 2 decimales" }
            val producto = Producto(p?.id ?: 0, nombre.text.toString(), decimal.movePointRight(2).longValueExact(), entero(stock, "Stock"), codigo.text.toString(), entero(minimo, "Stock mínimo"))
            dao.guardar(producto)
            avisar(producto)
        }
    }
    private fun movimiento() {
        val productos = dao.productos()
        if (productos.isEmpty()) { Toast.makeText(this, "Primero registra un producto", Toast.LENGTH_LONG).show(); return }
        val form = formulario()
        texto(form, "Producto")
        val producto = Spinner(this).apply {
            adapter = ArrayAdapter(this@InventarioActivity, android.R.layout.simple_spinner_dropdown_item, productos.map { "${it.nombre} · Stock ${it.stock}" }); form.addView(this)
        }
        texto(form, "Tipo de movimiento")
        val tipo = Spinner(this).apply {
            adapter = ArrayAdapter(this@InventarioActivity, android.R.layout.simple_spinner_dropdown_item, listOf("ENTRADA", "SALIDA")); form.addView(this)
        }
        val cantidad = campo(form, "Cantidad de unidades", "", InputType.TYPE_CLASS_NUMBER)
        dialogo("Registrar movimiento", form) {
            val actualizado = dao.mover(productos[producto.selectedItemPosition].id, entero(cantidad, "Cantidad"), tipo.selectedItem.toString())
            avisar(actualizado)
        }
    }
    private fun avisar(p: Producto) {
        if (p.stockBajo) AlertDialog.Builder(this).setTitle("Alerta de stock mínimo")
            .setMessage("${p.nombre}: quedan ${p.stock} unidades. El mínimo definido es ${p.minimo}.")
            .setPositiveButton("Entendido", null).show()
        else Toast.makeText(this, "Guardado correctamente", Toast.LENGTH_SHORT).show()
    }
}
