package pe.kipu.core.data.export

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalFileCacheClearInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: AndroidUserDataExportFileRepository

    @Before
    fun setUp() {
        repository = AndroidUserDataExportFileRepository(context)
    }

    @Test
    fun clearLocalFileCachesRemovesExportsAndReceiptImages() = runBlocking {
        val exportFile = File(context.cacheDir, "exports/kipu-test-export.json").apply {
            parentFile?.mkdirs()
            writeText("{\"test\":true}")
        }
        val receiptFile = File(context.cacheDir, "receipts/test-receipt.jpg").apply {
            parentFile?.mkdirs()
            writeText("fake-image")
        }

        val result = repository.clearLocalFileCaches()

        assertTrue(result.isSuccess)
        assertFalse(exportFile.exists())
        assertFalse(receiptFile.exists())
    }
}
