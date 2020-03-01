package net.pandolia.jane.libs

import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors

object Fs {
    fun readFile(filename: String): String {
        return File(filename).readText()
    }

    fun getPropsFromFile(filename: String): Map<String, String> {
        return readFile(filename).parseToDict()
    }

    fun getPropsFromResource(resName: String): Map<String, String> {
        return readResourceFile(resName).parseToDict()
    }

    fun getResourceURL(resPath: String): URL {
        return (object {}).javaClass.getResource(resPath)
            ?: throw FileNotFoundException("Resource:$resPath")
    }

    fun readResourceFile(resPath: String): String {
        return getResourceURL(resPath).readText()
    }

    fun copyResources(resDir: String, tarDir: String): String {
        val tarFile = File(tarDir)
        if (tarFile.list() == null || tarFile.list()!!.isNotEmpty()) {
            throw Exception("\$tarDir($tarDir) must be an empty direcotry")
        }

        val jarURI = getResourceURL("").toURI()

        // not run with jar file
        if (jarURI.path != null) {
            val resFile = File(getResourceURL(resDir).path)
            if (!resFile.isDirectory) {
                throw Exception("\$resDir($resDir) must be a direcotry")
            }

            resFile.copyRecursively(tarFile)
            return "ok"
        }

        // run with jar file
        val fileSystem = FileSystems.newFileSystem(jarURI, mapOf<String, String>())
        val resPath = fileSystem.getPath(resDir)
        val tarPath = tarFile.toPath()

        Files.walkFileTree(resPath, object : SimpleFileVisitor<Path>() {
            private lateinit var currentTarget: Path

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                currentTarget = tarPath.resolve(resPath.relativize(dir).toString())
                Files.createDirectories(currentTarget)
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (file == resPath) {
                    throw Exception("\$resDir($resDir) must be a direcotry")
                }

                Files.copy(file, tarPath.resolve(resPath.relativize(file).toString()))
                return FileVisitResult.CONTINUE
            }
        })

        return  "ok"
    }

    fun getRealPath(p: String): String {
        return Paths.get(p).toRealPath().toString().replace('\\', '/')
    }

    fun getChildFiles(directory: String): List<String> {
        testDirectory(directory)

        return Files.walk(Paths.get(directory))
            .filter { Files.isRegularFile(it) }
            .map { it.toRealPath().toString().replace('\\', '/') }
            .collect(Collectors.toList())
    }

    fun getChildFiles(directory: Path): List<Path> {
        return Files.walk(directory)
            .filter { Files.isRegularFile(it) }
            .collect(Collectors.toList())
    }

    fun mkDir(directory: String) = File(directory).mkdir()

    @Suppress("unused")
    fun clearDir(directory: String, pred: (String) -> Boolean = { true }): String {
        testDirectory(directory)

        val path = Paths.get(directory)

        val n = getRealPath(directory).length + 1

        Files.walk(path)
            .filter {
                it.toFile().isFile
                && pred(it.toRealPath().toString().replace('\\', '/').substring(n))
            }
            .forEach { Files.delete(it) }

        Files.walk(path)
            .filter { it != path && Files.isDirectory(it) }
            .collect(Collectors.toList())
            .reversed()
            .forEach { Files.delete(it) }

        return "ok"
    }

    fun testDirectory(path: String) {
        if (!isDirectory(path)) {
           throw Exception("Directory $path not exists")
        }
    }

    fun testFile(path: String) {
        if (!isFile(path)) {
            throw Exception("File $path not exists")
        }
    }

    @Suppress("unused")
    fun deleteDirectory(directory: String): String {
        val file = File(directory)
        if (!file.exists()) {
            return "not found"
        }

        testDirectory(directory)

        file.deleteRecursively()
        return "ok"
    }

    fun copyFileIfModified(source: String, target: String) {
        val src = File(source)
        val tar = File(target)

        if (src.lastModified() < tar.lastModified()) {
            Log.debug("Copy $source -> $target. Skipped")
            return
        }

        Try.exec("Copy $source -> $target") {
            src.copyTo(tar, true)
        }
    }

    fun isDirectory(path: String) = File(path).isDirectory

    fun isFile(path: String) = File(path).isFile

    fun exists(path: String) = File(path).exists()

    fun deleteFile(path: String) = File(path).delete()

    val mimeMap = getPropsFromResource("/mimelist")

    fun getMimeTypeByFileName(resPath: String): String {
        return mimeMap[resPath.substringAfterLast('.')] ?: "application/octet-stream"
    }
}

val File.mimeType get() = Fs.mimeMap[extension] ?: "application/octet-stream"