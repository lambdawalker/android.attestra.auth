# Integrar Attestra auth desde fuente

**Solo Desarrollo/código fuente. No hay un paquete auth estable confirmado.**
No hay configuración de publicación auth ni catálogo de releases GitHub en esta
auditoría. `versionName = "1.0"` del APK no es versión de la biblioteca. No se
anuncian coordenadas auth de Maven Central o JitPack.

## Espacio de trabajo admitido

Clone el repositorio y compile su espacio Gradle existente. `:app` consume `:auth` así:

```kotlin
implementation(project(":auth"))
```

Use [inicio rápido](agents/quickstart.md) y el
[anfitrión compilado](../../app/src/main/java/com/apexfission/android/attestra/auth/MainActivity.kt).
Copiar `auth/` a otro proyecto requiere adaptar convenciones raíz, tarea de activos
de `buildSrc` y catálogo. Ese trasplante no es una distribución independiente verificada.

## Requisitos

API 28 mínimo, compile SDK 37, bytecode Java 17. Las versiones reales están en
[catálogo](../../gradle/libs.versions.toml), [wrapper](../../gradle/wrapper/gradle-wrapper.properties)
y [build auth](../../auth/build.gradle.kts). Detector y modelo comparten identidad
JitPack; el build extrae activos del modelo de forma aislada/no transitiva. No
instale ambas etiquetas como dependencias runtime en conflicto.

## Versiones futuras

Antes de publicar auth registre destino confirmado, coordenadas exactas, versión,
identidades inmutables y evidencia pública en `sites/versions.json`. Conserve contratos
anteriores. El renderer rechaza snapshots de releases no implementados en lugar de
etiquetar código actual como antiguo. Esta tarea no publica ni implementa finalización de paquetes.
