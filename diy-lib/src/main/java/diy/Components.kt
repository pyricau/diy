package diy

fun <T> componentSingleton(factory: () -> T): () -> T {
  var instance: Any? = UNINITIALIZED
  return {
    if (instance === UNINITIALIZED) {
      instance = factory()
    }
    instance as T
  }
}