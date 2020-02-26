package net.pandolia.jane.libs

import java.util.*

fun <T> LinkedList<T>.removeOne(pred: (T) -> Boolean): Boolean {
    val it = this.iterator()
    while (it.hasNext()) {
        if (pred(it.next())) {
            it.remove()
            return true
        }
    }

    return false
}

fun <T> LinkedList<T>.uniqAdd(e: T, pred: (T, T) -> Boolean) {
    val it = this.iterator()
    while (it.hasNext()) {
        if (pred(e, it.next())) {
            it.remove()
            break
        }
    }

    this.add(e)
}

fun <T> LinkedList<T>.insert(e: T, isLess: (T, T) -> Boolean) {
    val i = this.indexOfFirst { isLess(e, it) }
    if (i == -1) {
        this.add(e)
        return
    }
    this.add(i, e)
}