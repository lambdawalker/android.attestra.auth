# Demos ejecutables y evidencia de UI

Ámbito: Desarrollo. Compile [el anfitrión completo](../../../app/src/main/java/com/apexfission/android/attestra/auth/MainActivity.kt)
con [inicio rápido](quickstart.md).

| Entrada | Requisitos y acciones | Resultado / fallo | Fuente |
| --- | --- | --- | --- |
| Open onboarding | Backend/ajustes compatibles; correo, enlace/código, passkey o contraseña permitida | Estado confirmado por servidor; recuperación de cancelaciones/ajustes | `MainActivity`, `EmailOnboardingHost` |
| Open UI catalog | Ejemplo debug/release; seleccione pantallas | Solo UI, sin mutar backend; decisiones antiguas son diseños | `OnboardingCatalog` |
| Test ID capture | Debug y cámara real; tarjeta de muestra, frente/reverso y retoma | Backend simulado, fallos/entrada manual/guardado; sin aprobar identidad | `IdentityRuntime`, `MockCaptureServer` |
| Captura live | Cuenta autenticada, divulgación y política vigentes | Upload privado, procesamiento, manual y guardado confirmado | `LiveCaptureFlow`, `CaptureHost` |
| Capturas de documentación | Renderizador host sin emulador, servicios ni credenciales | Fixtures sintéticos con manifiesto; falla si faltan escenarios | [Mantenimiento](../../screenshots.md) |

Pruebe estados de carga/error/recuperación, ambas caras, retoma, políticas,
fallos de upload/extracción/guardado y reingreso en los demos correspondientes.
Una imagen estática no prueba App Links, foco o permisos reales.

## Ejemplos revisados

![Configuración de correo, interfaz inglesa en tema claro con datos sintéticos](../../assets/screenshots/email-start-light.png)

![Lectura de documento, interfaz inglesa en tema oscuro con datos sintéticos](../../assets/screenshots/read-document-dark.png)

![Detalles guardados, interfaz inglesa en tema claro con datos sintéticos](../../assets/screenshots/details-saved-light.png)

Son componentes implementados con entradas sintéticas, no resultados de cuentas
reales. Consulte el manifiesto para hashes, fixtures y procedencia del renderizador.
