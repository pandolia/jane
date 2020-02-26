package net.pandolia.jane.libs

fun <T> MutableList<T>.removeOne(pred: (T) -> Boolean): Boolean {
    val it = iterator()
    while (it.hasNext()) {
        if (pred(it.next())) {
            it.remove()
            return true
        }
    }

    return false
}

fun <T> MutableList<T>.uniqAdd(e: T, pred: (T, T) -> Boolean) {
    val it = iterator()
    while (it.hasNext()) {
        if (pred(e, it.next())) {
            it.remove()
            break
        }
    }

    add(e)
}

fun <T> MutableList<T>.insert(e: T, isLess: (T, T) -> Boolean) {
    val i = indexOfFirst { isLess(e, it) }
    if (i == -1) {
        add(e)
        return
    }

    add(i, e)
}