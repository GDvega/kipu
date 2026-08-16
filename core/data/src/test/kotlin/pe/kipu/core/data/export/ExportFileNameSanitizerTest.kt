package pe.kipu.core.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportFileNameSanitizerTest {

    @Test
    fun sanitize_validStandardFileNames_remainIntact() {
        assertEquals(
            "kipu_export_2026-08-15.json",
            ExportFileNameSanitizer.sanitize("kipu_export_2026-08-15.json"),
        )
        assertEquals(
            "kipu_export_2026-08-15_movimientos.csv",
            ExportFileNameSanitizer.sanitize("kipu_export_2026-08-15_movimientos.csv"),
        )
        assertEquals(
            "kipu_export_2026-08-15_movimientos_excel.csv",
            ExportFileNameSanitizer.sanitize("kipu_export_2026-08-15_movimientos_excel.csv"),
        )
    }

    @Test
    fun sanitize_blankOrWhitespace_returnsDefault() {
        assertEquals("kipu_export.dat", ExportFileNameSanitizer.sanitize(""))
        assertEquals("kipu_export.dat", ExportFileNameSanitizer.sanitize("   "))
        assertEquals("kipu_export.dat", ExportFileNameSanitizer.sanitize("\t\n"))
    }

    @Test
    fun sanitize_pathTraversal_stripsDirectoriesAndResolvesSafely() {
        assertEquals(
            "passwd.dat",
            ExportFileNameSanitizer.sanitize("../../../../etc/passwd"),
        )
        assertEquals(
            "kipu.db",
            ExportFileNameSanitizer.sanitize("..\\..\\..\\kipu.db"),
        )
        assertEquals(
            "kipu_export.dat",
            ExportFileNameSanitizer.sanitize(".."),
        )
        assertEquals(
            "kipu_export.dat",
            ExportFileNameSanitizer.sanitize("../.."),
        )
        assertEquals(
            "kipu_export.dat",
            ExportFileNameSanitizer.sanitize("."),
        )
    }

    @Test
    fun sanitize_specialCharacters_replacesWithUnderscore() {
        assertEquals(
            "kipu_export_report_1.json",
            ExportFileNameSanitizer.sanitize("kipu export [report] (1).json"),
        )
        assertEquals(
            "kipu_export.json",
            ExportFileNameSanitizer.sanitize("kipu@#$%^&*()export.json"),
        )
    }

    @Test
    fun sanitize_onlySpecialCharacters_returnsDefault() {
        assertEquals("kipu_export.dat", ExportFileNameSanitizer.sanitize("@#$%^&*()"))
        assertEquals("kipu_export.dat", ExportFileNameSanitizer.sanitize("____"))
        assertEquals("kipu_export.dat", ExportFileNameSanitizer.sanitize("...."))
    }

    @Test
    fun sanitize_excessiveLength_truncatesPreservingExtension() {
        val longName = "a".repeat(200) + ".json"
        val result = ExportFileNameSanitizer.sanitize(longName)
        assertTrue(result.endsWith(".json"))
        assertTrue(result.length <= 100)
    }

    @Test
    fun sanitize_doesNotContainPathSeparatorsOrNullBytes() {
        val dirty = "../../root\u0000/test:name?*<>|.csv"
        val sanitized = ExportFileNameSanitizer.sanitize(dirty)
        assertFalse(sanitized.contains("/"))
        assertFalse(sanitized.contains("\\"))
        assertFalse(sanitized.contains("\u0000"))
        assertFalse(sanitized.contains(".."))
    }
}
