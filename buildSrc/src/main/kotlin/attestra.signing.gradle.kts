import java.util.Properties

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
    android {
        val registered = signingConfigs.create("registered") {
            storeFile = keystore
            storePassword = password
            keyAlias = alias
            this.keyPassword = keyPassword
        }
        buildTypes.named("debug") { signingConfig = registered }
        buildTypes.named("release") { signingConfig = registered }
    }
}
