package net.pandolia.jane.libs

import kotlin.system.exitProcess

object Proc {
    init {
        System.setProperty("file.encoding", "UTF-8")
    }

    val workingDirectory = Fs.getRealPath(".")

    val args = System.getProperty("exec.args", "")
        .split(" ")
        .filter { it.isNotEmpty() }
        .toMutableList()

    val isDebug = args.removeAll { it == "-d" || it == "--debug" }

    val command = args.getOrNull(0) ?: ""

    fun exit(code: Int): Nothing = exitProcess(code)

    fun abort(msg: String): Nothing {
        println("[ABORT] $msg")
        exitProcess(1)
    }

    fun getArgsOption(shortName: String, name: String): String? {
        val shortName1 = "-$shortName"
        val name1 = "-$name"

        for (i in args.size - 2 downTo 1) {
            if (args[i] == shortName1 || args[i] == name1) {
                return args[i + 1]
            }
        }

        return null
    }
}

object Log {
    fun debug(msg: String) {
        if (!Proc.isDebug) {
            return
        }

        println(msg.trimMargin())
    }

    fun info(msg: String) {
        println("[INFO] $msg")
    }

    fun warn(msg: String) {
        println("[WARN] $msg")
    }
}

object Try {
    fun <T> get(msg: String, block: () -> T): T? {
        return try {
            val result = block()
            println("[INFO] $msg")
            result
        } catch (ex: Exception) {
            println("[ERROR] Failed to ${msg.decapitalize()}: ${ex.detail}")
            null
        }
    }

    fun <T> exec(msg: String, block: () -> T): T {
        return get(msg, block) ?: Proc.exit(1)
    }
}

val Exception.detail: String get() {
    if (!Proc.isDebug) {
        return "${javaClass.simpleName}(${message})"
    }

    return "${javaClass.simpleName}(${message})\n    ${stackTrace.joinToString("\n    ")}"
}