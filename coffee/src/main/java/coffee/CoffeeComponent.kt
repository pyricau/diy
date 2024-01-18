package coffee

import diy.Binds
import diy.Component

@Component(modules = [CoffeeBindsModule::class])
interface CoffeeComponent {
  val coffeeMaker: CoffeeMaker
}

interface CoffeeBindsModule {
  @Binds fun bindHeater(heater: ElectricHeater): Heater
  @Binds fun bindPump(pump: Thermosiphon): Pump
}