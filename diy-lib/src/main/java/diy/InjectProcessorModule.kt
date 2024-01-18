package diy

class InjectProcessorModule : Module {
  override fun <T> get(requestedType: Class<T>) = try {
    val factoryClass = Class.forName("${requestedType.name}_Factory")
    factoryClass.getDeclaredConstructor().newInstance()
  } catch (notFound: ClassNotFoundException) {
    null
  } as Factory<T>?
}