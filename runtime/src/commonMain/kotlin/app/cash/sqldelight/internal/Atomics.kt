package app.cash.sqldelight.internal

import kotlin.reflect.KProperty

expect class AtomicBoolean(value: Boolean) {
  fun get(): Boolean
  fun set(value: Boolean)
}

operator fun AtomicBoolean.getValue(thisRef: Any?, prop: KProperty<*>): Boolean {
  return get()
}

operator fun AtomicBoolean.setValue(thisRef: Any?, prop: KProperty<*>, value: Boolean) {
  set(value)
}

expect class Atomic<V>(value: V) {
  fun get(): V
  fun set(value: V)
}

operator fun <T> Atomic<T>.getValue(thisRef: Any?, prop: KProperty<*>): T {
  return get()
}

operator fun <T> Atomic<T>.setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
  set(value)
}
