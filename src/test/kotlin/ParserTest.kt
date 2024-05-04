import org.example.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class ParserTest {
    @Test
    fun testCharParser() {
        val parser = CharParser('a')
        val result1 = parser.parse("abc")
        assertEquals(Tag.OK, result1.tag)
        assertEquals(CharParsingResult('a'), result1.result)
        assertEquals("bc", result1.leftover)

        val result2 = parser.parse("a")
        assertEquals(Tag.OK, result2.tag)
        assertEquals(CharParsingResult('a'), result2.result)
        assertEquals( "", result2.leftover)

        val result3 = parser.parse("bc")
        assertEquals(Tag.ERROR, result3.tag)
        assertNull(result3.result)
        assertEquals("bc", result3.leftover)
    }

    @Test
    fun testNumberParser() {
        val parser = NumberParser()
        val result1 = parser.parse("abc")
        assertEquals(Tag.ERROR, result1.tag)
        assertNull(result1.result)
        assertEquals("abc", result1.leftover)

        val result2 = parser.parse("123")
        assertEquals(Tag.OK, result2.tag)
        assertEquals(NumberParsingResult(123), result2.result)
        assertEquals("", result2.leftover)

        val result3 = parser.parse("123abc")
        assertEquals(Tag.OK, result3.tag)
        assertEquals(NumberParsingResult(123), result3.result)
        assertEquals("abc", result3.leftover)

        val result4 = parser.parse("abc123")
        assertEquals(Tag.ERROR, result4.tag)
        assertNull(result4.result)
        assertEquals("abc123", result4.leftover)

        val result5 = parser.parse("-123")
        assertEquals(Tag.ERROR, result5.tag)
        assertNull(result5.result)
        assertEquals("-123", result5.leftover)
    }

    @Test
    fun testChoiceParser() {
        val parser1 = CharParser('a')
        val parser2 = CharParser('b')
        val parser3 = CharParser('c')
        val parser = choice(parser1, parser2, parser3)
        val result1 = parser.parse("abc")
        assertEquals(Tag.OK, result1.tag)
        assertEquals(CharParsingResult('a'), result1.result)
        assertEquals("bc", result1.leftover)

        val result2 = parser.parse("a")
        assertEquals(Tag.OK, result2.tag)
        assertEquals(CharParsingResult('a'), result2.result)
        assertEquals("", result2.leftover)

        val result3 = parser.parse("bca")
        assertEquals(Tag.OK, result3.tag)
        assertEquals( CharParsingResult('b'), result3.result)
        assertEquals("ca", result3.leftover)

        val result4 = parser.parse("da")
        assertEquals(Tag.ERROR, result4.tag, )
        assertNull(result4.result)
        assertEquals("da", result4.leftover)
    }

    @Test
    fun testSequenceParser() {
        val parser1 = CharParser('a')
        val parser2 = CharParser('b')
        val parser3 = CharParser('c')
        val parser = sequence(parser1, parser2, parser3)
        val result1 = parser.parse("abc")
        assertEquals(Tag.OK, result1.tag)
        assertEquals(SequenceParsingResult(listOf(CharParsingResult('a'), CharParsingResult('b'), CharParsingResult('c'))), result1.result)
        assertEquals( "", result1.leftover)

        val result2 = parser.parse("a")
        assertEquals(result2.tag, Tag.ERROR)
        assertNull(result2.result)
        assertEquals("", result2.leftover)

        val result3 = parser.parse("bca")
        assertEquals(Tag.ERROR, result3.tag)
        assertNull(result3.result)
        assertEquals("bca", result3.leftover)

        val result4 = parser.parse("acc")
        assertEquals(Tag.ERROR, result4.tag)
        assertNull(result4.result)
        assertEquals("cc", result4.leftover)
    }

    @Test
    fun testConstantExpressionParser() {
        val parser = ConstantExpressionParser()
        val result1 = parser.parse("abc")
        assertEquals(Tag.ERROR, result1.tag)
        assertNull(result1.result)
        assertEquals("abc", result1.leftover)

        val result2 = parser.parse("123")
        assertEquals(Tag.OK, result2.tag)
        assertEquals(ConstantExpressionParsingResult(false, 123), result2.result)
        assertEquals("", result2.leftover)

        val result3 = parser.parse("123abc")
        assertEquals(Tag.OK, result3.tag)
        assertEquals(ConstantExpressionParsingResult(false, 123), result3.result)
        assertEquals("abc", result3.leftover)

        val result4 = parser.parse("-123")
        assertEquals(Tag.OK, result4.tag)
        assertEquals(ConstantExpressionParsingResult(true, 123), result4.result)
        assertEquals("", result4.leftover)

        val result5 = parser.parse("-123abc")
        assertEquals(Tag.OK, result5.tag)
        assertEquals(ConstantExpressionParsingResult(true, 123), result5.result)
        assertEquals("abc", result5.leftover)

        val result6 = parser.parse("-abc123")
        assertEquals(Tag.ERROR, result6.tag)
        assertNull(result6.result)
        assertEquals("-abc123", result6.leftover)
    }

    @Test
    fun testBinaryExpressionParser() {
        val parser = BinaryExpressionParser()
        val result1 = parser.parse("(1+2)")
        assertEquals(Tag.OK, result1.tag)
        assertEquals(
            BinaryExpressionParsingResult(
                ConstantExpressionParsingResult(false, 1),
                CharParsingResult('+'),
                ConstantExpressionParsingResult(false, 2)
            ),
            result1.result)
        assertEquals("", result1.leftover)

        val result2 = parser.parse("(1+-2)")
        assertEquals(Tag.OK, result2.tag)
        assertEquals(
            BinaryExpressionParsingResult(
                ConstantExpressionParsingResult(false, 1),
                CharParsingResult('+'),
                ConstantExpressionParsingResult(true, 2)
            ),
            result2.result)
        assertEquals("", result2.leftover)

        val result3 = parser.parse("1+2")
        assertEquals(Tag.ERROR, result3.tag)
        assertNull(result3.result)
        assertEquals("1+2", result3.leftover)

        val result4 = parser.parse("(1+2")
        assertEquals(Tag.ERROR, result4.tag)
        assertNull(result4.result)
        assertEquals("(1+2", result4.leftover)

        val result5 = parser.parse("1+2)")
        assertEquals(Tag.ERROR, result5.tag)
        assertNull(result5.result)
        assertEquals("1+2)", result5.leftover)

        val result6 = parser.parse("(1+2)))")
        assertEquals(Tag.OK, result6.tag)
        assertEquals(
            BinaryExpressionParsingResult(
                ConstantExpressionParsingResult(false, 1),
                CharParsingResult('+'),
                ConstantExpressionParsingResult(false, 2)
            ),
            result6.result)
        assertEquals("))", result6.leftover)

        val result7 = parser.parse("(((1+2)*(-3+4))-(5*-6))")
        assertEquals(Tag.OK, result7.tag)
        assertEquals(
            BinaryExpressionParsingResult(
                BinaryExpressionParsingResult(
                    BinaryExpressionParsingResult(
                        ConstantExpressionParsingResult(false, 1),
                        CharParsingResult('+'),
                        ConstantExpressionParsingResult(false, 2)
                    ),
                    CharParsingResult('*'),
                    BinaryExpressionParsingResult(
                        ConstantExpressionParsingResult(true, 3),
                        CharParsingResult('+'),
                        ConstantExpressionParsingResult(false, 4)
                    )
                ),
                CharParsingResult('-'),
                BinaryExpressionParsingResult(
                    ConstantExpressionParsingResult(false, 5),
                    op=CharParsingResult('*'),
                    right=ConstantExpressionParsingResult(true, 6)
                )
            ),
            result7.result)
        assertEquals("", result7.leftover)
    }

    @Test
    fun testExpressionParser() {
        val parser = ExpressionParser()
        val result1 = parser.parse("(1+2)")
        assertEquals(Tag.OK, result1.tag)
        assertEquals(
            BinaryExpressionParsingResult(
                ConstantExpressionParsingResult(false, 1),
                CharParsingResult('+'),
                ConstantExpressionParsingResult(false, 2)
            ),
            result1.result)
        assertEquals("", result1.leftover)

        val result2 = parser.parse("1")
        assertEquals(Tag.OK, result2.tag)
        assertEquals(ConstantExpressionParsingResult(false, 1), result2.result)
        assertEquals("", result2.leftover)

        val result3 = parser.parse("-123")
        assertEquals(Tag.OK, result3.tag)
        assertEquals(ConstantExpressionParsingResult(true, 123), result3.result)
        assertEquals("", result3.leftover)

        val result4 = parser.parse("1+2")
        assertEquals(Tag.ERROR, result4.tag)
        assertNull(result4.result)
        assertEquals("+2", result4.leftover)

        val result5 = parser.parse("(1+2))")
        assertEquals(Tag.ERROR, result5.tag)
        assertNull(result5.result)
        assertEquals(")", result5.leftover)

        val result6 = parser.parse("1(1+2)")
        assertEquals(Tag.ERROR, result6.tag)
        assertNull(result6.result)
        assertEquals("(1+2)", result6.leftover)
    }
}