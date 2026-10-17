package com.salati.shared

internal expect class CoreLock() { fun lock(); fun unlock() }
internal class LruCache<K,V>(private val capacity: Int) {
    val lock = CoreLock()
    private val values = mutableMapOf<K,V>()
    operator fun get(key: K): V? = values.remove(key)?.also { values[key] = it }
    fun getOrPut(key: K, factory: () -> V): V {
        get(key)?.let { return it }
        val value = factory()
        if(values.size >= capacity) values.remove(values.keys.first())
        values[key] = value
        return value
    }
    fun clear() = values.clear()
}
internal inline fun <K,V,T> synchronized(cache: LruCache<K,V>, block: () -> T): T {
    cache.lock.lock()
    try { return block() } finally { cache.lock.unlock() }
}
