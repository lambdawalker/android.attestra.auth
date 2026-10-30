<!-- Generado a partir de metadatos confirmados de auth. La plantilla canónica es docs/templates/IMPORT.md.template. -->
# Integrar Attestra auth

Solo aparecen versiones confirmadas públicamente. Las etiquetas, compilaciones
locales de verificación y cargas pendientes no prueban que un paquete esté disponible.

## attestra-auth

No hay una versión confirmada.

## Espacio de trabajo desde fuente

Hasta confirmar una versión, clone este repositorio y use su espacio Gradle
existente. La aplicación `:app` usa `implementation(project(":auth"))`.
No trasplante únicamente `auth/`: necesita convenciones raíz, extracción de activos
del modelo mediante `buildSrc` y el catálogo de dependencias. La versión del APK
es independiente.

## Herramientas y dependencias

API 28 mínimo, compile SDK 37 y bytecode Java 17. El daemon de Gradle usa JDK 25.
Conserve las versiones fijadas de Gradle, AGP y Kotlin del repositorio.
Los permisos y el detector usan JitPack, incluso si auth se instala desde Central.
Los activos del modelo se extraen del AAR aislado y fijado del modelo y se incluyen
en auth; no agregue su etiqueta junto al detector como dependencia de ejecución.

Consulte [inicio rápido](agents/quickstart.md), [el catálogo](../../gradle/libs.versions.toml)
y [publicación y configuración](../releases.md).
