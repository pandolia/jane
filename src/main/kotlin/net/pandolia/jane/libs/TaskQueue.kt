package net.pandolia.jane.libs

import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue

typealias Action = () -> Unit

val NullAction: Action = {}

class TaskQueue {
    private val queue = LinkedBlockingQueue<Action>()
    private val queueOnStop = LinkedBlockingQueue<Action>()

    fun put(task: Action) {
        queue.put(task)
    }

    fun <T : Any> putFuture(func: () -> T): Futrue<T> {
        val futrue = Futrue(func)
        put(futrue::task)
        return futrue
    }

    fun onStop(task: Action) {
        queueOnStop.put(task)
    }

    fun stop() {
        queueOnStop.toList().forEach { queue.put(it) }
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
    newThread {
        readLine()
        mainQueue.stop()
    }
    Log.info("Start main queue, press enter to exit.")
    mainQueue.run()
}

fun newThread(action: Action): Thread {
    val th = Thread(action)
    th.isDaemon = true
    th.start()
    return th
}

class Futrue<T: Any>(val func: () -> T) {
    private val latch = CountDownLatch(1)

    private lateinit var result: T

    internal fun task() {
        result = func()
        latch.countDown()
    }

    fun wait(): T {
        latch.await()
        return result
    }
}