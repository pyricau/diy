package diy

class Linker {
  val factories = mutableMapOf<Class<out Any?>, Linker.() -> Any?>()
}

inline fun <reified T> Linker.install(noinline factory: Linker.() -> T) {
  factories[T::class.java] = factory
}

inline fun <reified T> Linker.get(): T {
  return factories.getValue(T::class.java)() as T
}

inline fun <reified T> Linker.installSingleton(noinline factory: Linker.() -> T) {
  var instance = UNINITIALIZED
  install {
    if (instance === UNINITIALIZED) {
      instance = factory()
    }
    instance as T
  }
}

var UNINITIALIZED: Any? = Any()