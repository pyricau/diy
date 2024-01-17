package coffee

fun main() {
  manualDI()
}

fun manualDI() {
  val logger = CoffeeLogger()
  val heater = ElectricHeater(logger)
  val pump = Thermosiphon(logger, heater)
  val coffeeMaker = CoffeeMaker(logger, heater, pump)

  coffeeMaker.brew()
}

