package net.pandolia.jane.libs

import java.nio.file.*
import com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE
import java.nio.file.StandardWatchEventKinds.*
import java.util.*

const val MAX_WAITE_TIME_AFTER_CHANGE = 1500

enum class FileChangeType { MODIFY, DELETE }
val MODIFY = FileChangeType.MODIFY
val DELETE = FileChangeType.DELETE

class Watcher(
    val folder: String,
    private val onDelete: (String) -> Unit,
    private val onModify: (String) -> Unit,
    private val onFlush: () -> Unit,
    private val taskQueue: TaskQueue = mainQueue
) {
    private val prefixLength = Fs.getRealPath(folder).length + 1
    private val deletedFiles = HashSet<String>()
    private val modifiedFiles = HashSet<String>()
    private var lastChangeTime = 0L

    fun start() {
        taskQueue.onTick(::checkBuffer)
        newThread {
            if (Proc.osName == "Windows") {
                WatcherRunner(this).watch()
                return@newThread
            }

            WatcherRunnerForAllPlatform(this).watch()
        }
    }

    fun trim(realPath: String) = realPath.substring(prefixLength)

    fun trim(path: Path) = trim(path.toAbsolutePath().normalize().toString().replace('\\', '/'))

    fun put(type: FileChangeType, path: String) {
        // Log.debug("-- $type: $path")

        taskQueue.put {
            lastChangeTime = Proc.now
            if (type == MODIFY) {
                deletedFiles.remove(path)
                modifiedFiles.add(path)
            } else {
                deletedFiles.add(path)
                modifiedFiles.remove(path)
            }
        }
    }

    fun put(type: FileChangeType, path: Path) = put(type, trim(path))

    private fun checkBuffer() {
        if (lastChangeTime == 0L || Proc.now - lastChangeTime < MAX_WAITE_TIME_AFTER_CHANGE) {
            return
        }

        lastChangeTime = 0L
        deletedFiles.forEach(onDelete)
        modifiedFiles.forEach(onModify)
        onFlush()
        deletedFiles.clear()
        modifiedFiles.clear()
    }
}

class WatcherRunner(val watcher: Watcher) {

    private val allFiles = HashSet<String>()

    fun watch() {
        allFiles.addAll(Fs.getChildFiles(watcher.folder).map { watcher.trim(it) })

        val watchService = FileSystems.getDefault().newWatchService()
        val types = arrayOf(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        Paths.get(watcher.folder).register(watchService, types, FILE_TREE)

        while (true) {
            val key = watchService.take()
            key.pollEvents().forEach { processEvent(it) }
            if (!key.reset()) {
                Log.warn("Watcher for ${watcher.folder} is stopped")
                break
            }
        }
    }

    private fun processEvent(event: WatchEvent<*>) {
        val path = event.context().toString().replace('\\', '/')
        val type = event.kind()

        Log.debug("$type: $path")

        if (type == ENTRY_DELETE) {
            // a file or a directory is deleted
            val path1 = "$path/"
            allFiles.removeAll {
                if (it == path || it.startsWith(path1)) {
                    watcher.put(DELETE, it)
                    return@removeAll true
                }

                return@removeAll false
            }

            return
        }

        val truePath = "${watcher.folder}/$path"

        // a file is created or modified
        if (Fs.isFile(truePath)) {
            allFiles.add(path)
            watcher.put(MODIFY, path)
            return
        }

        if (!Fs.isDirectory(truePath) || type == ENTRY_MODIFY) {
            return
        }

        // a directory is created
        Fs.getChildFiles(truePath).forEach {
            val file = watcher.trim(it)
            allFiles.add(file)
            watcher.put(MODIFY, file)
        }
    }
}