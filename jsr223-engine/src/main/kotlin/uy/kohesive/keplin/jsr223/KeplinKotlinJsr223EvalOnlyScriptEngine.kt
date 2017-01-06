package uy.kohesive.keplin.jsr223

import uy.kohesive.keplin.kotlin.core.scripting.ReplRepeatingMode
import uy.kohesive.keplin.kotlin.core.scripting.ResettableRepl
import uy.kohesive.keplin.kotlin.core.scripting.ScriptArgsWithTypes
import uy.kohesive.keplin.kotlin.util.scripting.findClassJars
import uy.kohesive.keplin.kotlin.util.scripting.findKotlinCompilerJars
import uy.kohesive.keplin.kotlin.util.scripting.resolver.AnnotationTriggeredScriptDefinition
import uy.kohesive.keplin.kotlin.util.scripting.resolver.local.JarFileScriptDependenciesResolver
import uy.kohesive.keplin.util.scripting.resolver.maven.MavenScriptDependenciesResolver
import java.io.Reader
import javax.script.*

class KeplinKotlinJsr223EvalOnlyScriptEngine(val factory: KelpinKotlinJsr223EvalOnlyEngineFactory,
                                             defaultImports: List<String> = emptyList())
    : AbstractScriptEngine() {
    private val extraClasspath = findClassJars(ResettableRepl::class) +
            findKotlinCompilerJars(false)

    private val engine: ResettableRepl = ResettableRepl(
            moduleName = "KeplinKotlinJsr223-${System.currentTimeMillis()}",
            additionalClasspath = extraClasspath,
            repeatingMode = ReplRepeatingMode.NONE,
            scriptDefinition = AnnotationTriggeredScriptDefinition(
                    definitionName = "KeplinKotlinJsr223",
                    template = KeplinKotlinJsr223ScriptTemplate::class,
                    defaultEmptyArgs = KeplinKotlinJsr223ScriptTemplateEmptyArgs,
                    resolvers = listOf(JarFileScriptDependenciesResolver(), MavenScriptDependenciesResolver()),
                    defaultImports = defaultImports)
    )

    fun fixupArgsForScriptTemplate() {
        engine.defaultScriptArgs = ScriptArgsWithTypes(arrayOf<Any?>(engine, getContext()), KeplinKotlinJsr223ScriptTemplateArgTypes)
    }

    // TODO: capture input/output to the correct place for the script context

    override fun eval(script: String, context: ScriptContext): Any? {
        return engine.compileAndEval(engine.nextCodeLine(script),
                ScriptArgsWithTypes(arrayOf<Any?>(engine, context),
                        KeplinKotlinJsr223ScriptTemplateArgTypes)).resultValue
    }

    override fun eval(reader: Reader, context: ScriptContext): Any? {
        return eval(reader.use(Reader::readText), context)
    }

    override fun createBindings(): Bindings = SimpleBindings()
    override fun getFactory(): ScriptEngineFactory = factory
}

