package net.pandolia.jane.libs

import java.nio.file.*
import com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE
import java.nio.file.StandardWatchEventKinds.*
import java.util.*

const val MAX_WAITE_TIME_AFTER_CHANGE = 1000

open class WatcherBase(
    protected val folder: String,
    protected val onDelete: (String) -> Unit,
    protected val onModify: (String) -> Unit,
    protected val onFlush: () -> Unit,
    protected val taskQueue: TaskQueue = mainQueue
) {
    private val prefixLength = Fs.getRealPath(folder).length + 1

    protected var lastChangeTime = 0L
    protected val deletedFiles = HashSet<String>()
    protected val modifiedFiles = HashSet<String>()

    fun start() {
        taskQueue.onTick(::checkBuffer)
        newThread(::watch)
    }

    fun trim(realPath: String) = realPath.substring(prefixLength)

    protected open fun watch() { }

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

class Watcher(

    folder: String,
    onDelete: (String) -> Unit,
    onModify: (String) -> Unit,
    onFlush: () -> Unit,
    taskQueue: TaskQueue = mainQueue

): WatcherBase(folder, onDelete, onModify, onFlush, taskQueue) {

    private lateinit var allFiles: HashSet<String>

    override fun watch() {
        allFiles = HashSet(Fs.getChildFiles(folder).map { trim(it) })
        val watchService = FileSystems.getDefault().newWatchService()
        val types = arrayOf(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        Paths.get(folder).register(watchService, types, FILE_TREE)

        while (true) {
            val watchKey = watchService.take()

            for (event in watchKey.pollEvents()) {
                val path = event.context().toString().replace('\\', '/')
                val type = event.kind()
                Log.debug("* $type $path")
                taskQueue.put { onChangeEvent(path, type) }
            }

            if (!watchKey.reset()) {
                Proc.abort("Watch Service is stopped")
            }
        }
    }

    private fun onChangeEvent(path: String, type: WatchEvent.Kind<*>) {
        lastChangeTime = Proc.now

        if (type == ENTRY_DELETE) {
            // a file is deleted
            if (allFiles.remove(path)) {
                modifiedFiles.remove(path)
                deletedFiles.add(path)
                return
            }

            // a directory is deleted
            val path1 = "$path/"
            val files = allFiles.filter { it.startsWith(path1) }
            allFiles.removeAll(files)
            modifiedFiles.removeAll(files)
            deletedFiles.addAll(files)
            return
        }

        val truePath = "$folder/$path"

        // a file is created or modified
        if (Fs.isFile(truePath)) {
            allFiles.add(path)
            modifiedFiles.add(path)
            deletedFiles.remove(path)
            return
        }

        if (!Fs.isDirectory(truePath)) {
            return
        }

        // a directory's content is modified
        // means some child-files are created or deleted
        if (type == ENTRY_MODIFY) {
            return
        }

        // a directory is created
        val files = Fs.getChildFiles(truePath).map { trim(it) }
        allFiles.addAll(files)
        modifiedFiles.addAll(files)
        deletedFiles.removeAll(files)
    }
}