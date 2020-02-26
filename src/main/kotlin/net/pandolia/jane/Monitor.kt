package net.pandolia.jane

import net.pandolia.jane.libs.*

private var needReload = false

fun monitorProject() {
    Log.info("Monitoring file changes in $rootDir")
    Watcher(rootDir, ::onChangeEvent, ::onChangeEventsDone).start()
}

fun onChangeEvent(event: ChangeEvent) {
    val type = event.type
    val path = event.path

    if (isDirectory(path) || path.endsWith('~')) {
        return
    }

    if (path == configFile) {
        if (type == ENTRY_MODIFY) onConfigModify(path) else onConfigDelete(path)
        return
    }

    when (path.substringBefore('/')) {
        pageDir -> if (type == ENTRY_MODIFY) onPageModify(path) else onPageDelete(path)
        templateDir -> if (type == ENTRY_MODIFY) onTemplateModify(path) else onTemplateDelete(path)
        staticDir -> needReload = true
    }
}

fun onConfigDelete(path: String) {
    Proc.abort("The config file $path is deleted")
}

fun onConfigModify(path: String) {
    Log.info("The config file $path is modified")
    reloadConfig()
    needReload = true
}

fun onTemplateDelete(path: String) {
    if (hasPage(path)) {
        Proc.abort("Template $path which is needed by some pages is deleted")
    }
    Log.info("Template $path is deleted")
}

fun onTemplateModify(path: String) {
    Log.info("Template $path is modified")
    needReload = needReload || hasPage(path)
}

fun onPageDelete(path: String) {
    if (path == indexPagePath) {
        Proc.abort("Page $path is delete")
    }

    if (deletePage(path)) {
        Log.info("Page $path is deleted")
        needReload = true
    }
}

fun onPageModify(path: String) {
    Log.info("File $path is modified")

    val page = getPage(path)
    if (page != null) {
        needReload = page.readProps() || needReload
        return
    }

    needReload = makePage(path) || needReload
}

fun onChangeEventsDone() {
    if (!needReload) {
        return
    }

    notifyClientsToReload()
    needReload = false
}