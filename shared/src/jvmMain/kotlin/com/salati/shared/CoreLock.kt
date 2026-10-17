package com.salati.shared
internal actual class CoreLock actual constructor() {
    private val value = java.util.concurrent.locks.ReentrantLock()
    actual fun lock() = value.lock()
    actual fun unlock() = value.unlock()
}
