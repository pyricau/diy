package diy

import java.lang.reflect.Constructor
import javax.inject.Inject
import javax.inject.Singleton

class ReflectiveModule : Module {
  override fun <T> get(requestedType: Class<T>): Factory<T> {
    val reflectiveFactory = ReflectiveFactory(requestedType)
    return if (requestedType.isAnnotationPresent(Singleton::class.java)) {
      singleton(reflectiveFactory)
    } else {
      reflectiveFactory
    }
  }

  private class ReflectiveFactory<T>(
    requestedType: Class<T>
  ) : Factory<T> {
    private val injectConstructor = requestedType.constructors.single {
      it.isAnnotationPresent(Inject::class.java)
    } as Constructor<T>

    override fun get(objectGraph: ObjectGraph): T {
      val parameters = injectConstructor.parameterTypes.map { paramType ->
        objectGraph[paramType]
      }.toTypedArray()
      return injectConstructor.newInstance(*parameters)
    }
  }
}