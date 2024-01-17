import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import diy.Inject
import diy.Singleton

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
      .forEach { it.accept(InjectVisitor(), Unit) }
    return unprocessedSymbols
  }

  @OptIn(KspExperimental::class)
  inner class InjectVisitor : KSVisitorVoid() {
    override fun visitFunctionDeclaration(
      function: KSFunctionDeclaration,
      data: Unit
    ) {
      val injectedClass = function.parentDeclaration as KSClassDeclaration
      val injectedClassSimpleName = injectedClass.simpleName.asString()
      val packageName = injectedClass.containingFile!!.packageName.asString()
      val className = "${injectedClassSimpleName}_Factory"
      val file = codeGenerator.createNewFile(
        Dependencies(true, function.containingFile!!), packageName, className
      )
      file.appendLine("package $packageName")
      file.appendLine()
      file.appendLine("import diy.Factory")
      file.appendLine("import diy.ObjectGraph")

      if (injectedClass.isAnnotationPresent(Singleton::class)) {
        file.appendLine("import diy.singleton")
      }

      if (function.parameters.isNotEmpty()) {
        file.appendLine("import diy.get")
      }
      file.appendLine()
      file.appendLine("class $className : Factory<$injectedClassSimpleName> {")

      val constructorInvocation =
        "${injectedClassSimpleName}(" + function.parameters.joinToString(", ") {
          "objectGraph.get()"
        } + ")"

      if (injectedClass.isAnnotationPresent(Singleton::class)) {
        val linkerParameter = if (function.parameters.isNotEmpty()) "objectGraph ->" else ""
        file.appendLine("    private val singletonFactory = singleton { $linkerParameter")
        file.appendLine("        $constructorInvocation")
        file.appendLine("    }")
        file.appendLine()
        file.appendLine(
          "    override fun get(objectGraph: ObjectGraph) = singletonFactory.get(objectGraph)"
        )
      } else {
        file.appendLine("    override fun get(objectGraph: ObjectGraph) = $constructorInvocation")
      }
      file.appendLine("}")
      file.close()
    }
  }
}