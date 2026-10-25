# Pulpería

Aplicación Kotlin con vistas XML y SQLiteOpenHelper. Sigue la separación de Exa1Par3121:

- `modelo`: Producto y Movimiento.
- `basedatos`: ConexionSQLite e InventarioDAO (consultas y transacciones).
- `controlador`: InventarioActivity (Productos, Movimientos y Resumen).

## Uso

Abre esta carpeta en Android Studio, sincroniza Gradle y ejecuta `app` en un dispositivo Android API 24 o superior.

1. En Productos registra nombre, precio en lempiras, stock inicial, código opcional y mínimo (5 por defecto).
2. Edita o elimina productos desde su listado. Eliminar pide confirmación y borra también sus movimientos.
3. En Movimientos selecciona un producto, ENTRADA o SALIDA y una cantidad positiva. No se permiten salidas superiores al stock disponible.
4. En Resumen consulta el valor total, los cinco productos con mayor `precio × stock` y los productos que necesitan reposición.

Los códigos se guardan como texto para conservar ceros iniciales. Los códigos no vacíos son únicos. Las alertas se muestran dentro de la aplicación cuando `stock <= min_stock`.

## Persistencia y cálculos

`products(id, name, price, stock, barcode, min_stock)` y `movements(id, product_id, qty, type, date)` se guardan en `pulperia.db`.
`price` contiene centavos enteros, convertidos a lempiras al mostrar y capturar datos; así se evita el redondeo de punto flotante. `date` contiene milisegundos desde Unix epoch y se presenta en la hora local del teléfono.
El stock inicial positivo y los cambios de stock al editar generan movimientos. Cada operación actualiza stock e historial en una transacción. El historial muestra el nombre actual del producto.
Los límites son L 1,000,000.00 por unidad y 1,000,000 unidades; los precios admiten hasta dos decimales.

## Verificación

`gradlew.bat :app:assembleDebug :app:lintDebug`

Con un emulador o teléfono conectado: `gradlew.bat :app:connectedDebugAndroidTest`.
InventarioTest comprueba persistencia, ajustes, rechazo de salidas excesivas y códigos duplicados, total, top 5 y eliminación relacionada. Utiliza la base del paquete de pruebas.

Prueba manual: registra Arroz a L 12.50, stock 10 y mínimo 5; registra una salida de 5. Debe quedar stock 5, total L 62.50 y aparecer la alerta. Intenta retirar 6: debe rechazarse sin modificar el stock ni el historial.
