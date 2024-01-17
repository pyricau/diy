package coffee

import diy.Linker
import diy.get
import diy.install
import diy.installSingleton

fun main() {
  manualDI()
  simpleLinker()
  simpleLinkerWithModules()
}

fun manualDI() {
  println("\nManual DI\n")
  val logger = CoffeeLogger()
  val heater = ElectricHeater(logger)
  val pump = Thermosiphon(logger, heater)
  val coffeeMaker = CoffeeMaker(logger, heater, pump)

  coffeeMaker.brew()
}

fun simpleLinker() {
  println("\nSimple Linker\n")
  val linker = Linker()
  linker.installSingleton {
    CoffeeLogger()
  }
  linker.installSingleton<Heater> {
    ElectricHeater(get())
  }
  linker.install<Pump> {
    Thermosiphon(get(), get())
  }
  linker.install {
    CoffeeMaker(get(), get(), get())
  }

  val maker = linker.get<CoffeeMaker>()
  maker.brew()
}

fun simpleLinkerWithModules() {
  println("\nSimple Linker with modules\n")
  val logModule = Linker()
  logModule.installSingleton {
    CoffeeLogger()
  }

  val partsModule = Linker()
  partsModule.installSingleton<Heater> {
    ElectricHeater(get())
  }
  partsModule.install<Pump> {
    Thermosiphon(get(), get())
  }

  val appModule = Linker()
  appModule.install {
    CoffeeMaker(get(), get(), get())
  }

  appModule.factories += logModule.factories + partsModule.factories

  val maker = appModule.get<CoffeeMaker>()
  maker.brew()
}

