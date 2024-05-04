package org.example

enum class Tag {
    OK,
    ERROR
}

abstract class ParsingResult {
    open fun readableForm(): String = toString()
}

data class CharParsingResult(val ch: Char) : ParsingResult() {
    override fun readableForm(): String {
        return ch.toString()
    }
}

data class NumberParsingResult(val number: Int) : ParsingResult() {
    override fun readableForm(): String {
        return number.toString()
    }
}

data class ConstantExpressionParsingResult(val negative: Boolean, val value: Int) : ParsingResult() {
    override fun readableForm(): String {
        return "Constant expression ${if (negative) "-" else ""}$value"
    }
}

data class BinaryExpressionParsingResult(val left: ParsingResult, val op: CharParsingResult, val right: ParsingResult) :
    ParsingResult() {
    override fun readableForm(): String {
        val leftLines = left.readableForm().lines().joinToString("\n") { "\t$it" }
        val rightLines = right.readableForm().lines().joinToString("\n") { "\t$it" }

        return "Binary expression:\n" +
                "(\n" +
                "${leftLines}\n" +
                "\t${op.readableForm()}\n" +
                "${rightLines}\n" +
                ")"
    }
}

data class SequenceParsingResult(val sequence: List<ParsingResult>) : ParsingResult()

data class Result(
    val tag: Tag,
    val result: ParsingResult?,
    val leftover: String
)

abstract class Parser {
    abstract fun parse(input: String): Result
}

class CharParser(private val ch: Char) : Parser() {
    override fun parse(input: String): Result {
        if (input.isEmpty()) return Result(Tag.ERROR, null, input)
        if (input[0] == ch) return Result(Tag.OK, CharParsingResult(ch), input.substring(1))
        return Result(Tag.ERROR, null, input)
    }
}

class NumberParser : Parser() {
    override fun parse(input: String): Result {
        if (input.isEmpty() || !input[0].isDigit()) return Result(Tag.ERROR, null, input)
        var i = 0;
        var result = 0
        while (i < input.length && input[i].isDigit()) {
            result = result * 10 + (input[i] - '0')
            i++
        }
        return Result(Tag.OK, NumberParsingResult(result), input.substring(i))
    }
}

class ConstantExpressionParser : Parser() {
    override fun parse(input: String): Result {
        val negativeParserResult = CharParser('-').parse(input)
        val negative = negativeParserResult.tag == Tag.OK
        val numberParsingResult = NumberParser().parse(negativeParserResult.leftover)
        if (numberParsingResult.tag == Tag.OK) {
            return Result(
                Tag.OK,
                ConstantExpressionParsingResult(negative, (numberParsingResult.result!! as NumberParsingResult).number),
                numberParsingResult.leftover
            )
        }
        return Result(Tag.ERROR, null, input)
    }
}

class BinaryExpressionParser : Parser() {
    override fun parse(input: String): Result {
        val openParenParser = CharParser('(')
        val leftParser = choice(ConstantExpressionParser(), BinaryExpressionParser())
        val opParser = choice(CharParser('+'), CharParser('-'), CharParser('*'))
        val rightParser = choice(ConstantExpressionParser(), BinaryExpressionParser())
        val closeParenParser = CharParser(')')
        val result = sequence(openParenParser, leftParser, opParser, rightParser, closeParenParser).parse(input)
        if (result.tag == Tag.OK) {
            return Result(
                Tag.OK, BinaryExpressionParsingResult(
                    (result.result!! as SequenceParsingResult).sequence[1],
                    (result.result as SequenceParsingResult).sequence[2] as CharParsingResult,
                    result.result.sequence[3]
                ), result.leftover
            )
        }
        return Result(Tag.ERROR, null, input)
    }
}

class ExpressionParser : Parser() {
    override fun parse(input: String): Result {
        val result = choice(BinaryExpressionParser(), ConstantExpressionParser()).parse(input)
        if (result.leftover.isNotEmpty()) {
            return Result(Tag.ERROR, null, result.leftover)
        }
        return result
    }
}

fun choice(vararg parsers: Parser): Parser {
    return object : Parser() {
        override fun parse(input: String): Result {
            for (parser in parsers) {
                val result = parser.parse(input)
                if (result.tag == Tag.OK) return result
            }
            return Result(Tag.ERROR, null, input)
        }
    }
}

fun sequence(vararg parsers: Parser): Parser {
    return object : Parser() {
        override fun parse(input: String): Result {
            val results = mutableListOf<ParsingResult>()
            var previousResult = Result(Tag.OK, null, input)
            for (parser in parsers) {
                val result = parser.parse(previousResult.leftover)
                if (result.tag == Tag.ERROR) return result
                results.add(result.result!!)
                previousResult = result
            }
            return Result(Tag.OK, SequenceParsingResult(results), previousResult.leftover)
        }
    }
}


fun main() {
    val expressionParser = ExpressionParser()
    println(expressionParser.parse("(1--2)").result!!.readableForm())
    println(expressionParser.parse("((1+2)*(3+4))").result!!.readableForm())
}

