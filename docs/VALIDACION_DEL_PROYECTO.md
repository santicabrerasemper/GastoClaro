# Validación del proyecto GastoClaro

## Verificaciones realizadas

- Estructura raíz compatible con importación mediante **File > Open**.
- Scripts `gradlew` y `gradlew.bat` incluidos.
- Android Gradle Plugin, Kotlin, KSP, Hilt y Compose configurados.
- Manifiesto y siete recursos XML validados sintácticamente.
- 55 archivos Kotlin revisados sin marcadores `TODO`, `FIXME` ni implementaciones vacías.
- Utilidades puras de dinero, fechas y resumen mensual compiladas y ejecutadas mediante una prueba de humo.
- Claves foráneas e índices definidos para separar los datos de cada perfil.
- Operaciones de guardado, edición, eliminación, cierre y reapertura implementadas dentro de transacciones Room.
- Meses cerrados configurados como solo lectura desde Dashboard y Movimientos.
- Datos confirmados persistidos mediante Room; perfil activo y mes seleccionado persistidos mediante DataStore.

## Primera compilación

Este entorno de generación no contiene Android SDK ni una caché de dependencias Gradle, por lo que el ensamblado Android completo debe ejecutarse en Android Studio durante la primera sincronización.

Requisitos:

- JDK 17.
- Android SDK 35.
- Conexión a internet en la primera sincronización.

Comandos opcionales desde la raíz:

```bash
./gradlew test
./gradlew assembleDebug
```

En Windows:

```bat
gradlew.bat test
gradlew.bat assembleDebug
```
