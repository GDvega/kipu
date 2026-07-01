package pe.kipu.core.data.local.seed

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.os.CancellationSignal
import android.util.Pair
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery

class RecordingSqliteDatabase : SupportSQLiteDatabase {
    val executedSql: MutableList<String> = mutableListOf()

    override fun execSQL(sql: String) {
        executedSql += sql
    }

    override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        executedSql += sql
    }

    override fun query(query: String): Cursor = unsupported()
    override fun query(query: String, bindArgs: Array<out Any?>): Cursor = unsupported()
    override fun query(query: SupportSQLiteQuery): Cursor = unsupported()
    override fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor = unsupported()
    override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long = unsupported()
    override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int = unsupported()
    override fun update(
        table: String,
        conflictAlgorithm: Int,
        values: ContentValues,
        whereClause: String?,
        whereArgs: Array<out Any?>?,
    ): Int = unsupported()
    override fun beginTransaction() = Unit
    override fun beginTransactionNonExclusive() = Unit
    override fun beginTransactionWithListener(transactionListener: android.database.sqlite.SQLiteTransactionListener) = Unit
    override fun beginTransactionWithListenerNonExclusive(
        transactionListener: android.database.sqlite.SQLiteTransactionListener,
    ) = Unit
    override fun endTransaction() = Unit
    override fun setTransactionSuccessful() = Unit
    override fun inTransaction(): Boolean = false
    override val isDbLockedByCurrentThread: Boolean = false
    override fun yieldIfContendedSafely(): Boolean = false
    override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean = false
    override var version: Int = 1
    override val maximumSize: Long = 0L
    override fun setMaximumSize(numBytes: Long): Long = numBytes
    override var pageSize: Long = 0L
    override fun compileStatement(sql: String): androidx.sqlite.db.SupportSQLiteStatement = unsupported()
    override val isReadOnly: Boolean = false
    override val isOpen: Boolean = true
    override fun needUpgrade(newVersion: Int): Boolean = false
    override val path: String = ":memory:"
    override fun setLocale(locale: java.util.Locale) = Unit
    override fun setMaxSqlCacheSize(cacheSize: Int) = Unit
    override fun setForeignKeyConstraintsEnabled(enable: Boolean) = Unit
    override fun enableWriteAheadLogging(): Boolean = false
    override fun disableWriteAheadLogging() = Unit
    override val isWriteAheadLoggingEnabled: Boolean = false
    override val attachedDbs: List<Pair<String, String>>? = null
    override val isDatabaseIntegrityOk: Boolean = true
    override fun close() = Unit

    private fun <T> unsupported(): T = throw UnsupportedOperationException("Only execSQL is used by seed tests")
}
