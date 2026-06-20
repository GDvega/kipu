package pe.kipu.core.data.local.migration

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.local.KipuDatabase

@RunWith(AndroidJUnit4::class)
class KipuDatabaseMigrationInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "kipu-migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrateFromVersion4To6PreservesExistingTablesAndAddsGatherings() {
        createVersion4Database()

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(*KipuDatabaseMigrations.ALL)
            .allowMainThreadQueries()
            .build()

        val readable = database.openHelper.readableDatabase
        assertNotNull(readable.query("SELECT name FROM sqlite_master WHERE type='table' AND name='gatherings'").use { it.moveToFirst() })
        assertNotNull(
            readable.query("SELECT name FROM sqlite_master WHERE type='table' AND name='gathering_expenses'").use {
                it.moveToFirst()
            },
        )
        assertTrue(
            readable.query("SELECT name FROM sqlite_master WHERE type='table' AND name='movements'").use {
                it.moveToFirst()
            },
        )

        database.close()
    }

    @Test
    fun migrateFromVersion6To7AddsPaidByAndMovementIdColumns() {
        createVersion6Database()

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(*KipuDatabaseMigrations.ALL)
            .allowMainThreadQueries()
            .build()

        val readable = database.openHelper.readableDatabase
        readable.query("PRAGMA table_info(`gathering_expenses`)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            assertTrue(columns.contains("paidByParticipant"))
            assertTrue(columns.contains("movementId"))
        }
        assertNotNull(
            readable.query(
                "SELECT name FROM sqlite_master WHERE type='index' AND name='index_gathering_expenses_movementId'",
            ).use { it.moveToFirst() },
        )

        database.close()
    }

    @Test
    fun migrateFromVersion8To9AddsCurrencyCodeColumn() {
        createVersion8Database()

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(KipuDatabaseMigrations.MIGRATION_8_9)
            .allowMainThreadQueries()
            .build()

        val readable = database.openHelper.readableDatabase
        readable.query("PRAGMA table_info(`commitments`)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            assertTrue(columns.contains("currencyCode"))
        }

        database.close()
    }

    private fun createVersion8Database() {
        createVersion6Database()
        Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(
                KipuDatabaseMigrations.MIGRATION_6_7,
                KipuDatabaseMigrations.MIGRATION_7_8,
            )
            .allowMainThreadQueries()
            .build()
            .close()
    }

    private fun createVersion6Database() {
        createVersion4Database()
        val helper = object : SQLiteOpenHelper(context, dbName, null, 6) {
            override fun onCreate(db: SQLiteDatabase) = Unit

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.use { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `gatherings` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `participantCount` INTEGER NOT NULL,
                    `participantNames` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `gathering_expenses` (
                    `id` TEXT NOT NULL,
                    `gatheringId` TEXT NOT NULL,
                    `amountCents` INTEGER NOT NULL,
                    `description` TEXT,
                    `recordedAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`gatheringId`) REFERENCES `gatherings`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_gathering_expenses_gatheringId` " +
                    "ON `gathering_expenses` (`gatheringId`)",
            )
        }
        helper.close()
    }

    private fun createVersion4Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 4) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `iconKey` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `movements` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `channel` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `description` TEXT,
                        `counterpartyName` TEXT,
                        `operationNumber` TEXT,
                        `recordedAtMillis` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_movements_categoryId` ON `movements` (`categoryId`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `envelopes` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `weeklyLimitCents` INTEGER NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_envelopes_categoryId` ON `envelopes` (`categoryId`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dismissed_duplicate_pairs` (
                        `pairKey` TEXT NOT NULL,
                        `dismissedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`pairKey`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `commitments` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetAmountCents` INTEGER,
                        `currentAmountCents` INTEGER,
                        `dueDateEpochDay` INTEGER,
                        `counterpartyName` TEXT,
                        `isSettled` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `financial_plans` (
                        `id` TEXT NOT NULL,
                        `estimatedMonthlyIncomeCents` INTEGER NOT NULL,
                        `fixedExpensesCents` INTEGER NOT NULL,
                        `envelopeIds` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.close()
        helper.close()
    }
}
