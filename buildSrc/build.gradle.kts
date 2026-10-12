plugins { `kotlin-dsl` }

dependencies {
    implementation(libs.android.gradle.plugin)
    // Keep AGP's runtime Kotlin plugin aligned with the application catalog.
    // Compile buildSrc itself with Gradle's embedded Kotlin DSL compiler.
    implementation(libs.kotlin.gradle.plugin)
}
