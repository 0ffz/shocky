package me.dvyy.shocky

import kotlin.reflect.KType
import kotlin.reflect.typeOf

class DependencyHolder(val parent: DependencyHolder? = null) {
    @PublishedApi
    internal val dependencies = mutableMapOf<KType, Any>()

    @PublishedApi
    internal fun <T> getOrNull(type: KType): T? = dependencies[type] as? T ?: parent?.getOrNull(type)

    inline fun <reified T : Any> getOrNull(): T? = getOrNull(typeOf<T>())
    inline fun <reified T : Any> get(): T =
        getOrNull(typeOf<T>()) ?: error("Requested dependency ${typeOf<T>()} not found!")

    inline fun <reified T : Any> provide(value: T): T {
        dependencies[typeOf<T>()] = value
        return value
    }

    fun child() = DependencyHolder(this)
}