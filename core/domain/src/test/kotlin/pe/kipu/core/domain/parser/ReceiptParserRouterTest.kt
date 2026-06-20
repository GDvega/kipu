package pe.kipu.core.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReceiptParseResult
import pe.kipu.core.domain.test.ReceiptFixtureLoader

class ReceiptParserRouterTest {

    private val router = ReceiptParserRouter(
        yapeReceiptParser = YapeReceiptParser(),
        plinReceiptParser = PlinReceiptParser(),
    )

    @Test
    fun `routes yape receipt to yape parser`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_standard.txt")
        val result = router.parse(text) as ReceiptParseResult.Success

        assertEquals(PaymentChannel.YAPE, result.suggestion.channel)
    }

    @Test
    fun `routes plin receipt to plin parser`() {
        val text = ReceiptFixtureLoader.load("receipts/plin_standard.txt")
        val result = router.parse(text) as ReceiptParseResult.Success

        assertEquals(PaymentChannel.PLIN, result.suggestion.channel)
    }

    @Test
    fun `returns unsupported channel for unknown text`() {
        val text = ReceiptFixtureLoader.load("receipts/unknown_channel.txt")
        val result = router.parse(text)

        assertEquals(ReceiptParseResult.UnsupportedChannel, result)
    }

    @Test
    fun `rejects blank text`() {
        val result = router.parse("  ")
        assertTrue(result is ReceiptParseResult.Failure)
    }
}
