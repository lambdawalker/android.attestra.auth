# Solución de problemas

Ámbito: Desarrollo. Separe evidencia Android/dispositivo de
[operaciones del backend](https://raw.githubusercontent.com/lambdawalker/go.attestra.aws.auth/main/docs/agents/troubleshooting.md).

| Síntoma | Diagnóstico | Respuesta |
| --- | --- | --- |
| Gradle falla en activos generados | Revise `auth/build.gradle.kts` y AGP | Use el registro de fuentes generadas; no convierta source sets antiguos ni culpe automáticamente a caché |
| Conflicto modelo/detector | Compare catálogo y `cardModelAssets` | Extracción de activos aislada/no transitiva; no agregue dos versiones de la misma identidad JitPack a runtime |
| Enlace abre navegador | Host, paquete, certificado, `assetlinks.json` e identidad instalada | Siga [firma](../../android-signing.md); el índice no asocia App Links |
| Otro dispositivo solicita código | Falta prueba A local | Use B + C; nunca B solo ni copie secretos desde logs |
| Ajustes no disponibles | Índice, binding, `/v1/app-config`, caché y validación | Reintente configuración confiable; no invente política exitosa ni cambie de servidor |
| Estado `finalizing` no avanza | Validación o transacción de evidencia no concluyó | Correlacione `capture.work_*` y `capture.store_swap_failed`; revise S3/DynamoDB antes del modelo |
| `ready/pending` | Extracción en cola/ejecución | Correlacione `capture.inference_*` e intentos; HTTP y etapa distinguen rechazo de respuesta inválida |
| Extracción `failed` | No se pudo leer texto | Formulario manual vacío, no datos parciales de un proveedor fallido |
| Guardado falla/conflicto | Revise `reviewed` y revisión del borrador | Conserve cambios, actualice/reconcilie; no finalice sin confirmación coincidente |
| Requiere retoma tras reiniciar | Bytes solo en memoria | Vuelva a tomar fotos inconclusas; ID/checkpoint no reconstruye imágenes |
| Passkey cancelada/no admitida | `PasskeyCancelled` / `PasskeyUnsupported` | Ofrezca recuperación; no marque registro exitoso |
| Captura de pantalla vacía/faltante | Inventario, estado de fixture y reporte | Genere candidatos e inspeccione; no reemplace baselines durante validación |

## Registros seguros

Etiqueta Android: `AttestraCapture`. El backend correspondiente puede registrar
`capture.dispatch`, `capture.work_started`, `capture.work_state`, `capture.work_finished`,
`capture.extraction_result`, `capture.inference_response`, `capture.inference_failed`
y `capture.store_swap_failed`. Lambda END/REPORT no demuestra éxito: hay fallos
SQS por mensaje. Los registros Android no prueban por sí solos la revisión desplegada.
Nunca registre tokens, contraseñas, secretos de enlaces, JPEG, URLs firmadas,
texto personal, respuestas del proveedor o excepciones que incluyan esos valores.
