package net.pandolia.jane.libs

import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import kotlin.collections.HashMap

class WatcherRunnerForAllPlatform(val watcher: Watcher) {
    private val watchService = FileSystems.getDefault().newWatchService()
    private val directoryTable = HashMap<WatchKey, Path>()
    private val files = HashSet<Path>()
    private val directories = HashSet<Path>()

    fun watch() {
        registerRecursively(Paths.get(watcher.folder), false)

        while (true) {
            val key = watchService.take()

            val directory = directoryTable[key]
            if (directory == null) {
                Log.warn("WatchKey($key) not recognized!")
                continue
            }

            key.pollEvents().forEach { processEvent(directory, it) }

            if (!key.reset()) {
                // Log.debug("Remove $directory from monitor list")
                directoryTable.remove(key)
                if (directoryTable.isEmpty()) {
                    Log.warn("Watcher for ${watcher.folder} is stopped")
                    break
                }
            }
        }
    }

    private fun processEvent(directory: Path, event: WatchEvent<*>) {
        val kind = event.kind()
        val entry = directory.resolve(event.context().toString())
        val isDirectory = Files.isDirectory(entry)
        val isFile = Files.isRegularFile(entry)

        // Log.debug("$kind: ${watcher.trim(entry)}, isDirectory=$isDirectory, isFile=$isFile")

        if (kind == ENTRY_MODIFY) {
            if (isFile) {
                files.add(entry)
                watcher.put(MODIFY, entry)
            }

            return
        }

        if (kind === ENTRY_CREATE) {
            if (isDirectory) {
                registerRecursively(entry, true)
                return
            }

            if (isFile) {
                files.add(entry)
                watcher.put(MODIFY, entry)
            }

            return
        }

        if (directories.removeAll { it.startsWith(entry) }) {
            files.removeAll {
                if (it.startsWith(entry)) {
                    watcher.put(DELETE, it)
                    return@removeAll true
                }

                return@removeAll false
            }

            return
        }

        if (files.remove(entry)) {
            watcher.put(DELETE, entry)
        }
    }

    private fun registerRecursively(folder: Path, isFolderNewlyCreated: Boolean) {
        Files.walkFileTree(folder, object : SimpleFileVisitor<Path>() {

            override fun preVisitDirectory(directory: Path, attrs: BasicFileAttributes): FileVisitResult {
                // Log.debug("Add $directory to monitor list")
                directories.add(directory)
                val key = directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                directoryTable[key] = directory
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                files.add(file)
                if (isFolderNewlyCreated) {
                    watcher.put(MODIFY, file)
                }
                return FileVisitResult.CONTINUE
            }

        })
    }
}