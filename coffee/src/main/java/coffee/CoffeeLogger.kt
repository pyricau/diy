package coffee

import diy.Inject
import diy.Singleton

@Singleton
class CoffeeLogger @Inject constructor() {

  private val _logs = mutableListOf<String>()

  val logs: List<String>
    get() = _logs

  fun log(msg: String) {
    _logs.add(msg)
    println(msg)
  }
}