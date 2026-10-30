# Limitaciones y límites de seguridad

Ámbito: Desarrollo. No existe paquete estable auth confirmado ni manual histórico.
[Instalación](../IMPORT.md) describe integración desde fuente.

- API 28 mínimo. Credential Manager depende del proveedor/dispositivo y asociación del servidor.
- Se requiere backend Attestra compatible. No es cliente OAuth genérico ni verificador de identidad autónomo.
- Correo confirmado, passkey, evidencia validada, extracción, revisión e identidad son
  hechos distintos. Extracción y datos manuales pueden ser incorrectos; guardar no aprueba identidad.
- Las fotos son sensibles. El ejemplo live usa `FLAG_SECURE`; un anfitrión propio
  debe aplicar privacidad y excluir respaldos. Capturas sintéticas no prueban privacidad real.
- JPEG de uploads pendientes solo vive en memoria. Tras morir el proceso puede
  requerirse retoma; checkpoints no contienen imágenes ni URLs firmadas.
- Fuerza de contraseña es una heurística, no garantía ni comprobación de filtraciones.
  Política admite ASCII imprimible sin espacios; el servidor valida independientemente.
- Configuración pública confiable orienta peticiones, no autentica usuarios. El hash
  no es firma. Aísle estado antes de cambiar backend/cuenta.
- `OnboardingBusinessActions` es un componente vacío de catálogo. Pantallas antiguas
  de decisión de identidad son prototipos, no salidas del flujo real.
- Pruebas y capturas host-side no demuestran foco de cámara, permisos del sistema,
  App Links, restauración de proceso, entrega de correo o permisos AWS desplegados.

## Auditoría de publicación

Antes de esta tarea existían verificación Android, firma y capturas de documentación.
No se publica un paquete auth ni se finalizan versiones en un registro. No invente
etiquetas o esperas de registro para corregir documentación. La construcción del sitio
es independiente y no publica paquetes. Habilitar hosting es tarea del propietario
hasta que se verifique un despliegue real.
