package net.pandolia.jane.libs

import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue

const val TICK_INTERVAL_MILLIS_SECONDS = 500L

typealias Action = () -> Unit

private val NullAction: Action = {}

class TaskQueue {
    private val queue = LinkedBlockingQueue<Action>()
    private val stopActions = ArrayList<Action>()
    private val tickActions = ArrayList<Action>()

    fun put(task: Action) {
        queue.put(task)
    }

    fun stop() = put {
        stopActions.forEach { put(it) }
        put(NullAction)
    }

    fun onStop(task: Action) = stopActions.add(task)

    fun onTick(task: Action) = tickActions.add(task)

    fun tickForever() {
        while (true) {
            Thread.sleep(TICK_INTERVAL_MILLIS_SECONDS)
            put {
                tickActions.forEach { put(it) }
            }
        }
    }

    fun run() {
        newThread(::tickForever)

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