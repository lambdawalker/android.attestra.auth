# Inicio rápido

Ámbito: Desarrollo/código fuente. Lea [instalación](../IMPORT.md).
La integración mínima verificada es `:app`: incluye App Links, aislamiento de
entornos, actualización al volver al primer plano, almacenamiento y captura.

## Compilar y ejecutar

```sh
git clone https://github.com/lambdawalker/android.attestra.auth.git
cd android.attestra.auth
./gradlew :app:assembleDebug -PattestraEnvironment=qa
./gradlew :app:installDebug -PattestraEnvironment=qa
```

Conecte un dispositivo/emulador API 28 o posterior con depuración USB. Abra Attestra.
**Open onboarding** usa el entorno desplegado; **Open UI catalog** muestra estados
sin backend; **Test ID capture**, en debug, combina cámara real con resultados de
backend simulados. Cámara y Credential Manager requieren pruebas en dispositivo.
No use identificaciones reales para capturas de documentación o pruebas ordinarias.

## Integrar la interfaz

En este espacio Gradle, `:app` depende de `:auth` mediante `implementation(project(":auth"))`.
La siguiente llamada es un fragmento, no una Activity independiente:

```kotlin
EmailOnboardingHost(
    apiBaseUrl = config.apiUrl,
    onboarding = config.onboardingPolicy(),
    onRetryConfiguration = environment::refreshRuntimeConfiguration,
    link = incomingLink,
    onLinkConsumed = ::clearIncomingLink,
    identityContent = { passkeyAdded, policy, exit ->
        AccountIdentityFlow(passkeyAdded, policy, exit)
    },
    onPasskeyDeferred = {
        destination = EntryScreen.Home; clearIncomingLink()
    },
)
```

La implementación completa que CI compila está en
[MainActivity](../../../app/src/main/java/com/apexfission/android/attestra/auth/MainActivity.kt),
[EnvironmentViewModel](../../../app/src/main/java/com/apexfission/android/attestra/auth/EnvironmentViewModel.kt)
y [LiveCaptureFlow](../../../app/src/main/java/com/apexfission/android/attestra/auth/LiveCaptureFlow.kt).
`AccountIdentityFlow` es código del anfitrión de ejemplo, no una API exportada de auth.
Implemente sus responsabilidades equivalentes; no importe componentes internos del ejemplo.
El slot `identityContent` predeterminado muestra indisponibilidad, no una captura funcional.

## Responsabilidades del anfitrión

1. Resuelva el índice confiable y ajustes válidos; vincúlelos al backend/cuenta antes
   de crear clientes. Consulte [entornos](../../environment-discovery.md) y
   [política dinámica](../../runtime-configuration.md).
2. Use `AttestraAuthTheme` y un propietario Compose ligado a una Activity.
   Credential Manager necesita el contexto de Activity; almacenamiento usa el de aplicación.
3. Solo acepte el origen HTTPS `/verify-email` configurado mediante
   `parseVerificationLink(uri, expectedHost)` y consuma cada enlace procesado.
4. Configure manifiesto, paquete y asociación de certificados para App Links.
   Consulte [firma](../../android-signing.md); el índice no configura esa asociación.
5. Gestione salidas y actualización de estado; no finalice la incorporación solo en el cliente.

## Verificar

```sh
./gradlew :auth:testDebugUnitTest :identity-mock:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

Después pruebe correo real, código de respaldo, cancelación de passkey, retomas,
fallo de extracción/entrada manual y guardado/reingreso contra el backend correspondiente.
Compilar no demuestra entrega de correo ni disponibilidad de AWS.
