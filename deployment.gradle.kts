import org.apache.tools.ant.taskdefs.condition.Os
import java.io.ByteArrayOutputStream

val harmonyAppDirName = "ohosApp"
val harmonyPackageName = "com.kyant.backdrop.catalog"
val harmonyAbilityName = "EntryAbility"

fun osExecutableName(name: String): String =
    if (Os.isFamily(Os.FAMILY_WINDOWS)) "$name.exe" else name

fun pathExecutable(name: String): String? {
    val exeName = osExecutableName(name)
    return System.getenv("PATH")
        .split(File.pathSeparator)
        .map { File(it, exeName) }
        .firstOrNull { it.exists() && it.canExecute() }
        ?.absolutePath
}

fun devEcoExecutable(name: String): String? {
    val exeName = osExecutableName(name)
    val candidates = when {
        name == "hdc" && Os.isFamily(Os.FAMILY_MAC) -> listOf(
            "/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains/$exeName",
            "/Applications/DevEco Studio.app/Contents/sdk/default/openharmony/toolchains/$exeName",
        )
        name == "ohpm" && Os.isFamily(Os.FAMILY_MAC) -> listOf(
            "/Applications/DevEco-Studio.app/Contents/tools/ohpm/bin/$exeName",
            "/Applications/DevEco Studio.app/Contents/tools/ohpm/bin/$exeName",
        )
        else -> emptyList()
    }
    return candidates.firstOrNull { File(it).let { file -> file.exists() && file.canExecute() } }
}

fun findExecutablePath(name: String): String =
    pathExecutable(name) ?: devEcoExecutable(name) ?: name

abstract class DeployHarmonyTask : DefaultTask() {
    @get:Input
    abstract val hdcPath: Property<String>

    @get:Input
    abstract val harmonyPackage: Property<String>

    @get:Input
    abstract val harmonyAbility: Property<String>

    @get:InputDirectory
    abstract val hapOutputDir: DirectoryProperty

    @get:Internal
    abstract val workingDirectory: DirectoryProperty

    @TaskAction
    fun deploy() {
        val outputDir = hapOutputDir.get().asFile
        val workDir = workingDirectory.get().asFile
        val hap = outputDir
            .walkTopDown()
            .filter { it.extension == "hap" && it.name.contains("signed") }
            .maxByOrNull { it.lastModified() }
            ?: throw GradleException("No signed HAP file found in ${outputDir.absolutePath}")

        val devicesOut = ByteArrayOutputStream()
        val hdc = hdcPath.get()
        runProcess(listOf(hdc, "list", "targets"), workDir, devicesOut)
        val deviceId = devicesOut.toString()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.contains("Empty") }
            .map { it.split(Regex("\\s+")).first() }
            .firstOrNull()
            ?: throw GradleException("No HarmonyOS device found.")

        runProcess(listOf(hdc, "-t", deviceId, "install", "-r", hap.absolutePath), workDir)
        runProcess(
            listOf(
                hdc,
                "-t",
                deviceId,
                "shell",
                "aa",
                "start",
                "-a",
                harmonyAbility.get(),
                "-b",
                harmonyPackage.get(),
            ),
            workDir,
        )
    }

    private fun runProcess(command: List<String>, workingDir: File, output: ByteArrayOutputStream? = null) {
        val process = ProcessBuilder(command)
            .directory(workingDir)
            .redirectErrorStream(output == null)
            .start()
        if (output == null) {
            process.inputStream.copyTo(System.out)
        } else {
            process.inputStream.copyTo(output)
            process.errorStream.copyTo(System.err)
        }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("Command failed with exit code $exitCode: ${command.joinToString(" ")}")
        }
    }
}

val harmonyAppDir = layout.projectDirectory.dir(harmonyAppDirName)
val harmonyEntryDir = harmonyAppDir.dir("entry")
val nodeExecutable = file("/Applications/DevEco-Studio.app/Contents/tools/node/bin/node")
val hvigorScript = file("/Applications/DevEco-Studio.app/Contents/tools/hvigor/bin/hvigorw.js")
val ohpmExecutable = findExecutablePath("ohpm")
val hdcExecutable = findExecutablePath("hdc")

tasks.register<Exec>("installHarmonyRootDeps") {
    group = "deployment"
    description = "Install HarmonyOS root ohpm dependencies."
    workingDir = harmonyAppDir.asFile
    commandLine(ohpmExecutable, "install", "--all")
}

tasks.register<Exec>("installHarmonyEntryDeps") {
    group = "deployment"
    description = "Install HarmonyOS entry ohpm dependencies."
    dependsOn("installHarmonyRootDeps")
    workingDir = harmonyEntryDir.asFile
    commandLine(ohpmExecutable, "install", "--all")
}

tasks.register("installHarmonyDeps") {
    group = "deployment"
    description = "Install HarmonyOS ohpm dependencies."
    dependsOn("installHarmonyRootDeps", "installHarmonyEntryDeps")
}

tasks.register<Exec>("assembleHarmonyHap") {
    group = "deployment"
    description = "Publish shared binaries and build the HarmonyOS HAP."
    dependsOn(":app:publishDebugBinariesToOhosApp", "installHarmonyDeps")
    workingDir = harmonyAppDir.asFile

    if (nodeExecutable.exists() && hvigorScript.exists()) {
        commandLine(
            nodeExecutable.absolutePath,
            hvigorScript.absolutePath,
            "--no-daemon",
            "--no-parallel",
            "--mode",
            "module",
            "-p",
            "module=entry@default",
            "-p",
            "product=default",
            "assembleHap",
        )
    } else {
        commandLine(
            if (Os.isFamily(Os.FAMILY_WINDOWS)) "hvigorw.bat" else "hvigorw",
            "--no-daemon",
            "--no-parallel",
            "assembleHap",
        )
    }
}

tasks.register<DeployHarmonyTask>("deployHarmony") {
    group = "deployment"
    description = "Build, install and launch HarmonyOS app."
    dependsOn("assembleHarmonyHap")
    hdcPath.set(hdcExecutable)
    harmonyPackage.set(harmonyPackageName)
    harmonyAbility.set(harmonyAbilityName)
    hapOutputDir.set(harmonyEntryDir.dir("build/default/outputs/default"))
    workingDirectory.set(layout.projectDirectory)
}

tasks.register("harmonyApp") {
    group = "application"
    description = "Build, install and launch the HarmonyOS app."
    dependsOn("deployHarmony")
}

tasks.register("ohosApp") {
    group = "application"
    description = "Build, install and launch the OHOS app."
    dependsOn("deployHarmony")
}
