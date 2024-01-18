package coffee

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoffeeLogger @Inject constructor() {

  fun log(msg: String) {
    println(msg)
  }
}