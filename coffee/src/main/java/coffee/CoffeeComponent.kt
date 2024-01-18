package coffee

import diy.Binds
import diy.Component

@Component(modules = [CoffeeBinds::class])
interface CoffeeComponent {
  val coffeeMaker: CoffeeMaker
}

abstract class CoffeeBinds {
  @Binds abstract fun bindHeater(heater: ElectricHeater): Heater
  @Binds abstract fun bindPump(pump: Thermosiphon): Pump
}