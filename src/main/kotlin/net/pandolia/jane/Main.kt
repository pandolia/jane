package net.pandolia.jane

import net.pandolia.jane.libs.*

const val staticDir = "static"
const val pageDir = "page"
const val templateDir = "template"
const val configFile = "site.config"
const val footerTmplPath = "$templateDir/footer.mustache"
const val headerTmplPath = "$templateDir/header.mustache"
const val indexPagePath = "$pageDir/index.md"
const val defaultServerPort = 80

lateinit var rootDir: String
    private set

lateinit var buildDir: String
    private set

var serverPort: Int = 0
    private set

fun main() {
    rootDir = Proc.workingDirectory
    buildDir = "../${rootDir.substringAfterLast('/')}-build"
    serverPort = Proc.getArgsOption("p", "port")?.toInt() ?: defaultServerPort

    when (Proc.command) {
        "init" -> initProject()
        "clean" -> cleanProject()
        "build" -> buildProject()
        "dev" -> developProject()
        else -> printUsage()
    }
}

fun printUsage() {
    println("jane init|clean|build|dev [-d|--debug] [-p|--port 80]")
}

fun initProject() {
    Try.get("Init a jane project in $rootDir") {
        Fs.copyResources("/template-project", rootDir)
    }
}

fun cleanProject() {
    Try.get("Delete directory $buildDir") {
        Fs.deleteDirectory(buildDir)
    }
}

fun buildProject() {
    loadConfig()
    loadPages()
    renderPages()
    copyStatics()
    deleteExtraFiles()
}

fun developProject() {
    loadConfig()
    loadPages()
    serveProject()
    monitorProject()
    startMainQueue()
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

            if (Fs.isFile(staticPath) || Site.pages.any { it.target_path == targetPath }) {
                return@forEach
            }

            Fs.deleteFile(path)
            Log.info("Delete $path")
        }
}