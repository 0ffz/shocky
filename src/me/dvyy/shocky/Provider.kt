//package me.dvyy.shocky
//
//import kotlin.properties.ReadWriteProperty
//import kotlin.reflect.KProperty
//
//interface Provider<T> {
//    fun get(): T
//
//    fun <R> map(transform: (T) -> R): Provider<R> = object : Provider<R> {
//        override fun get(): R = transform(this@Provider.get())
//    }
//}
//
//// The configurable Property
//class Property<T> : Provider<T>, ReadWriteProperty<Any?, T> {
//    private var explicitValue: T? = null
//    private var provider: Provider<T>? = null
//
//    fun set(value: T?) {
//        this.explicitValue = value
//        this.provider = null
//    }
//
//    fun setLazy(provider: () -> T) {
//        this.provider = object : Provider<T> {
//            override fun get(): T = provider()
//        }
//    }
//
//    fun set(provider: Provider<T>) {
//        this.provider = provider
//        this.explicitValue = null
//    }
//
//    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
//        set(value)
//    }
//
//    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
//        return get()
//    }
//
//    fun getOrNull(): T? {
//        return if (provider == null) null else get()
//    }
//
//    override fun get(): T {
//        explicitValue?.let { return it }
//        val provider = provider ?: error("Property not configured")
//        val result = provider.get()
//        explicitValue = result
//        return result
//    }
//}
//
//fun <T> property(default: () -> T): Property<T> {
//    return Property<T>().apply { setLazy(default) }
//}
//
//fun <T : Any> Property<T>.assign(value: T?) {
//    this.set(value)
//}