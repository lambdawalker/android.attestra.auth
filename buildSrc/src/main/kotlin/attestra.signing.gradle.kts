import java.util.Properties
import java.security.KeyStore
import java.security.PrivateKey

plugins { id("com.android.application") }

// Per setting: environment variable > root local.properties > existing default.
// An explicitly empty environment value overrides the file too.
// No signing inputs are read from -P, gradle.properties or the endpoint export.
val localSigning = Properties().apply {
    val contents = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText.orNull
    contents?.reader()?.use { load(it) }
}
fun signingSetting(name: String): String? =
    providers.environmentVariable(name).orNull ?: localSigning.getProperty(name)

val keystorePath = signingSetting("ATTESTRA_KEYSTORE_PATH")
    ?.takeIf { it.isNotBlank() }
if (keystorePath != null) {
    require(!gradle.startParameter.isConfigurationCacheRequested) {
        "Registered signing requires --no-configuration-cache to avoid persisting signing credentials"
    }
    val keystore = rootProject.file(keystorePath)
    require(keystore.isFile) { "ATTESTRA_KEYSTORE_PATH must reference an existing keystore" }
    val password = signingSetting("ATTESTRA_KEYSTORE_PASSWORD")
    require(!password.isNullOrBlank()) { "Set ATTESTRA_KEYSTORE_PASSWORD when ATTESTRA_KEYSTORE_PATH is set" }
    val alias = signingSetting("ATTESTRA_KEY_ALIAS")
        ?.takeIf { it.isNotBlank() } ?: "attestra"
    val keyPassword = signingSetting("ATTESTRA_KEY_PASSWORD")
        ?.takeIf { it.isNotBlank() } ?: password
    // Validate before exposing a variant; never log passwords or provider exceptions.
    val storeChars = password.toCharArray()
    val keyChars = keyPassword.toCharArray()
    try {
        val store = try {
            KeyStore.getInstance(keystore, storeChars)
        } catch (_: Exception) {
            throw GradleException("Cannot open signing keystore; check ATTESTRA_KEYSTORE_PASSWORD and the keystore format")
        }
        require(store.isKeyEntry(alias) && store.getCertificate(alias) != null) {
            "ATTESTRA_KEY_ALIAS must identify a private signing key with a certificate"
        }
        val key = try {
            store.getKey(alias, keyChars)
        } catch (_: Exception) {
            throw GradleException("Cannot unlock signing key; check ATTESTRA_KEY_PASSWORD (defaults to the store password)")
        }
        require(key is PrivateKey) { "ATTESTRA_KEY_ALIAS must identify a private signing key" }
    } finally {
        storeChars.fill('\u0000')
        keyChars.fill('\u0000')
    }
    android {
        val registered = signingConfigs.create("registered") {
            storeFile = keystore
            storePassword = password
            keyAlias = alias
            this.keyPassword = keyPassword
        }
        buildTypes.named("debug") { signingConfig = registered }
        buildTypes.named("release") { signingConfig = registered }
        buildTypes.create("signedDebug") {
            initWith(buildTypes.getByName("debug"))
            signingConfig = registered
            matchingFallbacks += "debug"
        }
        // The app's debug IdentityRuntime supplies the capture mock entry point.
        sourceSets.getByName("signedDebug").java.directories.add(file("src/debug/java").absolutePath)
    }
    androidComponents {
        onVariants(selector().withBuildType("signedDebug")) { variant ->
            variant.sources.java?.addStaticSourceDirectory("src/debug/java")
        }
    }
    // BuildType.initWith does not inherit dependency configurations.
    listOf("Implementation", "CompileOnly", "RuntimeOnly", "AnnotationProcessor").forEach { scope ->
        configurations.named("signedDebug$scope") {
            extendsFrom(configurations.getByName("debug$scope"))
        }
    }
}
