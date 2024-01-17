package coffee

import diy.Inject

class Thermosiphon @Inject constructor(
  private val logger: CoffeeLogger,
  private val heater: Heater
) : Pump {
  override fun pump() {
    if (heater.isHot) {
      logger.log("=> => pumping => =>")
    }
  }
}