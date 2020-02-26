package net.pandolia.jane

import net.pandolia.jane.libs.*

const val staticDir = "static"
const val pageDir = "page"
const val templateDir = "template"
const val configFile = "site.config"
const val footerTmplPath = "$templateDir/footer.mustache"
const val headerTmplPath = "$templateDir/header.mustache"
const val indexPagePath = "$pageDir/index.md"
val rootDir = getRealPath(".")
val buildDir = "../${rootDir.substringAfterLast('/')}-build"

fun main() {
    when (Proc.command) {
        "init" -> initProject()
        "clean" -> cleanProject()
        "build" -> buildProject()
        "dev" -> developProject()
        else -> printUsage()
    }
}

fun printUsage() {
    println("jane init|clean|build|dev")
}

fun initProject() {
    tryGet("Init a jane project in $rootDir") {
        copyResources("/template-project", rootDir)
    }
}

fun cleanProject() {
    tryGet("Delete directory $buildDir") {
        deleteDirectory(buildDir)
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
    getChildFiles(staticDir)
        .forEach {
            val source = it.substring(m)
            val target = "$buildDir/${it.substring(n)}"
            copyFileIfModified(source, target)
        }
}

fun deleteExtraFiles() {
    val n = getRealPath(buildDir).length + 1
    getChildFiles(buildDir)
        .forEach { path ->
            val relPath = path.substring(n)
            val staticPath = "$staticDir/$relPath"
            val targetPath = "$buildDir/$relPath"

            if (isFile(staticPath) || Site.pages.any { it.target_path == targetPath }) {
                return@forEach
            }

            deleteFile(path)
            Log.info("Delete $path")
        }
}