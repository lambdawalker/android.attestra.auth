// BEGIN MAVEN PUBLICATION ONLY
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.publish.PublishingExtension

// END MAVEN PUBLICATION ONLY
plugins {
// BEGIN MAVEN PUBLICATION ONLY
    id("com.vanniktech.maven.publish")
// END MAVEN PUBLICATION ONLY
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
// BEGIN MAVEN PUBLICATION ONLY


// Only the auth release variant is a publication. app and identity-mock are local.
val jitpackBuild = providers.gradleProperty("jitpackBuild").orElse("false").map(String::toBoolean)
val projectUrl = "https://github.com/lambdawalker/android.attestra.auth"
val pomLicenseName = providers.gradleProperty("POM_LICENSE_NAME").orElse("")
val pomLicenseUrl = providers.gradleProperty("POM_LICENSE_URL").orElse("")
mavenPublishing {
    coordinates(
        if (jitpackBuild.get()) "com.github.lambdawalker" else providers.gradleProperty("GROUP").get(),
        if (jitpackBuild.get()) "android.attestra.auth" else providers.gradleProperty("POM_ARTIFACT_ID").get(),
        if (jitpackBuild.get()) "auth~v${project.version}" else project.version.toString()
    )
    configure(AndroidSingleVariantLibrary(variant = "release", javadocJar = JavadocJar.Empty(), sourcesJar = SourcesJar.Sources()))
    if (!jitpackBuild.get()) publishToMavenCentral()
    if (!jitpackBuild.get() && providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    pom {
        name.set("Attestra Auth")
        description.set("Android email, passkey and identity-capture onboarding for Attestra.")
        inceptionYear.set("2026")
        url.set(projectUrl)
        licenses { license { name.set(pomLicenseName); url.set(pomLicenseUrl) } }
        developers { developer {
            id.set("lambdawalker")
            name.set("David Garcia")
            url.set("https://github.com/lambdawalker")
        } }
        scm {
            url.set(projectUrl)
            connection.set("scm:git:$projectUrl.git")
            developerConnection.set("scm:git:ssh://git@github.com/lambdawalker/android.attestra.auth.git")
        }
    }
}
tasks.withType<com.vanniktech.maven.publish.tasks.JavadocJar>().configureEach {
    from(rootProject.file("README.md"))
    from(rootProject.file("docs/agents")) { include("**/*.md"); into("guides") }
    from(rootProject.file("LICENSE"))
}
tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
extensions.configure<PublishingExtension> {
    repositories.maven {
        name = "verification"
        url = rootProject.layout.buildDirectory.dir("verification-repository").get().asFile.toURI()
    }
}
val verifyPublicationLicense = tasks.register("verifyPublicationLicense") {
    doLast {
        require(pomLicenseName.get().isNotBlank() && pomLicenseUrl.get().startsWith("https://")) {
            "Owner must set POM_LICENSE_NAME/POM_LICENSE_URL in gradle.properties before publishing; no license is inferred"
        }
    }
}
tasks.configureEach {
    if (name.contains("MavenCentral", ignoreCase = true)) {
        dependsOn(rootProject.tasks.named("verifyPublicationReservation"), verifyPublicationLicense)
    }
    if (this is org.gradle.api.publish.maven.tasks.PublishToMavenLocal) dependsOn(verifyPublicationLicense)
}
// END MAVEN PUBLICATION ONLY
