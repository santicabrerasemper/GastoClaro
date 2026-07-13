# GastoClaro

App Android local para administrar presupuestos mensuales de multiples perfiles.

## Funcionalidades incluidas

- Varios perfiles independientes.
- Presupuesto inicial distinto por perfil y mes.
- Registro de gastos e ingresos.
- Fecha contable editable en cada movimiento.
- Edicion y eliminacion de movimientos.
- Barra de busqueda por nota, categoria, monto exacto o medio de pago.
- Filtros por gasto, ingreso, categoria y medio de pago.
- Medios de pago por perfil: efectivo, debito, cuenta bancaria, billetera virtual y tarjeta de credito.
- Resumen mensual por tarjeta con compras del periodo, cuotas activas, cuotas futuras y total comprometido.
- Gastos en cuotas para tarjetas de credito, con impacto mensual automatico.
- Gastos anualizados para suscripciones o pagos anuales que deben impactar varios meses.
- Dashboard con saldo, totales, grafico de dona y categorias.
- Selector mensual reutilizado en Dashboard, Movimientos, Historial y Medios.
- Cierre automatico de meses anteriores.
- Historial mensual persistente con resumen congelado por categoria.
- Reapertura explicita de un mes para corregirlo y cierre posterior.
- Persistencia con Room y DataStore, incluso al matar la aplicacion.
- Interfaz Jetpack Compose + Material 3.
- Arquitectura MVVM con capas `ui`, `domain` y `data`.
- Inyeccion de dependencias con Hilt.
- WorkManager como respaldo del cierre mensual.

## Requisitos

- Android Studio compatible con Android Gradle Plugin 8.7.
- JDK 17.
- Android SDK 35 instalado.
- Conexion a internet para la primera sincronizacion de Gradle.

## Como abrirlo

1. Descomprimir `GastoClaro.zip`.
2. Abrir Android Studio.
3. Seleccionar **File > Open**.
4. Elegir la carpeta `GastoClaro`, no la carpeta `app`.
5. Confirmar el uso del Gradle Wrapper.
6. Esperar a que termine **Gradle Sync**.
7. Ejecutar en un emulador o dispositivo con Android 8.0 o superior.

La carpeta correcta contiene:

```text
GastoClaro/
|-- settings.gradle.kts
|-- build.gradle.kts
|-- gradlew
|-- gradle/
|-- docs/
`-- app/
```

## Flujo de prueba recomendado

1. Crear un perfil con un monto inicial.
2. Crear un segundo perfil y comprobar que sus datos esten separados.
3. Crear medios de pago: efectivo, debito y una tarjeta de credito.
4. Registrar un gasto comun con categoria, fecha, nota y medio de pago.
5. Registrar un ingreso.
6. Registrar una compra en cuotas con tarjeta de credito y revisar el resumen de tarjeta.
7. Registrar una suscripcion anual usando impacto mensual/anualizado y revisar meses futuros.
8. Abrir **Movimientos** y buscar por nota, categoria, monto o medio de pago.
9. Tocar un movimiento, modificar monto, fecha o medio de pago y guardar.
10. Cambiar el presupuesto base desde el dashboard.
11. Seleccionar un mes anterior y registrar un movimiento para probar periodos.
12. Volver a abrir la app para ejecutar el cierre de meses anteriores.
13. Abrir **Historial**, consultar el resumen y probar la reapertura.
14. Cerrar la app desde recientes y abrirla nuevamente para comprobar persistencia.

## Reglas funcionales importantes

- Las cuotas solo se permiten con medios de pago de tipo tarjeta de credito.
- Un movimiento puede no tener medio de pago.
- Las compras en cuotas impactan desde el mes de la compra hasta completar la cantidad de cuotas.
- Los gastos anualizados impactan el mes de carga y los meses siguientes del periodo configurado.
- El resumen de tarjeta toma el mes seleccionado como referencia y muestra deuda activa/futura.
- Los importes se guardan en centavos como `Long`; no se usa `Double` como fuente de verdad.

## Estructura principal

```text
com.santiago.gastoclaro
|-- core/util
|-- data
|   |-- local
|   |   |-- dao
|   |   |-- entity
|   |   `-- model
|   |-- preferences
|   `-- repository
|-- domain
|   |-- model
|   `-- repository
|-- di
|-- ui
|   |-- components
|   |-- dashboard
|   |-- history
|   |-- movements
|   |-- payments
|   |-- profiles
|   `-- theme
`-- worker
```

## Persistencia

Room guarda perfiles, categorias, presupuestos, movimientos, medios de pago y cierres mensuales. DataStore conserva el perfil activo y el ultimo mes seleccionado. Los ViewModels reconstruyen la UI desde estas fuentes al reiniciar el proceso.

## Validacion tecnica

Compilacion Kotlin verificada con:

```powershell
gradlew.bat :app:compileDebugKotlin --no-daemon
```

La documentacion extendida esta en:

```text
docs/GUIA_APP_GASTOS_MULTIPERFIL_COMPOSE.md
docs/VALIDACION_DEL_PROYECTO.md
```
