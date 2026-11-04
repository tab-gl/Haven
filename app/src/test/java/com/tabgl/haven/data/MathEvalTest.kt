package com.tabgl.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MathEvalTest {

    private fun eval(input: String) = MathEval.evaluate(input)?.let { MathEval.format(it) }

    @Test
    fun arithmeticFollowsPrecedence() {
        assertEquals("14", eval("2+3*4"))
        assertEquals("20", eval("(2+3)*4"))
        assertEquals("2", eval("10-8"))
        assertEquals("2.5", eval("5/2"))
    }

    @Test
    fun powerIsRightAssociative() {
        assertEquals("512", eval("2^3^2"))
        assertEquals("8", eval("2^3"))
    }

    @Test
    fun unaryMinusWorks() {
        assertEquals("-5", eval("-5"))
        assertEquals("1", eval("3+-2"))
        assertEquals("-8", eval("-2^3"))
    }

    @Test
    fun moduloIsSupported() {
        assertEquals("1", eval("7%3"))
    }

    @Test
    fun decimalsRoundTripWithoutTrailingZeros() {
        assertEquals("0.5", eval("1/2"))
        assertEquals("0.33333333", eval("1/3"))
        assertEquals("100", eval("10*10"))
    }

    @Test
    fun divisionByZeroIsRefused() {
        assertNull(MathEval.evaluate("5/0"))
    }

    @Test
    fun incompleteExpressionsAreRefused() {
        assertNull(MathEval.evaluate("2+"))
        assertNull(MathEval.evaluate("(2+3"))
        assertNull(MathEval.evaluate("2 3 +"))
        assertNull(MathEval.evaluate(""))
    }

    @Test
    fun onlyArithmeticIsEverEvaluated() {
        // Nothing outside the arithmetic alphabet may reach the parser.
        assertFalse(MathEval.looksLikeMath("rm -rf /"))
        assertFalse(MathEval.looksLikeMath("Runtime.getRuntime()"))
        assertNull(MathEval.evaluate("java.lang.Runtime"))
    }

    @Test
    fun looksLikeMathNeedsDigitsAndAnOperator() {
        assertTrue(MathEval.looksLikeMath("12*7"))
        assertFalse(MathEval.looksLikeMath("12"))
        assertFalse(MathEval.looksLikeMath("camera"))
        assertFalse(MathEval.looksLikeMath("12+"))
    }

    @Test
    fun formatKeepsIntegersClean() {
        assertEquals("42", MathEval.format(42.0))
        assertEquals("-7", MathEval.format(-7.0))
        assertEquals("0.25", MathEval.format(0.25))
    }
}
