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
import kotlin.reflect.KClass

class ComponentProcessorProvider : SymbolProcessorProvider {
  override fun create(
    environment: SymbolProcessorEnvironment
  ): SymbolProcessor {
    return ComponentProcessor(environment.codeGenerator)
  }
}

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

      codeGenerator.createNewFile(
        Dependencies(true, classDeclaration.containingFile!!), packageName, className
      ).use { ktFile ->

        ktFile.appendLine("package $packageName")
        ktFile.appendLine()

        val componentAnnotation = classDeclaration.annotations.single {
          it isInstance Component::class
        }

        val modulesArgument = componentAnnotation.getArgument("modules")

        val modules = modulesArgument.value as List<KSType>

        val importDeclarations = mutableSetOf<KSDeclaration>()
        val imports = mutableSetOf<String>()

        val entryPoints = classDeclaration.getDeclaredProperties().map { property ->
          val resolvedPropertyType = property.type.resolve().declaration
          importDeclarations += resolvedPropertyType

          property.simpleName.asString() to resolvedPropertyType
        }

        val typesToProcess = entryPoints.map { it.second }.toMutableList()

        val binds = modules
          .map { it.declaration as KSClassDeclaration }
          .flatMap { it.getDeclaredFunctions() }
          .filter { it.isAnnotationPresent(Binds::class) }
          .associate { function ->
            val resolvedReturnType = function.returnType!!.resolve().declaration
            val resolvedParamType = function.parameters.single().type.resolve().declaration
            importDeclarations += resolvedParamType
            typesToProcess += resolvedParamType
            resolvedReturnType to resolvedParamType
          }

        data class ComponentFactory(
          val type: KSClassDeclaration,
          val constructorParameters: List<KSDeclaration>,
          val isSingleton: Boolean
        )

        val factories = mutableListOf<ComponentFactory>()
        val typesVisited = mutableListOf<KSDeclaration>()

        while (typesToProcess.isNotEmpty()) {
          val classDeclaration = typesToProcess.removeFirst() as KSClassDeclaration
          if (classDeclaration !in typesVisited) {
            typesVisited += classDeclaration
            val injectConstructors = classDeclaration.getConstructors()
              .filter { it.isAnnotationPresent(Inject::class) }
              .toList()
            check(injectConstructors.size < 2) {
              "There should be a most one @Inject constructor"
            }
            if (injectConstructors.isNotEmpty()) {
              val injectConstructor = injectConstructors.first()
              importDeclarations += classDeclaration
              val constructorParams =
                injectConstructor.parameters.map { it.type.resolve().declaration }
              typesToProcess += constructorParams
              val isSingleton = classDeclaration.isAnnotationPresent(Singleton::class)
              if (isSingleton) {
                imports += "diy.componentSingleton"
              }
              factories += ComponentFactory(classDeclaration, constructorParams, isSingleton)
            }
          }
        }

        imports += importDeclarations
          .filter { it.packageName != classDeclaration.packageName }
          .map { it.qualifiedName!!.asString() }

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

        entryPoints.forEach { (name, type) ->
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