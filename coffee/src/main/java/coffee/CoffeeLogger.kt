package coffee

import diy.Inject
import diy.Singleton

@Singleton
class CoffeeLogger @Inject constructor() {

  fun log(msg: String) {
    println(msg)
  }
}