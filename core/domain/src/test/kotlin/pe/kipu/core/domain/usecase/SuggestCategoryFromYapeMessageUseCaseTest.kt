package pe.kipu.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.category.YapeMessageCategoryRules

class SuggestCategoryFromYapeMessageUseCaseTest {

    private val useCase = SuggestCategoryFromYapeMessageUseCase()

    @Test
    fun `almuerzo maps to food with receipt keyword reason`() {
        val result = useCase("almuerzo en el centro")

        assertEquals(CategoryIds.FOOD, result?.categoryId)
        assertEquals(YapeMessageCategoryRules.REASON_KEY, result?.reason)
    }

    @Test
    fun `pasaje maps to transport`() {
        val result = useCase("pasaje micro")

        assertEquals(CategoryIds.TRANSPORT, result?.categoryId)
        assertEquals(YapeMessageCategoryRules.REASON_KEY, result?.reason)
    }

    @Test
    fun `no message returns no suggestion`() {
        assertNull(useCase(null))
        assertNull(useCase("   "))
    }
}
