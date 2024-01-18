package diy

fun interface Factory<T> {
  fun get(objectGraph: ObjectGraph): T
}

fun <T> singleton(factory: Factory<T>): Factory<T> {
  var instance: Any? = UNINITIALIZED
  return Factory { linker ->
    if (instance === UNINITIALIZED) {
      instance = factory.get(linker)
    }
    instance as T
  }
}

val UNINITIALIZED = Any()