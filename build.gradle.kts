// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
// BEGIN MAVEN PUBLICATION ONLY
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
// END MAVEN PUBLICATION ONLY
    id("com.android.application") apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.android.library") apply false
}
// BEGIN MAVEN PUBLICATION ONLY

val releaseModule = providers.gradleProperty("releaseModule").orElse("")
val releaseVersion = providers.gradleProperty("releaseVersion").orElse("0.0.0-SNAPSHOT")
val jitpackBuild = providers.gradleProperty("jitpackBuild").orElse("false").map(String::toBoolean)
require(releaseModule.get() in listOf("", "auth")) { "Only auth may be published" }
if (releaseModule.get().isNotBlank()) {
    require(releaseVersion.get().matches(Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"))) { "Expected stable releaseVersion X.Y.Z" }
}
subprojects {
    group = rootProject.providers.gradleProperty("GROUP").get()
    version = if (name == "auth") releaseVersion.get() else "0.0.0-SNAPSHOT"
}
gradle.taskGraph.whenReady {
    allTasks.filter { it is org.gradle.api.publish.maven.tasks.PublishToMavenLocal || it is org.gradle.api.publish.maven.tasks.PublishToMavenRepository }.forEach {
        require(it.project.name == "auth") { "Only auth may publish" }
        if (jitpackBuild.get()) {
            require(it is org.gradle.api.publish.maven.tasks.PublishToMavenLocal ||
                (it is org.gradle.api.publish.maven.tasks.PublishToMavenRepository && it.repository.name == "verification")) { "JitPack may publish only locally" }
        }
    }
}
tasks.register<Exec>("verifyPublicationReservation") {
    workingDir(rootDir)
    commandLine("python3", "scripts/module_release.py", "guard", "--module", "auth", "--version", releaseVersion.get())
    doFirst {
        require(!jitpackBuild.get() && releaseModule.get() == "auth") { "Central requires releaseModule=auth" }
        listOf("mavenCentralUsername", "mavenCentralPassword", "signingInMemoryKey").forEach {
            require(!providers.gradleProperty(it).orNull.isNullOrBlank()) { "Missing publishing credential: $it" }
        }
    }
}
listOf("generateImportDocs" to "generate", "verifyImportDocs" to "verify").forEach { (taskName, command) ->
    tasks.register<Exec>(taskName) {
        workingDir(rootDir)
        commandLine("python3", "scripts/module_release.py", command)
    }
}
// END MAVEN PUBLICATION ONLY
