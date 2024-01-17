package coffee

import diy.FactoryHolderModule
import diy.ObjectGraph
import diy.InjectProcessorModule
import diy.ReflectiveModule
import diy.bind
import diy.get
import diy.install
import diy.installSingleton

fun main() {
  println("\nManual DI\n")
  val coffeeMaker1 = createWithManualDI()
  coffeeMaker1.brew()

  println("\nFactory Holder\n")
  val coffeeMaker2 = createWithFactoryHolderModule()
  coffeeMaker2.brew()

  println("\nFactory Holder Modules\n")
  val coffeeMaker3 = createWithFactoryHolderModules()
  coffeeMaker3.brew()

  println("\nReflective Module\n")
  val coffeeMaker4 = createWithReflectiveModule()
  coffeeMaker4.brew()

  println("\nInject Processor Module\n")
  val coffeeMaker5 = createWithInjectProcessorModule()
  coffeeMaker5.brew()
}

fun createWithManualDI(): CoffeeMaker {
  val logger = CoffeeLogger()
  val heater = ElectricHeater(logger)
  val pump = Thermosiphon(logger, heater)
  return CoffeeMaker(logger, heater, pump)
}

fun createWithFactoryHolderModule(): CoffeeMaker {
  val module = FactoryHolderModule()
  module.installSingleton {
    CoffeeLogger()
  }
  module.installSingleton<Heater> {
    ElectricHeater(get())
  }
  module.install<Pump> {
    Thermosiphon(get(), get())
  }
  module.install {
    CoffeeMaker(get(), get(), get())
  }

  val objectGraph = ObjectGraph(module)

  return objectGraph.get()
}

fun createWithFactoryHolderModules(): CoffeeMaker {
  val logModule = FactoryHolderModule()
  logModule.installSingleton {
    CoffeeLogger()
  }

  val partsModule = FactoryHolderModule()
  partsModule.installSingleton<Heater> {
    ElectricHeater(get())
  }
  partsModule.install<Pump> {
    Thermosiphon(get(), get())
  }

  val appModule = FactoryHolderModule()
  appModule.install {
    CoffeeMaker(get(), get(), get())
  }

  val objectGraph = ObjectGraph(logModule, partsModule, appModule)

  return objectGraph.get()
}

fun createWithReflectiveModule(): CoffeeMaker {
  val module = FactoryHolderModule()
  module.bind<Heater, ElectricHeater>()
  module.bind<Pump, Thermosiphon>()

  val objectGraph = ObjectGraph(module, ReflectiveModule())

  return objectGraph.get()
}

fun createWithInjectProcessorModule(): CoffeeMaker {
  val module = FactoryHolderModule()
  module.bind<Heater, ElectricHeater>()
  module.bind<Pump, Thermosiphon>()

  val objectGraph = ObjectGraph(module, InjectProcessorModule())

  return objectGraph.get()
}