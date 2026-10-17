package com.salati.shared
import platform.Foundation.NSRecursiveLock
internal actual class CoreLock actual constructor() {
    private val value = NSRecursiveLock()
    actual fun lock() = value.lock()
    actual fun unlock() = value.unlock()
}
