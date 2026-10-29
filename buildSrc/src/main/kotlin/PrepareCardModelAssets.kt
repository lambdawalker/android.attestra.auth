import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/** Extracts only the pinned model's assets; no model classes or old core enter the runtime. */
abstract class PrepareCardModelAssets : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val modelArchive: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val archives: ArchiveOperations

    @get:Inject
    abstract val files: FileSystemOperations

    @TaskAction
    fun extract() {
        files.sync {
            from(archives.zipTree(modelArchive)) {
                include("assets/**")
                eachFile { path = path.removePrefix("assets/") }
                includeEmptyDirs = false
            }
            into(outputDirectory)
        }
    }
}
