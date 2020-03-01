package net.pandolia.jane.libs

import java.nio.file.*
import com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import kotlin.collections.HashMap

const val MAX_WAITE_TIME_AFTER_CHANGE = 1500

typealias FileChangeType = Boolean
const val MODIFY = true
const val DELETE = false

class Watcher(
    folder: String,
    private val onDelete: (String) -> Unit,
    private val onModify: (String) -> Unit,
    private val onFlush: () -> Unit,
    private val taskQueue: TaskQueue = mainQueue
) {
    private val folderPath = Paths.get(folder)
    private val deletedFiles = HashSet<String>()
    private val modifiedFiles = HashSet<String>()
    private var lastChangeTime = 0L

    fun start() {
        taskQueue.onTick(::checkBuffer)
        newThread { WatcherRunner(folderPath, ::put).watch() }
    }

    fun put(type: FileChangeType, path: String) = taskQueue.put {
        lastChangeTime = Proc.now
        if (type == MODIFY) {
            deletedFiles.remove(path)
            modifiedFiles.add(path)
        } else {
            deletedFiles.add(path)
            modifiedFiles.remove(path)
        }
    }

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

private class WatcherRunner(
    val folderPath: Path,
    val put: (FileChangeType, String) -> Unit
) {

    private val prefixLength = folderPath.toRealPath().toString().length + 1

    private val allFiles = HashSet<String>(Fs.getChildFiles(folderPath).map { trim(it) })

    private val watchService = FileSystems.getDefault().newWatchService()

    private val types = arrayOf(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)

    private val directoryTable = HashMap<WatchKey, Path>()

    private var supportDeepMonitor = true

    private fun trim(path: Path) = path
        .toAbsolutePath()
        .normalize()
        .toString()
        .replace('\\', '/')
        .substring(prefixLength)

    fun watch() {
        try {
            folderPath.register(watchService, types, FILE_TREE)
            Log.info("Monitoring Windows File Change in $folderPath")
        } catch (Ex: UnsupportedOperationException) {
            supportDeepMonitor = false
            registerRecursively(folderPath, false)
            Log.info("Monitoring File Change in $folderPath")
        }

        while (true) {
            val key = watchService.take()

            val directory = if (supportDeepMonitor) folderPath else directoryTable[key]
            if (directory == null) {
                Log.warn("Untracked watchKey($key)")
                continue
            }

            key.pollEvents().forEach { processEvent(directory, it) }

            if (!key.reset()) {
                if (supportDeepMonitor) {
                    Log.warn("Stop to monitoring File Change in $folderPath")
                    break
                }

                directoryTable.remove(key)
                if (directoryTable.isEmpty()) {
                    Log.warn("Stop to monitoring File Change in $folderPath")
                    break
                }
            }
        }
    }

    private fun processEvent(directory: Path, event: WatchEvent<*>) {
        val kind = event.kind()
        val entry = directory.resolve(event.context().toString())
        val path = trim(entry)
        val isDirectory = Files.isDirectory(entry)
        val isFile = Files.isRegularFile(entry)

        Log.debug("$kind: ${path}, isDirectory=$isDirectory, isFile=$isFile")

        if (isFile) {
            allFiles.add(path)
            put(MODIFY, path)
            return
        }

        if (isDirectory) {
            if (kind == ENTRY_MODIFY) {
                return
            }

            if (!supportDeepMonitor) {
                registerRecursively(entry, true)
                return
            }

            Fs.getChildFiles(entry)
                .map { trim(it) }
                .forEach {
                    allFiles.add(it)
                    put(MODIFY, it)
                }

            return
        }

        if (kind != ENTRY_DELETE) {
            return
        }

        val path1 = "$path/"

        allFiles.removeAll {
            if (it == path || it.startsWith(path1)) {
                put(DELETE, it)
                return@removeAll true
            }

            return@removeAll false
        }
    }

    private fun registerRecursively(dir: Path, dirIsNewlyCreate: Boolean) {
        Files.walkFileTree(dir, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(path: Path, attrs: BasicFileAttributes): FileVisitResult {
                directoryTable[path.register(watchService, types)] = path
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (dirIsNewlyCreate) {
                    val path = trim(file)
                    allFiles.add(path)
                    put(MODIFY, path)
                }
                return FileVisitResult.CONTINUE
            }
        })
    }

}