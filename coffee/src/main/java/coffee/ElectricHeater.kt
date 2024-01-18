package coffee

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ElectricHeater @Inject constructor(private val logger: CoffeeLogger) : Heater {
  override var isHot = false
    private set

  override fun on() {
    isHot = true
    logger.log("~ ~ ~ heating ~ ~ ~")
  }

  override fun off() {
    isHot = false
    logger.log("~ ~ ~ cooling ~ ~ ~")
  }
}