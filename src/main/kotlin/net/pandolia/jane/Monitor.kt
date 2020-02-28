package net.pandolia.jane

import net.pandolia.jane.libs.*

private var needReload = false

fun monitorProject() {
    Log.info("Monitoring file changes in $rootDir")
    Watcher(rootDir, ::onDelete, ::onModify, ::onFlush).start()
}

fun onDelete(path: String) {
    if (path.endsWith('~')) {
        return
    }

    Log.info("Detect file deleted: $path")

    if (path == configFile || path == templatePath) {
        Proc.exit(1)
    }

    val dir = path.substringBefore('/')

    if (dir == staticDir) {
        needReload = true
        return
    }

    if (dir != pageDir || !path.endsWith(".md")) {
        return
    }

    if (deletePage(path)) {
        needReload = true
        return
    }
}

fun onModify(path: String) {
    if (path.endsWith('~')) {
        return
    }

    Log.info("Detect file modified: $path")

    if (path == configFile) {
        reloadConfig()
        needReload = true
        return
    }

    val dir = path.substringBefore('/')

    if (path == templatePath || dir == staticDir) {
        needReload = true
        return
    }

    if (dir != pageDir || !path.endsWith(".md")) {
        return
    }

    val page = getPage(path)
    if (page != null) {
        needReload = page.readProps() || needReload
        return
    }

    needReload = makePage(path) || needReload
}

fun onFlush() {
    if (!needReload) {
        return
    }

    broadcast("reload page")
    needReload = false
}