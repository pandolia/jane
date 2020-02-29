package net.pandolia.jane.libs

import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import kotlin.collections.HashMap

fun watchFolder(
    folder: String,
    onDelete: (String) -> Unit,
    onModify: (String) -> Unit,
    onFlush: () -> Unit,
    taskQueue: TaskQueue = mainQueue
) {
    if (Proc.osName == "Windows") {
        Watcher(folder, onDelete, onModify, onFlush, taskQueue).start()
        return
    }

    WatcherForAllPlatform(folder, onDelete, onModify, onFlush, taskQueue).start()
}

class WatcherForAllPlatform(

    folder: String,
    onDelete: (String) -> Unit,
    onModify: (String) -> Unit,
    onFlush: () -> Unit,
    taskQueue: TaskQueue = mainQueue

): WatcherBase(folder, onDelete, onModify, onFlush, taskQueue) {

    private val watchService = FileSystems.getDefault().newWatchService()
    private val directoryTable = HashMap<WatchKey, Path>()
    private val files = HashSet<Path>()
    private val directories = HashSet<Path>()

    private fun onModifyFile(file: String) {
        lastChangeTime = Proc.now
        deletedFiles.remove(file)
        modifiedFiles.add(file)
    }

    private fun onDeleteFile(file: String) {
        lastChangeTime = Proc.now
        deletedFiles.add(file)
        modifiedFiles.remove(file)
    }
    
    override fun watch() {
        val folderPath = Paths.get(folder)
        registerRecursively(folderPath, false)

        while (true) {
            val key = watchService.take()

            val directory = directoryTable[key]
            if (directory == null) {
                Log.warn("WatchKey($key) not recognized!")
                continue
            }

            for (event in key.pollEvents()) {
                val kind = event.kind()
                @Suppress("UNCHECKED_CAST") val name = (event as WatchEvent<Path?>).context()!!
                val entry = directory.resolve(name)
                val relPath = trim(entry.toAbsolutePath().normalize().toString().replace('\\', '/'))

                Log.debug("** $kind: $entry, $relPath, isDirectory=${Files.isDirectory(entry)}, "
                              + "isFile=${Files.isRegularFile(entry)}")

                if (kind == ENTRY_MODIFY) {
                    if (Files.isRegularFile(entry)) {
                        files.add(entry)
                        taskQueue.put { onModifyFile(relPath) }
                    }

                    continue
                }

                if (kind === ENTRY_CREATE) {
                    if (Files.isDirectory(entry)) {
                        registerRecursively(entry, true)
                        continue
                    }

                    if (Files.isRegularFile(entry)) {
                        files.add(entry)
                        taskQueue.put { onModifyFile(relPath) }
                    }

                    continue
                }

                if (directories.removeAll { it.startsWith(entry) }) {
                    files.removeAll {
                        if (it.startsWith(entry)) {
                            val p = trim(it.toAbsolutePath().normalize().toString().replace('\\', '/'))
                            taskQueue.put { onDeleteFile(p) }
                            true
                        } else {
                            false
                        }
                    }
                    continue
                }

                if (files.remove(entry)) {
                    taskQueue.put { onDeleteFile(relPath) }
                }
            }

            if (!key.reset()) {
                directoryTable.remove(key)
                if (directoryTable.isEmpty()) {
                    Log.warn("Watch loop is stopped")
                    break
                }
            }
        }
    }

    private fun registerRecursively(folder: Path, isFolderNewlyCreated: Boolean) {
        Files.walkFileTree(folder, object : SimpleFileVisitor<Path>() {

            override fun preVisitDirectory(directory: Path, attrs: BasicFileAttributes): FileVisitResult {
                directories.add(directory)
                val key = directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                directoryTable[key] = directory
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                files.add(file)
                if (isFolderNewlyCreated) {
                    val relPath = trim(file.toRealPath().toString().replace('\\', '/'))
                    taskQueue.put { onModifyFile(relPath) }
                }
                return FileVisitResult.CONTINUE
            }

        })
    }
}