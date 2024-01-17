package diy

class Linker {
  val factories = mutableMapOf<Class<out Any?>, Linker.() -> Any?>()
}

fun <T> Linker.install(
  requestedType: Class<T>,
  factory: Linker.() -> T
) {
  factories[requestedType] = factory
}

var UNINITIALIZED: Any? = Any()

fun <T> (Linker.() -> T).asSingleton(): (Linker.() -> T) {
  var instance = UNINITIALIZED
  return {
    if (instance === UNINITIALIZED) {
      instance = this@asSingleton()
    }
    instance as T
  }
}

fun <REQUESTED, PROVIDED : REQUESTED> Linker.bind(
  requestedType: Class<REQUESTED>,
  providedType: Class<PROVIDED>
) {
  factories[requestedType] = {
    get(providedType)
  }
}

fun <T> Linker.get(requestedType: Class<T>): T {
  val factoryOrNull = factories[requestedType] as (Linker.() -> T)?

  val factory = if (factoryOrNull == null) {
    val injectConstructor =
      requestedType.constructors.single { it.isAnnotationPresent(Inject::class.java) }
    val factory: Linker.() -> T = {
      injectConstructor.newInstance(
        *injectConstructor.parameterTypes.map { get(it) }.toTypedArray()
      ) as T
    }
    val wrappedFactory = if (requestedType.isAnnotationPresent(Singleton::class.java)) {
      factory.asSingleton()
    } else {
      factory
    }
    install(requestedType, wrappedFactory)
    wrappedFactory
  } else {
    factoryOrNull
  }
  return factory()
}

inline fun <reified T> Linker.get() = get(T::class.java)

inline fun <reified T> Linker.install(noinline factory: Linker.() -> T) =
  install(T::class.java, factory)

inline fun <reified T> Linker.installSingleton(noinline factory: Linker.() -> T) {
  install(factory.asSingleton())
}

inline fun <reified REQUESTED, reified PROVIDED : REQUESTED> Linker.bind() {
  bind(REQUESTED::class.java, PROVIDED::class.java)
}

