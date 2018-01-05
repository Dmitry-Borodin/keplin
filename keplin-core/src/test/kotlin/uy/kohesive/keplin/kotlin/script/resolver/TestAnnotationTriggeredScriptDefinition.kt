package uy.kohesive.keplin.kotlin.script.resolver

import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.reflect.KClass

class TestAnnotationTriggeredScriptDefinition {
    private val EMPTY_SCRIPT_ARGS: Array<out Any?> = arrayOf(emptyArray<String>())
    private val EMPTY_SCRIPT_ARGS_TYPES: Array<out KClass<out Any>> = arrayOf(Array<String>::class)
    abstract class ScriptTemplateWithDisplayHelpers(val args: kotlin.Array<kotlin.String>) {
        fun resultOf(vararg mimeToData: Pair<String, Any>): MimeTypedResult = MimeTypedResult(mapOf(*mimeToData))
    }
    class MimeTypedResult(mimeData: Map<String, Any>) : Map<String, Any> by mimeData

    @Test
    fun testCanCreateWithoutException() {
        val annotationTriggeredScriptDefinition = AnnotationTriggeredScriptDefinition(
                "varargTemplateWithMavenResolving",
                ScriptTemplateWithDisplayHelpers::class,
                ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES),
                listOf(JarFileScriptDependenciesResolver()))

        assertNotNull(annotationTriggeredScriptDefinition)
        assertNotNull(AnnotationTriggeredScriptDefinition.log)
    }

}