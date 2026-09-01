package pe.kipu.core.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.receipt.ServiceReceiptKey
import java.math.BigDecimal

class VoiceFinancialIntentParserTest {

    private val parser = VoiceFinancialIntentParser()

    @Test
    fun `parses daily water expense accurately`() {
        val input = "He gastado 5 soles comprándome un agua"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.Expense)
        val expense = intent as VoiceFinancialIntent.Expense
        assertEquals(Money.of(BigDecimal("5.00")).getOrError(), expense.amount)
        assertEquals(CategoryIds.FOOD, expense.categoryId)
        assertEquals("Agua", expense.description)
        assertEquals(PaymentChannel.CASH, expense.channel)
    }

    @Test
    fun `parses yape payment for bottled water from speech transcription`() {
        val intent = parser.parse("Yapeé un sol por un agua")

        assertTrue(intent is VoiceFinancialIntent.Expense)
        val expense = intent as VoiceFinancialIntent.Expense
        assertEquals(Money.of(BigDecimal("1.00")).getOrError(), expense.amount)
        assertEquals(CategoryIds.FOOD, expense.categoryId)
        assertEquals("Agua", expense.description)
        assertEquals(PaymentChannel.YAPE, expense.channel)
    }

    @Test
    fun `parses public transit ticket expense accurately`() {
        val input = "He gastado 5 soles para pagar un pasaje"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.Expense)
        val expense = intent as VoiceFinancialIntent.Expense
        assertEquals(Money.of(BigDecimal("5.00")).getOrError(), expense.amount)
        assertEquals(CategoryIds.TRANSPORT, expense.categoryId)
        assertEquals("Pasaje", expense.description)
    }

    @Test
    fun `parses taxi ride expense with direct amount accurately`() {
        val input = "He pagado un taxi 15 soles"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.Expense)
        val expense = intent as VoiceFinancialIntent.Expense
        assertEquals(Money.of(BigDecimal("15.00")).getOrError(), expense.amount)
        assertEquals(CategoryIds.TRANSPORT, expense.categoryId)
        assertEquals("Taxi", expense.description)
    }

    @Test
    fun `parses car savings goal contribution accurately`() {
        val input = "He guardado 200 soles para mi meta de carro"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.GoalContribution)
        val goal = intent as VoiceFinancialIntent.GoalContribution
        assertEquals(Money.of(BigDecimal("200.00")).getOrError(), goal.amount)
        assertEquals("Carro", goal.goalQuery)
    }

    @Test
    fun `parses motorcycle savings goal contribution accurately`() {
        val input = "He guardado 50 soles para mi meta de moto"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.GoalContribution)
        val goal = intent as VoiceFinancialIntent.GoalContribution
        assertEquals(Money.of(BigDecimal("50.00")).getOrError(), goal.amount)
        assertEquals("Moto", goal.goalQuery)
    }

    @Test
    fun `parses light service receipt payment without explicit amount`() {
        val input = "Ya pagué mi recibo de luz"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.ServiceReceiptPayment)
        val receipt = intent as VoiceFinancialIntent.ServiceReceiptPayment
        assertEquals(ServiceReceiptKey.LIGHT, receipt.serviceKey)
        assertEquals(null, receipt.amount)
    }

    @Test
    fun `parses water service receipt payment with amount accurately`() {
        val input = "Ya pagué mi recibo del agua 35 soles"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.ServiceReceiptPayment)
        val receipt = intent as VoiceFinancialIntent.ServiceReceiptPayment
        assertEquals(ServiceReceiptKey.WATER, receipt.serviceKey)
        assertEquals(Money.of(BigDecimal("35.00")).getOrError(), receipt.amount)
    }

    @Test
    fun `parses internet receipt payment with direct syntax`() {
        val input = "Pagué el internet 80 soles"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.ServiceReceiptPayment)
        val receipt = intent as VoiceFinancialIntent.ServiceReceiptPayment
        assertEquals(ServiceReceiptKey.INTERNET, receipt.serviceKey)
        assertEquals(Money.of(BigDecimal("80.00")).getOrError(), receipt.amount)
    }

    @Test
    fun `parses actual amount before light receipt name`() {
        val intent = parser.parse("He pagado 55 soles del recibo de luz")

        assertTrue(intent is VoiceFinancialIntent.ServiceReceiptPayment)
        val receipt = intent as VoiceFinancialIntent.ServiceReceiptPayment
        assertEquals(ServiceReceiptKey.LIGHT, receipt.serviceKey)
        assertEquals(Money.of(BigDecimal("55.00")).getOrError(), receipt.amount)
    }

    @Test
    fun `parses gas configured as a custom monthly service`() {
        val intent = parser.parse("Pagué 42 soles del recibo de gas")

        assertTrue(intent is VoiceFinancialIntent.ServiceReceiptPayment)
        val receipt = intent as VoiceFinancialIntent.ServiceReceiptPayment
        assertEquals(ServiceReceiptKey.custom("Gas"), receipt.serviceKey)
        assertEquals(Money.of(BigDecimal("42.00")).getOrError(), receipt.amount)
    }

    @Test
    fun `parses income deposit with description`() {
        val input = "Me depositaron 1500 soles de mi sueldo"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.Income)
        val income = intent as VoiceFinancialIntent.Income
        assertEquals(Money.of(BigDecimal("1500.00")).getOrError(), income.amount)
        assertEquals(CategoryIds.OTHER, income.categoryId)
        assertEquals("Mi sueldo", income.description)
    }

    @Test
    fun `parses slang lucas with yape payment channel`() {
        val input = "Pagué 10 lucas de taxi con Yape"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.Expense)
        val expense = intent as VoiceFinancialIntent.Expense
        assertEquals(Money.of(BigDecimal("10.00")).getOrError(), expense.amount)
        assertEquals(CategoryIds.TRANSPORT, expense.categoryId)
        assertEquals(PaymentChannel.YAPE, expense.channel)
    }

    @Test
    fun `parses transport expense correctly`() {
        val input = "Gasté 5 soles en transporte"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.Expense)
        val expense = intent as VoiceFinancialIntent.Expense
        assertEquals(Money.of(BigDecimal("5.00")).getOrError(), expense.amount)
        assertEquals(CategoryIds.TRANSPORT, expense.categoryId)
        assertEquals("Transporte", expense.description)
    }

    @Test
    fun `parses ant spending expense correctly into other category`() {
        val input = "Gasté 3 soles en gasto hormiga"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.Expense)
        val expense = intent as VoiceFinancialIntent.Expense
        assertEquals(Money.of(BigDecimal("3.00")).getOrError(), expense.amount)
        assertEquals(CategoryIds.OTHER, expense.categoryId)
    }

    @Test
    fun `parses ant spending candy expense into other category`() {
        val input = "Gasté 2 soles en un chicle"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.Expense)
        val expense = intent as VoiceFinancialIntent.Expense
        assertEquals(Money.of(BigDecimal("2.00")).getOrError(), expense.amount)
        assertEquals(CategoryIds.OTHER, expense.categoryId)
    }

    @Test
    fun `parses services expense into services category`() {
        val input = "Gasté 50 soles en servicios"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.Expense)
        val expense = intent as VoiceFinancialIntent.Expense
        assertEquals(Money.of(BigDecimal("50.00")).getOrError(), expense.amount)
        assertEquals(CategoryIds.SERVICES, expense.categoryId)
    }

    @Test
    fun `parses direct amount without category defaulting to other category`() {
        val input = "Gasté 5 soles"
        val intent = parser.parse(input)

        assertTrue(intent is VoiceFinancialIntent.Expense)
        val expense = intent as VoiceFinancialIntent.Expense
        assertEquals(Money.of(BigDecimal("5.00")).getOrError(), expense.amount)
        assertEquals(CategoryIds.OTHER, expense.categoryId)
    }
}
