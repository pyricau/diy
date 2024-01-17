package diy

class InjectProcessorModule : Module {
  override fun <T> get(requestedType: Class<T>) = try {
    Class.forName("${requestedType.name}_Factory").getDeclaredConstructor().newInstance()
  } catch (notFound: ClassNotFoundException) {
    null
  } as Factory<T>?
}