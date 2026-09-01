package pe.kipu.core.data.local.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.plan.PlanEnvelopeTemplates

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
            .addMigrations(*migrationsFromVersion(8))
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

    @Test
    fun migrateFromVersion9To10AddsCommitmentIdColumn() {
        createVersion9Database()

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(*migrationsFromVersion(9))
            .allowMainThreadQueries()
            .build()

        val readable = database.openHelper.readableDatabase
        readable.query("PRAGMA table_info(`movements`)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            assertTrue(columns.contains("commitmentId"))
        }
        assertNotNull(
            readable.query(
                "SELECT name FROM sqlite_master WHERE type='index' AND name='index_movements_commitmentId'",
            ).use { it.moveToFirst() },
        )

        database.close()
    }

    @Test
    fun migrateFromVersion10To11AddsSavingsHorizonMonthsColumn() {
        createVersion10Database()

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(*migrationsFromVersion(10))
            .allowMainThreadQueries()
            .build()

        val readable = database.openHelper.readableDatabase
        readable.query("PRAGMA table_info(`commitments`)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            assertTrue(columns.contains("savingsHorizonMonths"))
        }

        database.close()
    }

    @Test
    fun migrateFromVersion7To8ClearsPrimaryPlanEnvelopeIds() {
        createVersion7Database()
        applyMigrations(KipuDatabaseMigrations.MIGRATION_7_8)

        openRawDatabase().use { db ->
            db.query(
                "SELECT envelopeIds FROM financial_plans WHERE id = ?",
                arrayOf(FinancialPlanIds.PRIMARY),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrateFromVersion11To12RepairsEmptyEnvelopeIdsAndAddsIncomeColumns() {
        createVersion11DatabaseWithEmptyEnvelopeIds()

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(*migrationsFromVersion(11))
            .allowMainThreadQueries()
            .build()

        val readable = database.openHelper.readableDatabase
        readable.query("PRAGMA table_info(`financial_plans`)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            assertTrue(columns.contains("incomeProfile"))
            assertTrue(columns.contains("payFrequency"))
        }
        readable.query(
            "SELECT envelopeIds, incomeProfile, payFrequency FROM financial_plans WHERE id = ?",
            arrayOf(FinancialPlanIds.PRIMARY),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(PlanEnvelopeTemplates.wizardEnvelopeIdsCsv(), cursor.getString(0))
            assertEquals("FIXED", cursor.getString(1))
            assertEquals("MONTHLY", cursor.getString(2))
        }

        database.close()
    }

    @Test
    fun migrateFromVersion13To14AddsIsSettledColumnToGatherings() {
        createVersion11DatabaseWithEmptyEnvelopeIds() // We can start from 11 and apply up to 13
        applyMigrations(
            KipuDatabaseMigrations.MIGRATION_11_12,
            KipuDatabaseMigrations.MIGRATION_12_13,
        )

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(*migrationsFromVersion(13))
            .allowMainThreadQueries()
            .build()

        val readable = database.openHelper.readableDatabase
        readable.query("PRAGMA table_info(`gatherings`)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            assertTrue(columns.contains("isSettled"))
        }

        database.close()
    }

    @Test
    fun migrateFromVersion11To13AddsInitialBalanceCents() {
        createVersion11DatabaseWithEmptyEnvelopeIds()
        applyMigrations(KipuDatabaseMigrations.MIGRATION_11_12)

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(*migrationsFromVersion(12))
            .allowMainThreadQueries()
            .build()

        val readable = database.openHelper.readableDatabase
        readable.query("PRAGMA table_info(`financial_plans`)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            assertTrue(columns.contains("initialBalanceCents"))
        }
        database.close()
    }

    @Test
    fun migrateFromVersion13To15AddsBudgetCycle() {
        createVersion11DatabaseWithEmptyEnvelopeIds()
        applyMigrations(
            KipuDatabaseMigrations.MIGRATION_11_12,
            KipuDatabaseMigrations.MIGRATION_12_13,
            KipuDatabaseMigrations.MIGRATION_13_14,
        )

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(*migrationsFromVersion(14))
            .allowMainThreadQueries()
            .build()

        val readable = database.openHelper.readableDatabase
        readable.query("PRAGMA table_info(`financial_plans`)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            assertTrue(columns.contains("budgetCycle"))
        }
        database.close()
    }

    @Test
    fun migrateFromVersion15To16AddsAtomicPlanSettingsWithSafeDefaults() {
        createVersion11DatabaseWithEmptyEnvelopeIds()
        applyMigrations(
            KipuDatabaseMigrations.MIGRATION_11_12,
            KipuDatabaseMigrations.MIGRATION_12_13,
            KipuDatabaseMigrations.MIGRATION_13_14,
            KipuDatabaseMigrations.MIGRATION_14_15,
        )

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(
                KipuDatabaseMigrations.MIGRATION_15_16,
                KipuDatabaseMigrations.MIGRATION_16_17,
                KipuDatabaseMigrations.MIGRATION_17_18,
                KipuDatabaseMigrations.MIGRATION_18_19,
                KipuDatabaseMigrations.MIGRATION_19_20,
                KipuDatabaseMigrations.MIGRATION_20_21,
                KipuDatabaseMigrations.MIGRATION_21_22,
            )
            .allowMainThreadQueries()
            .build()

        val readable = database.openHelper.readableDatabase
        readable.query(
            "SELECT antSpendingLimitCents, antSpendingAlertEnabled, " +
                "antSpendingAlertPercent, antSpendingTrackedCategoryIds " +
                "FROM financial_plans WHERE id = ?",
            arrayOf(FinancialPlanIds.PRIMARY),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals(80, cursor.getInt(2))
            assertEquals("", cursor.getString(3))
        }
        database.close()
    }

    @Test
    fun migrateAllTheWayToLatest() {
        createVersion4Database()

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(*KipuDatabaseMigrations.ALL)
            .allowMainThreadQueries()
            .build()

        val readable = database.openHelper.readableDatabase

        // Comprobar que hemos llegado a la versión actual.
        assertEquals(22, readable.version)

        // Comprobar alguna de las últimas columnas añadidas
        readable.query("PRAGMA table_info(`financial_plans`)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            assertTrue(columns.contains("budgetCycle"))
            assertTrue(columns.contains("antSpendingLimitCents"))
            assertTrue(columns.contains("antSpendingAlertEnabled"))
            assertTrue(columns.contains("antSpendingAlertPercent"))
            assertTrue(columns.contains("antSpendingTrackedCategoryIds"))
        }
        database.close()
    }

    @Test
    fun migrateFromVersion20To21AddsReserveTargetAndLedgerWithoutInventingBalance() {
        createVersion4Database()
        applyMigrations(
            *KipuDatabaseMigrations.ALL
                .filter { it.startVersion >= 4 && it.endVersion <= 20 }
                .toTypedArray(),
        )
        openRawDatabase().use { db ->
            db.execSQL(
                """
                INSERT INTO financial_plans (
                    id, estimatedMonthlyIncomeCents, fixedExpensesCents, envelopeIds
                ) VALUES (?, 200000, 120000, '')
                """.trimIndent(),
                arrayOf(FinancialPlanIds.PRIMARY),
            )
        }

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(
                KipuDatabaseMigrations.MIGRATION_20_21,
                KipuDatabaseMigrations.MIGRATION_21_22,
            )
            .allowMainThreadQueries()
            .build()
        val readable = database.openHelper.readableDatabase

        readable.query(
            "SELECT reserveMonthlyContributionCents FROM financial_plans WHERE id = ?",
            arrayOf(FinancialPlanIds.PRIMARY),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0L, cursor.getLong(0))
        }
        assertNotNull(
            readable.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='reserve_events'",
            ).use { it.moveToFirst() },
        )
        assertNotNull(
            readable.query(
                "SELECT name FROM sqlite_master WHERE type='index' " +
                    "AND name='index_reserve_events_sourceMovementId_type'",
            ).use { it.moveToFirst() },
        )
        database.close()
    }

    @Test
    fun migrateFromVersion21To22PreservesLedgerAndAllowsHistoricalUses() {
        createVersion4Database()
        applyMigrations(
            *KipuDatabaseMigrations.ALL
                .filter { it.startVersion >= 4 && it.endVersion <= 21 }
                .toTypedArray(),
        )
        openRawDatabase().use { db ->
            db.execSQL(
                """
                INSERT INTO reserve_events (
                    id, type, amountCents, sourceMovementId, reversesEventId,
                    occurredAtMillis, createdAtMillis
                ) VALUES ('use-old', 'USE', 10000, 'movement-1', NULL, 1, 1)
                """.trimIndent(),
            )
        }

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(KipuDatabaseMigrations.MIGRATION_21_22)
            .allowMainThreadQueries()
            .build()
        val writable = database.openHelper.writableDatabase

        writable.execSQL(
            """
            INSERT INTO reserve_events (
                id, type, amountCents, sourceMovementId, reversesEventId,
                occurredAtMillis, createdAtMillis
            ) VALUES ('use-history', 'USE', 6000, 'movement-1', NULL, 2, 2)
            """.trimIndent(),
        )
        writable.query(
            "SELECT COUNT(*) FROM reserve_events WHERE sourceMovementId = 'movement-1'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
        }
        assertNotNull(
            writable.query(
                "SELECT name FROM sqlite_master WHERE type='index' " +
                    "AND name='index_reserve_events_sourceMovementId'",
            ).use { it.moveToFirst() },
        )
        database.close()
    }

    @Test
    fun migrateFromVersion19To20AddsNullableEnvelopeIdWithoutReassigningHistory() {
        createVersion4Database()
        applyMigrations(
            *KipuDatabaseMigrations.ALL
                .filter { it.startVersion >= 4 && it.endVersion <= 19 }
                .toTypedArray(),
        )
        openRawDatabase().use { db ->
            db.execSQL(
                """
                INSERT INTO movements (
                    id, type, amountCents, categoryId, channel, source, status,
                    description, counterpartyName, operationNumber, commitmentId,
                    recordedAtMillis, createdAtMillis
                ) VALUES (?, 'EXPENSE', 4000, 'other', 'CASH', 'MANUAL', 'CONFIRMED',
                    NULL, NULL, NULL, NULL, 1, 1)
                """.trimIndent(),
                arrayOf("legacy-other"),
            )
        }

        val database = Room.databaseBuilder(context, KipuDatabase::class.java, dbName)
            .addMigrations(
                KipuDatabaseMigrations.MIGRATION_19_20,
                KipuDatabaseMigrations.MIGRATION_20_21,
                KipuDatabaseMigrations.MIGRATION_21_22,
            )
            .allowMainThreadQueries()
            .build()
        val readable = database.openHelper.readableDatabase

        readable.query("SELECT envelopeId FROM movements WHERE id = 'legacy-other'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
        }
        assertNotNull(
            readable.query(
                "SELECT name FROM sqlite_master WHERE type='index' AND name='index_movements_envelopeId'",
            ).use { it.moveToFirst() },
        )
        database.close()
    }

    private fun createVersion11DatabaseWithEmptyEnvelopeIds() {
        createVersion10Database()
        seedPrimaryFinancialPlan()
        applyMigrations(KipuDatabaseMigrations.MIGRATION_10_11)
        openRawDatabase().use { db ->
            db.execSQL(
                """
                UPDATE financial_plans
                SET envelopeIds = ''
                WHERE id = ?
                """.trimIndent(),
                arrayOf(FinancialPlanIds.PRIMARY),
            )
        }
    }

    private fun migrationsFromVersion(startVersion: Int): Array<Migration> =
        KipuDatabaseMigrations.ALL.filter { it.startVersion >= startVersion }.toTypedArray()

    private fun applyMigrations(vararg migrations: Migration) {
        val currentVersion = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { it.version }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(currentVersion) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            )
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase
        migrations.forEach { migration ->
            check(db.version == migration.startVersion) {
                "Expected version ${migration.startVersion} but was ${db.version}"
            }
            migration.migrate(db)
            db.execSQL("PRAGMA user_version = ${migration.endVersion}")
        }
        db.close()
        helper.close()
    }

    private fun openRawDatabase(): SupportSQLiteDatabase {
        val currentVersion = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { it.version }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(currentVersion) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            )
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    private fun seedPrimaryFinancialPlan(envelopeIds: String = "envelope-food,envelope-transport") {
        openRawDatabase().use { db ->
            db.execSQL(
                """
                INSERT OR REPLACE INTO financial_plans
                (id, estimatedMonthlyIncomeCents, fixedExpensesCents, envelopeIds)
                VALUES (?, 3000000, 1800000, ?)
                """.trimIndent(),
                arrayOf(FinancialPlanIds.PRIMARY, envelopeIds),
            )
        }
    }

    private fun createVersion7Database() {
        createVersion6Database()
        seedPrimaryFinancialPlan()
        applyMigrations(KipuDatabaseMigrations.MIGRATION_6_7)
    }

    private fun createVersion10Database() {
        createVersion9Database()
        applyMigrations(KipuDatabaseMigrations.MIGRATION_9_10)
    }

    private fun createVersion9Database() {
        createVersion8Database()
        applyMigrations(KipuDatabaseMigrations.MIGRATION_8_9)
    }

    private fun createVersion8Database() {
        createVersion6Database()
        seedPrimaryFinancialPlan()
        applyMigrations(
            KipuDatabaseMigrations.MIGRATION_6_7,
            KipuDatabaseMigrations.MIGRATION_7_8,
        )
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
