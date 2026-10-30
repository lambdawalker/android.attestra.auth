# Recetas de integración

Ámbito: Desarrollo. Use [inicio rápido](quickstart.md), no una versión Maven supuesta.

## Agregar captura

Reutilice las responsabilidades de [LiveCaptureFlow](../../../app/src/main/java/com/apexfission/android/attestra/auth/LiveCaptureFlow.kt):
retener clientes API/upload, aislar checkpoint por cuenta/backend, suministrar token
solo de esa cuenta, cerrar clientes y conectar `IdentityCamera` con `CaptureHost`.
Lleve `acceptedPolicy` desde Continuar y active `requireReviewedPolicy = true`.
Nunca acepte una política diferente sin conocimiento del usuario.
`allowNewCapture` refleja ajustes vigentes, no autorización del servidor.

Flujo esperado: Continuar → permiso/cámara → revisar frente/reverso → subir → validar
archivos → extracción → formulario poblado/manual → **Details saved** confirmado.
Cancelación, 401, consentimiento obsoleto, evidencia inválida y desconexión tienen
recuperación propia. Reanude fotos existentes sin reabrir cámara innecesariamente.

## Descubrimiento seguro

Use `environmentIndexClient()` con `EnvironmentCache`; cierre el cliente con su
propietario. Resuelva primero el entorno. Consulte
[EnvironmentViewModel](../../../app/src/main/java/com/apexfission/android/attestra/auth/EnvironmentViewModel.kt),
[BackendConfiguration](../../../app/src/main/java/com/apexfission/android/attestra/auth/BackendConfiguration.kt)
y [entornos](../../environment-discovery.md). Nunca use un cliente con bearer para el índice público.

## Personalizar UI

Use pantallas con estado bajo `ui/onboarding` dentro de `AttestraAuthTheme`.
Sus callbacks deben llamar al controlador, no navegar a éxito anticipadamente.
`OnboardingFrame` admite acciones primarias/secundarias y contenido; acciones omitidas
no se muestran. Callbacks de Compose se disparan en UI; lance funciones suspendidas
en el scope correspondiente. El [catálogo](../../../auth/src/main/java/com/apexfission/android/attestra/auth/ui/onboarding/OnboardingCatalog.kt)
es visual. Sus resultados antiguos de identidad son diseños, no resultados live.

## Limpiar datos y diagnosticar

`SecureAuthStorage.clearLocalUser()` solo limpia el dispositivo. No revoca tokens
ni elimina passkeys/evidencia remota. Para evidencia use cancelación autenticada;
no reinicie infraestructura para probar salida de un dispositivo.

Filtre Logcat por `AttestraCapture` y correlacione ID/hora UTC en backend.
`finalizing` precede extracción: investigue validación, almacenamiento y transacciones.
`ready/pending` sí corresponde a extracción. Consulte [diagnóstico](troubleshooting.md).
