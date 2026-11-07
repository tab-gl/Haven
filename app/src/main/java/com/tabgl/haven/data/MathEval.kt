package com.tabgl.haven.data

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * A tiny, allocation-conscious arithmetic evaluator for the search box.
 *
 * Deliberately not `ScriptEngine` and not `eval`: only digits, the five operators, `%` as modulo,
 * `^` as power and parentheses are accepted, so a pasted string can never execute anything.
 */
object MathEval {

    private val ALLOWED = Regex("^[0-9+\\-*/%^().\\s]+$")

    /** True when [input] is worth showing a calculator result for. */
    fun looksLikeMath(input: String): Boolean {
        val s = input.trim()
        if (s.length < 3 || s.length > 64) return false
        if (!ALLOWED.matches(s)) return false
        if (s.none { it.isDigit() }) return false
        if (s.none { it in "+-*/%^" }) return false
        // A trailing operator ("12+") is still being typed; don't flash a result yet.
        if (s.last() in "+-*/%^") return false
        return s.any { it.isDigit() }
    }

    /** Evaluates [input], or null when it is not a complete expression (or divides by zero). */
    fun evaluate(input: String): Double? = try {
        val parser = Parser(input)
        val value = parser.parseExpression()
        parser.skipSpaces()
        when {
            !parser.atEnd() -> null
            value.isNaN() || value.isInfinite() -> null
            else -> value
        }
    } catch (e: RuntimeException) {
        null
    }

    /** Formats a result the way a calculator would: no trailing zeros, no exponent soup. */
    fun format(value: Double): String {
        if (abs(value) >= 1e15) return BigDecimal(value).setScale(4, RoundingMode.HALF_UP).toPlainString()
        val rounded = BigDecimal(value).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
        return rounded.toPlainString()
    }

    private class Parser(private val text: String) {
        private var index = 0

        fun atEnd(): Boolean = index >= text.length

        fun skipSpaces() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '+' -> { index++; value += parseTerm() }
                    '-' -> { index++; value -= parseTerm() }
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parsePower()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '*' -> { index++; value *= parsePower() }
                    '/' -> { index++; value /= parsePower() }
                    '%' -> { index++; value %= parsePower() }
                    else -> return value
                }
            }
        }

        private fun parsePower(): Double {
            val base = parseUnary()
            skipSpaces()
            if (peek() == '^') {
                index++
                return Math.pow(base, parsePower())
            }
            return base
        }

        private fun parseUnary(): Double {
            skipSpaces()
            return when (peek()) {
                '-' -> { index++; -parseUnary() }
                '+' -> { index++; parseUnary() }
                else -> parsePrimary()
            }
        }

        private fun parsePrimary(): Double {
            skipSpaces()
            if (peek() == '(') {
                index++
                val value = parseExpression()
                skipSpaces()
                if (peek() != ')') throw IllegalArgumentException("unbalanced")
                index++
                return value
            }
            val start = index
            while (index < text.length && (text[index].isDigit() || text[index] == '.')) index++
            if (start == index) throw IllegalArgumentException("expected a number")
            return text.substring(start, index).toDoubleOrNull()
                ?: throw IllegalArgumentException("bad number")
        }

        private fun peek(): Char? = if (index < text.length) text[index] else null
    }
}
