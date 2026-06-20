package pe.kipu.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiptShareIntentInstrumentedTest {

    @Test
    fun shareIntentLaunchesMainActivityAndSurvivesProcessing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = createShareIntent(context)

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            runCatching {
                onView(withText("Configurar plan después")).perform(click())
            }
            Thread.sleep(5_000)
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    private fun createShareIntent(context: Context): Intent {
        val file = File(context.cacheDir, "receipts/instrumented-share.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(MINIMAL_JPEG)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private companion object {
        val MINIMAL_JPEG: ByteArray = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte(),
        )
    }
}
