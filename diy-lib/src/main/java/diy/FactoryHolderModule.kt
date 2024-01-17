package diy

class FactoryHolderModule : Module {
  private val factories = mutableMapOf<Class<out Any?>, Factory<out Any?>>()

  override fun <T> get(requestedType: Class<T>): Factory<T>? =
    factories[requestedType] as Factory<T>?

  fun <T> install(
    requestedType: Class<T>,
    factory: Factory<T>
  ) {
    factories[requestedType] = factory
  }
}

inline fun <reified T> FactoryHolderModule.install(noinline factory: ObjectGraph.() -> T) =
  install(T::class.java, factory)

inline fun <reified T> FactoryHolderModule.installSingleton(noinline factory: ObjectGraph.() -> T) {
  install(T::class.java, singleton(factory))
}

inline fun <reified REQUESTED, reified PROVIDED : REQUESTED> FactoryHolderModule.bind() {
  install(REQUESTED::class.java) { objectGraph ->
    objectGraph[PROVIDED::class.java]
  }
}
