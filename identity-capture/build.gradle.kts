plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "com.apexfission.android.attestra.identitycapture"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
    implementation(project(":auth"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    testImplementation(libs.junit)
    implementation(libs.apexfission.permissions)
    implementation(libs.apexfission.card.model)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
