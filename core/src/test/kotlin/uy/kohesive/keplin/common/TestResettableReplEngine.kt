@file:Suppress("DEPRECATION")

package uy.kohesive.keplin.common

import org.junit.Test
import uy.kohesive.keplin.kotlin.core.scripting.ReplCompilerException
import uy.kohesive.keplin.kotlin.core.scripting.ReplEvalRuntimeException
import uy.kohesive.keplin.kotlin.core.scripting.ResettableRepl
import uy.kohesive.keplin.kotlin.util.scripting.findClassJars
import uy.kohesive.keplin.kotlin.util.scripting.findKotlinCompilerJars
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.junit.JUnitAsserter.fail

class TestResettableReplEngine {
    @Test
    fun testBasicScript() {
        ResettableRepl().use { repl ->
            val line1 = repl.nextCodeLine("val x = 10")
            val checkResult1 = repl.check(line1)

            assertEquals(line1, checkResult1.codeLine)
            assertTrue(checkResult1.isComplete)

            val compileResult1 = repl.compile(line1)

            assertEquals(line1, compileResult1.codeLine)
            assertFalse(compileResult1.compilerData.hasResult)

            val evalResult1 = repl.eval(compileResult1)

            assertEquals(line1, evalResult1.codeLine)
            assertEquals(Unit, evalResult1.resultValue)
        }
    }

    @Test
    fun testAtomicCompileAndEval() {
        ResettableRepl().use { repl ->
            repl.compileAndEval(repl.nextCodeLine("val x = 10"))
            val evalResult = repl.compileAndEval("x")
            assertEquals(10, evalResult.resultValue)
        }
    }

    @Test
    fun testResettingHistory() {
        ResettableRepl().use { repl ->
            val line1 = repl.nextCodeLine("val x = 10")
            repl.eval(repl.compile(line1))

            val line2 = repl.nextCodeLine("val y = x + 10")
            repl.eval(repl.compile(line2))

            val line3 = repl.nextCodeLine("val x = 30")
            repl.eval(repl.compile(line3))

            val line4 = repl.nextCodeLine("x")
            val evalResult4 = repl.eval(repl.compile(line4))

            assertEquals(30, evalResult4.resultValue)

            val line5 = repl.nextCodeLine("println(\"value of X is \$x\")")
            repl.eval(repl.compile(line5))

            // TODO: this doesn't evaluate correctly for the println before the assignment, it references the new C being constructed not the old X from previous script lines
            val line6 = repl.nextCodeLine("println(\"value of X is \$x\"); val x = 1000")
            repl.eval(repl.compile(line6))

            try {
                val removedLines = repl.resetToLine(line2)
                assertEquals(listOf(line6, line5, line4, line3), removedLines)

                val newLine3 = repl.nextCodeLine("x")

                val newCompileResult3 = repl.compile(newLine3)
                val newEvalResult3 = repl.eval(newCompileResult3)

                assertEquals(10, newEvalResult3.resultValue)

                val newLine4 = repl.nextCodeLine("x + 10")
                val newCompileResult4 = repl.compile(newLine4)
                val newEvalResult4 = repl.eval(newCompileResult4)

                assertEquals(20, newEvalResult4.resultValue)

                // TODO: why does this println print "0" instead of "10"
                val newLine5 = repl.nextCodeLine("println(x); val x = 99; println(x)")
                repl.eval(repl.compile(newLine5))

                val newLine6 = repl.nextCodeLine("x")
                val newCompileResult6 = repl.compile(newLine6)
                val newEvalResult6 = repl.eval(newCompileResult6)

                assertEquals(99, newEvalResult6.resultValue)

                val removedNewLines = repl.resetToLine(line2)
                assertEquals(listOf(newLine6, newLine5, newLine4, newLine3), removedNewLines)

                val finalLine3 = repl.nextCodeLine("x")
                val finalCompileResult3 = repl.compile(finalLine3)
                val finalEvalResult3 = repl.eval(finalCompileResult3)
                assertEquals(10, finalEvalResult3.resultValue)
            } catch (ex: Exception) {
                ex.printStackTrace()
                throw ex
            }
        }
    }


    @Test
    fun testRecursingScriptsDifferentEngines() {
        val extraClasspath = findClassJars(ResettableRepl::class) +
                findKotlinCompilerJars(false)

        ResettableRepl(additionalClasspath = extraClasspath).use { repl ->
            val outerEval = repl.compileAndEval("""
                 import uy.kohesive.keplin.kotlin.core.scripting.ResettableRepl
                 import uy.kohesive.keplin.kotlin.util.scripting.*

                 val extraClasspath =  findClassJars(ResettableRepl::class) +
                                       findKotlinCompilerJars(false)
                 val result = ResettableRepl(additionalClasspath = extraClasspath).use { repl ->
                    val innerEval = repl.compileAndEval("println(\"inner world\"); 100")
                    innerEval.resultValue
                 }
                 result
            """)
            assertEquals(100, outerEval.resultValue)
        }
    }


    @Test
    fun testCompileAllFirstEvalAllLast() {
        ResettableRepl().use { repl ->
            val line1 = repl.compile(repl.nextCodeLine("""val x = 1"""))
            val line2 = repl.compile(repl.nextCodeLine("""val y = 2"""))
            val line3 = repl.compile(repl.nextCodeLine("""x+y"""))

            repl.eval(line1)
            repl.eval(line2)
            val result = repl.eval(line3)
            assertEquals(3, result.resultValue)
        }
    }

    @Test(expected = ReplCompilerException::class)
    fun testCompileInOrderThenEvalOutOfOrderError() {
        ResettableRepl().use { repl ->
            val line1 = repl.compile(repl.nextCodeLine("""val x = 1"""))
            val line2 = repl.compile(repl.nextCodeLine("""val y = 2"""))
            val line3 = repl.compile(repl.nextCodeLine("""x+y"""))

            repl.eval(line1)
            repl.eval(line3)
            repl.eval(line2)
        }
    }

    @Test
    fun testOneShotUnitEval() {
        ResettableRepl().use { repl ->
            repl.compileAndEval("""println("hello world")""")
        }
    }

    @Test
    fun testOneShotAssignEval() {
        ResettableRepl().use { repl ->
            repl.compileAndEval("""val x = 10""")
        }
    }

    @Test
    fun testBasicCompilerErrors() {
        ResettableRepl().use { repl ->
            try {
                repl.compileAndEval("java.util.Xyz()")
            } catch (ex: ReplCompilerException) {
                assertTrue("unresolved reference: Xyz" in ex.message!!)
            }
        }
    }

    @Test
    fun testBasicRuntimeErrors() {
        ResettableRepl().use { repl ->
            try {
                repl.compileAndEval("val x: String? = null; x!!")
            } catch (ex: ReplEvalRuntimeException) {
                assertTrue("NullPointerException" in ex.message!!)
            }
        }
    }

    @Test
    fun testResumeAfterCompilerError() {
        ResettableRepl().use { repl ->
            try {
                repl.compileAndEval("val x = 10")
                try {
                    repl.compileAndEval("java.util.fish")
                    fail("Expected compile error")
                } catch (ex: ReplCompilerException) {
                    // nop
                }

                val result = repl.compileAndEval("x")
                assertEquals(10, result.resultValue)
            } catch (ex: ReplEvalRuntimeException) {
                assertTrue("NullPointerException" in ex.message!!)
            }
        }
    }

    @Test
    fun testResumeAfterRuntimeError() {
        ResettableRepl().use { repl ->
            try {
                repl.compileAndEval("val y = 100")
                repl.compileAndEval("val x: String? = null")
                try {
                    repl.compileAndEval("x!!")
                    fail("Expected runtime error")
                } catch (ex: ReplEvalRuntimeException) {
                    // nop
                }

                val result = repl.compileAndEval("\"\$x \$y\"")
                assertEquals("null 100", result.resultValue)
            } catch (ex: ReplEvalRuntimeException) {
                assertTrue("NullPointerException" in ex.message!!)
            }
        }
    }
}