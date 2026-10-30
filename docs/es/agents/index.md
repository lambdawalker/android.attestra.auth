# Autenticación Android de Attestra

**Ámbito: Desarrollo (`main`), integración desde el código fuente; no hay un paquete auth publicado confirmado.**
Use `:auth` para confirmación de correo, acceso con passkey/contraseña, incorporación
regida por políticas y captura de ambas caras de una tarjeta contra el backend Attestra.
`:app` es el anfitrión ejecutable completo. No es un backend, un SDK OAuth genérico,
un servicio de autenticación en segundo plano ni un motor de verificación de identidad.

Lea [instalación](../IMPORT.md) y después [inicio rápido](quickstart.md).


## Recorrido de UI implementada

Estas pantallas están en inglés y usan datos sintéticos; no demuestran éxito de
una cuenta real. Comience con correo, espere extracción y finalice solo cuando
el servidor confirme los datos guardados.

| Inicio | Procesamiento | Guardado |
| --- | --- | --- |
| <img src="../../assets/screenshots/email-start-light.png" alt="Correo; interfaz inglesa clara y datos sintéticos" width="210"> | <img src="../../assets/screenshots/read-document-dark.png" alt="Lectura; interfaz inglesa oscura y datos sintéticos" width="210"> | <img src="../../assets/screenshots/details-saved-light.png" alt="Guardado; interfaz inglesa clara y datos sintéticos" width="210"> |

Consulte [demos y recuperación](demos.md) para acciones y resultados esperados.

En pantallas estrechas, desplace horizontalmente el recorrido de imágenes.

| Tarea | Guía | API preferida |
| --- | --- | --- |
| Incorporar la interfaz de autenticación | [Inicio rápido](quickstart.md) | `EmailOnboardingHost`, `MainActivity` de ejemplo |
| Resolver un entorno seguro | [Conceptos](concepts.md) | `EnvironmentIndex`, `AppConfiguration` |
| Consumir operaciones individuales | [API](api.md) | Adaptadores de correo y acceso; el anfitrión posee los clientes |
| Capturar y revisar una tarjeta | [Recetas](recipes.md) | `CaptureHost`, `CaptureController`, `IdentityCamera` |
| Recuperar errores de procesamiento o guardado | [Diagnóstico](troubleshooting.md) | Estado de captura y registro `reviewed` confirmado |
| Reutilizar UI sin servicios | [Demos](demos.md) | Catálogo y escenarios de captura de pantalla |
| Consultar declaraciones exactas | [Inventario](api-inventory.md) | Firmas y enlaces a fuentes |
| Modificar el repositorio | [Reglas para colaboradores](../../../AGENTS.md) | [Mantenimiento](../../../sites/README.md) |

## Invariantes

- Valide el entorno antes de crear clientes autenticados. Nunca envíe credenciales
  de una cuenta a un nuevo servidor seleccionado sin aislar el estado anterior.
- Confirme correo con A + B o C + B; nunca use B solo ni confunda navegación con autenticación.
- Solo el servidor confirma la finalización. Fotos, texto extraído y datos guardados
  no afirman que la identidad sea válida.
- Retenga los controladores durante su flujo y cierre los clientes que posea.
  `CaptureHost` cierra su controlador al salir de composición; no lo reutilice.
- Los arreglos JPEG se transfieren al controlador. No los reutilice ni registre.
  Tras morir el proceso puede ser necesario volver a tomar fotos no enviadas.
- No incluya contraseñas, tokens, imágenes ni datos personales en registros o capturas.

[Conceptos](concepts.md) · [API](api.md) · [Recetas](recipes.md) ·
[Limitaciones](limitations.md) · [Diagnóstico](troubleshooting.md) ·
[Migración](migration.md) · [Cobertura](coverage.md) · [Evaluaciones](evaluation.md)

## Responsabilidades del sistema

La [entrada central para agentes](https://raw.githubusercontent.com/lambdawalker/design.attestra/main/docs/agents/index.md)
posee requisitos y arquitectura; la [entrada del backend](https://raw.githubusercontent.com/lambdawalker/go.attestra.aws.auth/main/docs/agents/index.md)
posee operaciones. El [contrato de captura](https://raw.githubusercontent.com/lambdawalker/go.attestra.aws.auth/main/docs/id-capture.md)
es la autoridad para HTTP. Los temporizadores de prototipos no demuestran éxito real.
