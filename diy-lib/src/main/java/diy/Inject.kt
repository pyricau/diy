package diy

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR

@Target(CONSTRUCTOR)
@Retention(RUNTIME)
annotation class Inject
