package diy

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

@Target(CLASS)
@Retention(SOURCE)
annotation class Component(
  val modules: Array<KClass<*>> = []
)
