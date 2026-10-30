# Selección y contratos de API

Ámbito: Desarrollo. Las declaraciones exactas están en el [inventario](api-inventory.md).
El [backend](https://raw.githubusercontent.com/lambdawalker/go.attestra.aws.auth/main/docs/agents/index.md)
posee la serialización HTTP. Fuente y pruebas prevalecen sobre prosa.

| API / paquete bajo `com.apexfission.android.attestra.auth` | Uso | Contrato |
| --- | --- | --- |
| `EmailOnboardingHost` · `email` | Autenticación e incorporación interactivas | Activity y Compose, origen confiable, política validada y callbacks de enlace. Posee cliente/almacenamiento/controlador recordados y cierra el cliente. ID predeterminado muestra indisponibilidad. |
| `parseVerificationLink` · `email` | Validar origen URI | HTTPS, host esperado, `/verify-email`, puerto por defecto/443; null si no coincide. El controlador valida además formato de pruebas. |
| `EmailFlowController` · `email` | UI de correo personalizada | Gateway y almacenamiento inyectados; prueba/reloj opcionales. Mutaciones suspendidas serializadas por mutex; guarda sesión solo tras éxito remoto. `screen` es estado Compose. Aceptar una petición no confirma correo. |
| `EmailVerificationApi` / `EmailHttpClient` · `email` | Transporte de correo | El llamador posee el cliente; origen HTTPS; sesión o `EmailApiError`. No reintente confirmación a ciegas sin reconciliación. |
| `ReturnAuthApi` · `passkey` | Acceso por correo/passkey/contraseña, refresh y estado | El llamador posee cliente y sesión. No registre contraseñas/pruebas. Finalización proviene del servidor. |
| `PasskeyRegistrationApi` / `AndroidPasskeyManager` · `passkey` | Passkeys de plataforma | Registro requiere bearer; el gestor necesita Activity y proveedor configurado. `PasskeyCancelled` y `PasskeyUnsupported` no son éxito. Confirme después en backend. |
| `EnvironmentIndex` · `discovery` | Descubrimiento público confiable | Cliente anónimo y caché pertenecen al llamador; claves separan URL de índice/entorno. Fallback limitado y validado. |
| `AppConfiguration` / `OnboardingPolicy` · `discovery` | Requisitos dinámicos | Valide política y entorno; ajustes inválidos bloquean admisión. `next()` usa hechos del servidor; omisiones locales no evitan pasos obligatorios. |
| `CaptureHost` / `CaptureController` · `capture` | Captura y revisión con estado | Retenga por cuenta/backend; anfitrión posee clientes y namespace; UI cierra controlador. `startCaptureOnEntry` y `requireReviewedPolicy` son false por defecto; el ejemplo live usa true y lleva divulgación revisada. |
| `CaptureApi` / `CaptureClients` · `capture` | Estado, mutaciones y PUT privado | Cliente de uploads separado y anónimo; valida origen/cabeceras S3. Bearer debe pertenecer a la cuenta; creador cierra ambos clientes. |
| `IdentityCamera` · `identity.capture` | Permiso y detector | Cámara/dispositivo, activos del modelo y entrada explícita del usuario. Transfiere dos arreglos JPEG; no emite decisión de identidad. |
| `SecureAuthStorage` · `email` | Datos por instalación | Cifrado Keystore; datos ilegibles aparecen ausentes. Limpieza local no es revocación. Excluya preferencias de respaldos. |

## Mutaciones de captura

`continueToCapture()` reanuda antes de crear evidencia. `capturedBoth(front, back)`
recibe propiedad de ambos arreglos; retiene uploads inconclusos solo en memoria.
`finalizeCapture()` persiste intención estable antes de pedir finalización;
`restore()` reconcilia respuestas inciertas. `retry()` solo cubre fallos elegibles;
no evade evidencia inválida/caducada. `cancel()` pide eliminación remota;
`close()` solo libera recursos locales.

`editCard()` cambia borrador local. `saveReview()` exige datos no vacíos, campos
acotados y origen admitido; envía revisión y consume confirmación del servidor.
Conflictos conservan cambios. La pantalla final requiere mismos campos/origen,
no `id_captured`. Diagnóstico solo debe incluir ID de captura, estado, revisión y HTTP;
nunca datos personales ni excepciones completas.

## UI y componentes internos

Las pantallas `ui.onboarding.*` reciben estado/callbacks; no implementan autenticación
por sí solas. `OnboardingBusinessActions` tiene hooks vacíos del catálogo, no de producción.
`IdentityDetails` y resultados antiguos son prototipos, no el modelo HTTP actual.
`CaptureHostContent`, `CardReviewForm`, pantallas de entrada y locales de configuración
son componentes internos; no contratos externos admitidos.

Los modelos de captura serializan transporte/estado. No fabrique registros `ready`
o `reviewed` para completar UI. Trate texto de tarjeta como no confiable; validación
de campos no acredita su veracidad. Consulte [captura](../../identity-capture.md).
