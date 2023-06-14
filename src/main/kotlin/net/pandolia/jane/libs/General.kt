package net.pandolia.jane.libs

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.system.exitProcess

object Proc {
    init {
        System.setProperty("file.encoding", "UTF-8")
    }

    val workingDirectory = Fs.getRealPath(".")

    val args = System.getProperty("exec.args", "").split(" ").filter { it.isNotEmpty() }

    val command = args.firstOrNull()

    val subCommand = args.getOrNull(1)

    val isDebug = hasOption("-d", "--debug")

    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    val currentTime: String get() = LocalDateTime.now().format(dateFormatter)

    val now: Long get() = Date().time

    @Suppress("unused")
    val osName: String = System.getProperties().getProperty("os.name")

    fun hasOption(vararg names: String) = args.any { it in names }

    fun getOption(vararg names: String): String? {
        for ( i in args.size - 2 downTo 0) {
            if (args[i] in names) {
                return args[i + 1]
            }
        }
        return null
    }

    fun exit(code: Int): Nothing = exitProcess(code)

    fun abort(msg: String): Nothing {
        Log.log("ABORT", msg)
        exitProcess(1)
    }
}

object Log {
    fun debug(msg: String) {
        if (!Proc.isDebug) {
            return
        }

        println(msg.trimMargin())
    }

    fun log(level: String, msg: String) {
        println("[${Proc.currentTime}] [$level] $msg")
    }

    fun info(msg: String) {
        log("INFO", msg)
    }

    fun warn(msg: String) {
        log("WARN", msg)
    }

    fun error(msg: String) {
        log("ERROR", msg)
    }
}

object Try {
    fun <T> get(msg: String, block: () -> T): T? {
        return try {
            val result = block()
            Log.log("INFO", msg)
            result
        } catch (ex: Exception) {
            Log.error("Failed to ${msg.decapitalize()}: ${ex.detail}")
            null
        }
    }

    fun <T> exec(msg: String, block: () -> T): T {
        return get(msg, block) ?: Proc.exit(1)
    }
}

val Exception.detail: String
    get() = if (!Proc.isDebug) {
        "${javaClass.simpleName}(${message})"
    } else {
        "${javaClass.simpleName}(${message})\n    ${stackTrace.joinToString("\n    ")}"
    }
