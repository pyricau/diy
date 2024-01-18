import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import javax.inject.Inject
import javax.inject.Singleton

class InjectProcessorProvider : SymbolProcessorProvider {
  override fun create(
    environment: SymbolProcessorEnvironment
  ): SymbolProcessor {
    return InjectProcessor(environment.codeGenerator)
  }
}

class InjectProcessor(
  val codeGenerator: CodeGenerator
) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    val annotatedSymbols = resolver.getSymbolsWithAnnotation(Inject::class.java.name)
    val unprocessedSymbols = annotatedSymbols.filter { !it.validate() }.toList()
    annotatedSymbols
      .filter { it is KSFunctionDeclaration && it.validate() }
      .forEach { it.accept(InjectConstructorVisitor(), Unit) }
    return unprocessedSymbols
  }

  @OptIn(KspExperimental::class)
  inner class InjectConstructorVisitor : KSVisitorVoid() {
    override fun visitFunctionDeclaration(
      function: KSFunctionDeclaration,
      data: Unit
    ) {
      val injectedClass = function.parentDeclaration as KSClassDeclaration
      val injectedClassSimpleName = injectedClass.simpleName.asString()
      val packageName = injectedClass.containingFile!!.packageName.asString()
      val className = "${injectedClassSimpleName}_Factory"
      codeGenerator.createNewFile(
        Dependencies(true, function.containingFile!!), packageName, className
      ).use { ktFile ->
        ktFile.appendLine("package $packageName")
        ktFile.appendLine()
        ktFile.appendLine("import diy.Factory")
        ktFile.appendLine("import diy.ObjectGraph")

        if (injectedClass.isAnnotationPresent(Singleton::class)) {
          ktFile.appendLine("import diy.singleton")
        }

        if (function.parameters.isNotEmpty()) {
          ktFile.appendLine("import diy.get")
        }
        ktFile.appendLine()
        ktFile.appendLine("class $className : Factory<$injectedClassSimpleName> {")

        val constructorInvocation =
          "${injectedClassSimpleName}(" + function.parameters.joinToString(", ") {
            "objectGraph.get()"
          } + ")"

        if (injectedClass.isAnnotationPresent(Singleton::class)) {
          val linkerParameter = if (function.parameters.isNotEmpty()) "objectGraph ->" else ""
          ktFile.appendLine("    private val singletonFactory = singleton { $linkerParameter")
          ktFile.appendLine("        $constructorInvocation")
          ktFile.appendLine("    }")
          ktFile.appendLine()
          ktFile.appendLine(
            "    override fun get(objectGraph: ObjectGraph) = singletonFactory.get(objectGraph)"
          )
        } else {
          ktFile.appendLine(
            "    override fun get(objectGraph: ObjectGraph) = $constructorInvocation"
          )
        }
        ktFile.appendLine("}")
      }
    }
  }
}