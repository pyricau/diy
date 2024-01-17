package coffee

class CoffeeLogger {

  private val _logs = mutableListOf<String>()

  val logs: List<String>
    get() = _logs

  fun log(msg: String) {
    _logs.add(msg)
    println(msg)
  }
}