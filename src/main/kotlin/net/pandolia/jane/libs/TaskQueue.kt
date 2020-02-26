package net.pandolia.jane.libs

import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue

typealias Action = () -> Unit

private val NullAction: Action = {}

class TaskQueue {
    private val queue = LinkedBlockingQueue<Action>()
    private val queueOnStop = LinkedBlockingQueue<Action>()

    fun put(task: Action) {
        queue.put(task)
    }

    fun onStop(task: Action) {
        queueOnStop.put(task)
    }

    fun stop() {
        @Suppress("UNCHECKED_CAST")
        queueOnStop.toArray().forEach { queue.put(it as Action) }
        queue.put(NullAction)
    }

    fun run() {
        while (true) {
            val task = queue.take()
            if (task == NullAction) {
                Proc.exit(0)
            }

            try {
                task()
            } catch (ex: Exception) {
                ex.printStackTrace(System.out)
                Proc.exit(1)
            }
        }
    }
}

val mainQueue = TaskQueue()

fun startMainQueue() {
    daemonThread {
        readLine()
        mainQueue.stop()
    }

    Log.info("Start main queue, press enter to exit.")
    mainQueue.run()
}

fun daemonThread(action: Action): Thread {
    val th = Thread(action)
    th.isDaemon = true
    th.start()
    return th
}

class Futrue<T: Any>(taskQueue: TaskQueue = mainQueue, val func: () -> T) {
    private val latch = CountDownLatch(1)

    private lateinit var result: T

    init {
        taskQueue.put(::task)
    }

    private fun task() {
        result = func()
        latch.countDown()
    }

    fun wait(): T {
        latch.await()
        return result
    }
}