plugins {
    id("com.android.library")
    alias(libs.plugins.screenshot)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.apexfission.android.attestra.auth.library"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    testOptions {
        unitTests.isReturnDefaultValues = true // JVM tests have no Android Log implementation.

    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.apexfission.permissions)
    implementation(libs.apexfission.card.detector)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)

    // Ktor client & Serialization
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
    screenshotTestImplementation(libs.screenshot.validation.api)
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Resolve the model in isolation: its JitPack artifact ID is shared with the detector.
// Only assets enter this library, never the model AAR's classes or pinned core 0.1.0.
val cardModelAssets by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}
dependencies { cardModelAssets(libs.apexfission.card.model) }
androidComponents.onVariants { variant ->
    val prepareAssets = tasks.register<PrepareCardModelAssets>(
        "prepare${variant.name.replaceFirstChar { it.uppercaseChar() }}CardModelAssets"
    ) {
        modelArchive.set(layout.file(cardModelAssets.elements.map { it.single().asFile }))
    }
    variant.sources.assets?.addGeneratedSourceDirectory(
        prepareAssets, PrepareCardModelAssets::outputDirectory
    )
}
android.androidResources.noCompress += "tflite"
