plugins { id("com.android.application") }

// Secrets never come from Gradle properties or the exported endpoint file.
val keystorePath = providers.environmentVariable("ATTESTRA_KEYSTORE_PATH").orNull
    ?.takeIf { it.isNotBlank() }
if (keystorePath != null) {
    require(!gradle.startParameter.isConfigurationCacheRequested) {
        "Registered signing requires --no-configuration-cache to avoid persisting signing credentials"
    }
    val keystore = rootProject.file(keystorePath)
    require(keystore.isFile) { "ATTESTRA_KEYSTORE_PATH must reference an existing keystore" }
    val password = providers.environmentVariable("ATTESTRA_KEYSTORE_PASSWORD").orNull
    require(!password.isNullOrBlank()) { "Set ATTESTRA_KEYSTORE_PASSWORD when ATTESTRA_KEYSTORE_PATH is set" }
    val alias = providers.environmentVariable("ATTESTRA_KEY_ALIAS").orNull
        ?.takeIf { it.isNotBlank() } ?: "attestra"
    val keyPassword = providers.environmentVariable("ATTESTRA_KEY_PASSWORD").orNull
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
