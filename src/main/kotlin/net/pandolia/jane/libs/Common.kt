package net.pandolia.jane.libs

import kotlin.system.exitProcess

object Proc {
    val args = System.getProperty("exec.args", "")
        .split(" ")
        .filter { it.isNotEmpty() }
        .toMutableList()

    var debugMode = args.remove("-d")

    val command = args.getOrNull(0) ?: ""

    init {
        System.setProperty("file.encoding", "UTF-8")
    }

    fun exit(code: Int): Nothing = exitProcess(code)

    fun abort(msg: String): Nothing {
        println("[ABORT] $msg")
        exitProcess(1)
    }
}

object Log {
    fun debug(msg: String) {
        if (!Proc.debugMode) {
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

    fun error(msg: String) {
        println("[ERROR] $msg")
    }
}

fun <T> tryGet(msg: String, block: () -> T): T? {
    try {
        val result = block()
        println("[INFO] $msg")
        return result
    } catch (ex: Exception) {
        val trace = if (Proc.debugMode) "\n    ${ex.stackTrace.joinToString("\n    ")}" else  ""
        println("[ERROR] Failed to ${msg.decapitalize()}: ${ex.javaClass.simpleName}(${ex.message})$trace")
        return null
    }
}

fun <T> tryExec(msg: String, block: () -> T): T {
    return tryGet(msg, block) ?: Proc.exit(1)
}

fun getProps(input: String): Map<String, String> {
    return input
        .split('\n')
        .map { it.trim() }
        .filter { !it.startsWith('#') && it.contains(':') }
        .map { line ->
            val i = line.indexOf(':')
            Pair(line.substring(0, i).trim(), line.substring(i + 1).trim())
        }
        .toMap()
}