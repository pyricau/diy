import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import diy.Binds
import diy.Component
import diy.Inject
import diy.Singleton
import java.io.OutputStream
import kotlin.reflect.KClass

class ComponentProcessorProvider : SymbolProcessorProvider {
  override fun create(
    environment: SymbolProcessorEnvironment
  ): SymbolProcessor {
    return ComponentProcessor(environment.codeGenerator)
  }
}

data class ComponentFactory(
  val type: KSClassDeclaration,
  val constructorParameters: List<KSDeclaration>,
  val isSingleton: Boolean
)

data class EntryPoint(
  val propertyDeclaration: KSPropertyDeclaration,
  val propertyType: KSDeclaration
)

class ComponentModel(
  val packageName: String,
  val imports: Set<String>,
  val className: String,
  val componentInterfaceName: String,
  val factories: List<ComponentFactory>,
  val binds: Map<KSDeclaration, KSDeclaration>,
  val entryPoints: List<EntryPoint>
)

class ComponentProcessor(
  val codeGenerator: CodeGenerator
) : SymbolProcessor {

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val annotatedSymbols = resolver.getSymbolsWithAnnotation(Component::class.java.name)
    val unprocessedSymbols = annotatedSymbols.filter { !it.validate() }.toList()
    annotatedSymbols
      .filterIsInstance<KSClassDeclaration>()
      .filter { it.validate() }
      .forEach { it.accept(ComponentVisitor(), Unit) }
    return unprocessedSymbols
  }

  @OptIn(KspExperimental::class)
  inner class ComponentVisitor : KSVisitorVoid() {
    override fun visitClassDeclaration(
      classDeclaration: KSClassDeclaration,
      data: Unit
    ) {
      val componentInterfaceName = classDeclaration.simpleName.asString()
      val packageName = classDeclaration.containingFile!!.packageName.asString()
      val className = "Generated${componentInterfaceName}"

      val componentAnnotation = classDeclaration.annotations.single {
        it isInstance Component::class
      }

      val entryPoints = readEntryPoints(classDeclaration)

      val entryPointTypes = entryPoints.map { it.propertyType }

      val binds = readBinds(componentAnnotation)

      val bindProvidedTypes = binds.values
      val factories = traverseDependencyGraph(entryPointTypes + bindProvidedTypes)

      val importDeclarations = entryPointTypes + bindProvidedTypes + factories.map { it.type }
      val actualImports = importDeclarations
        .filter { it.packageName != classDeclaration.packageName }
        .map { it.qualifiedName!!.asString() }.toSet() +
        if (factories.any { it.isSingleton }) {
          setOf("diy.componentSingleton")
        } else emptySet()

      val model = ComponentModel(
        packageName = packageName,
        imports = actualImports,
        className = className,
        componentInterfaceName = componentInterfaceName,
        factories = factories,
        binds = binds,
        entryPoints = entryPoints,
      )

      codeGenerator.createNewFile(
        Dependencies(true, classDeclaration.containingFile!!), packageName, className
      ).use { ktFile ->
        generateComponent(model, ktFile)
      }
    }

    private fun readEntryPoints(classDeclaration: KSClassDeclaration) =
      classDeclaration.getDeclaredProperties().map { property ->
        val resolvedPropertyType = property.type.resolve().declaration
        EntryPoint(property, resolvedPropertyType)
      }.toList()

    private fun readBinds(componentAnnotation: KSAnnotation): Map<KSDeclaration, KSDeclaration> {
      val bindModules = componentAnnotation.getArgument("modules").value as List<KSType>
      val binds = bindModules
        .map { it.declaration as KSClassDeclaration }
        .flatMap { it.getDeclaredFunctions() }
        .filter { it.isAnnotationPresent(Binds::class) }
        .associate { function ->
          val resolvedReturnType = function.returnType!!.resolve().declaration
          val resolvedParamType = function.parameters.single().type.resolve().declaration
          resolvedReturnType to resolvedParamType
        }
      return binds
    }

    private fun traverseDependencyGraph(factoryEntryPoints: List<KSDeclaration>): List<ComponentFactory> {
      val typesToProcess = mutableListOf<KSDeclaration>()
      typesToProcess += factoryEntryPoints

      val factories = mutableListOf<ComponentFactory>()
      val typesVisited = mutableListOf<KSDeclaration>()
      while (typesToProcess.isNotEmpty()) {
        val visitedClassDeclaration = typesToProcess.removeFirst() as KSClassDeclaration
        if (visitedClassDeclaration !in typesVisited) {
          typesVisited += visitedClassDeclaration
          val injectConstructors = visitedClassDeclaration.getConstructors()
            .filter { it.isAnnotationPresent(Inject::class) }
            .toList()
          check(injectConstructors.size < 2) {
            "There should be a most one @Inject constructor"
          }
          if (injectConstructors.isNotEmpty()) {
            val injectConstructor = injectConstructors.first()
            val constructorParams =
              injectConstructor.parameters.map { it.type.resolve().declaration }
            typesToProcess += constructorParams
            val isSingleton = visitedClassDeclaration.isAnnotationPresent(Singleton::class)
            factories += ComponentFactory(visitedClassDeclaration, constructorParams, isSingleton)
          }
        }
      }
      return factories
    }

    private fun generateComponent(
      model: ComponentModel,
      ktFile: OutputStream
    ) {
      with(model) {
        ktFile.appendLine("package $packageName")
        ktFile.appendLine()


        imports.forEach { import ->
          ktFile.appendLine("import $import")
        }

        ktFile.appendLine()
        ktFile.appendLine("class $className : $componentInterfaceName {")

        factories.forEach { (classDeclaration, parameterDeclarations, isSingleton) ->
          val name = classDeclaration.simpleName.asString()
          val parameters = parameterDeclarations.map { requestedType ->
            val providedType = binds[requestedType] ?: requestedType
            providedType.simpleName.asString()
          }
          val singleton = if (isSingleton) "componentSingleton " else ""

          ktFile.appendLine("    private val provide$name = $singleton{")
          ktFile.appendLine("        $name(${parameters.joinToString(", ") { "provide$it()" }})")
          ktFile.appendLine("    }")
        }

        entryPoints.forEach { (propertyDeclaration, type) ->
          val name = propertyDeclaration.simpleName.asString()
          val typeSimpleName = type.simpleName.asString()
          ktFile.appendLine("    override val $name: $typeSimpleName")
          ktFile.appendLine("      get() = provide$typeSimpleName()")
        }
        ktFile.appendLine("}")
      }
    }
  }
}

infix fun KSAnnotation.isInstance(annotationKClass: KClass<*>): Boolean {
  return shortName.getShortName() == annotationKClass.simpleName &&
    annotationType.resolve().declaration.qualifiedName?.asString() == annotationKClass.qualifiedName
}

fun KSAnnotation.getArgument(name: String): KSValueArgument {
  return arguments.single { it.name?.asString() == "modules" }
}