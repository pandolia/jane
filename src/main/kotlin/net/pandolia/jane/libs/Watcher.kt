package net.pandolia.jane.libs

import java.nio.file.*
import com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE
import java.util.*

const val MAX_WAITE_TIME_AFTER_CHANGE = 1000

enum class ChangeType {
    EntryDelete,
    EntryModify
}

val ENTRY_DELETE = ChangeType.EntryDelete
val ENTRY_MODIFY = ChangeType.EntryModify

data class ChangeEvent(
    var type: ChangeType,
    val path: String
)

class Watcher(
    private val folder: String,
    private val onChange: (ChangeEvent) -> Unit,
    private val onChangesDone: () -> Unit,
    private val taskQueue: TaskQueue = mainQueue
) {
    private val eventBuffer = LinkedList<ChangeEvent>()
    private var lastEventTime = Date().time

    fun start() {
        Fs.testDirectory(folder)
        daemonThread(::watch)
        daemonThread(::monitorBuffer)
    }

    private fun mergeEventToBuffer(event: ChangeEvent) {
        eventBuffer.uniqAdd(event) { e1, e2 -> e1.path == e2.path }
        lastEventTime = Date().time
    }

    private fun checkBuffer() {
        if (eventBuffer.isEmpty() || Date().time - lastEventTime < MAX_WAITE_TIME_AFTER_CHANGE) {
            return
        }

        eventBuffer.forEach(onChange)
        onChangesDone()
        eventBuffer.clear()
    }

    private fun watch() {
        val watchService = FileSystems.getDefault().newWatchService()
        val types = arrayOf(
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )
        Paths.get(folder).register(watchService, types, FILE_TREE)

        while (true) {
            val watchKey = watchService.take()

            for (event in watchKey.pollEvents()) {
                val type = if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    ENTRY_DELETE
                } else {
                    ENTRY_MODIFY
                }
                val path = event.context().toString().replace('\\', '/')
                val ev = ChangeEvent(type, path)
                taskQueue.put { mergeEventToBuffer(ev) }
            }

            if (!watchKey.reset()) {
                Proc.abort("Watch Service is stopped")
            }
        }
    }

    private fun monitorBuffer() {
        while (true) {
            Thread.sleep(500)
            taskQueue.put(::checkBuffer)
        }
    }
}