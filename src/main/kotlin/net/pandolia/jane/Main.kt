package net.pandolia.jane

import net.pandolia.jane.libs.*

const val staticDir = "static"
const val pageDir = "page"
const val configFile = "site.config"
const val templatePath = "template.mustache"
const val buildDir = "../build"
const val defaultServerPort = 8000

var rootDir = ""
    private set

var serverPort = 0
    private set

fun main() {
    rootDir = Proc.workingDirectory
    serverPort = Proc.getArgsOption("p", "port")?.toInt() ?: defaultServerPort

    when (Proc.command) {
        "create" -> createProject()
        "dev" -> developProject()
        "build" -> buildProject()
        "clean" -> cleanProject()
        else -> printUsage()
    }
}

fun printUsage() {
    println("jane create PROJECT-NAME\njane dev|build|clean [-d|--debug] [-p|--port 80]")
}

fun createProject() {
    val projectName = Proc.args.firstOrNull() ?: ""

    if (Fs.exists(projectName)) {
        Proc.abort("Project $projectName already exists")
    }

    if (!Fs.mkDir(projectName)) {
        Proc.abort("Fail to make directory $projectName")
    }

    Try.get("Create a jane project($projectName)") {
        Fs.copyResources("/hello-jane", projectName)
    }
}

fun developProject() {
    loadConfig()
    loadPages()
    serveProject()
    monitorProject()
    startMainQueue()
}

fun buildProject() {
    loadConfig()
    loadPages()
    renderPages()
    copyStatics()
    deleteExtraFiles()
}

fun cleanProject() {
    Try.get("Delete files in $buildDir") {
        Fs.clearDir(buildDir) { it != "readme" }
    }
}

fun copyStatics() {
    val m = rootDir.length + 1
    val n = m + staticDir.length + 1
    Fs.getChildFiles(staticDir)
        .forEach {
            val source = it.substring(m)
            val target = "$buildDir/${it.substring(n)}"
            Fs.copyFileIfModified(source, target)
        }
}

fun deleteExtraFiles() {
    val n = Fs.getRealPath(buildDir).length + 1
    Fs.getChildFiles(buildDir)
        .forEach { path ->
            val relPath = path.substring(n)
            val staticPath = "$staticDir/$relPath"
            val targetPath = "$buildDir/$relPath"

            if (relPath == "readme"
                || Fs.isFile(staticPath)
                || Site.pages.any { it.target_path == targetPath }) {
                return@forEach
            }

            Fs.deleteFile(path)
            Log.info("Delete $path")
        }
}
