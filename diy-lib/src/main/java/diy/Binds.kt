package diy

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(FUNCTION)
@Retention(SOURCE)
annotation class Binds
