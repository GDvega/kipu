package pe.kipu.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pe.kipu.core.data.local.seed.DefaultEnvelopeSeed
import pe.kipu.core.domain.plan.FinancialPlanIds

object KipuDatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
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
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `dismissed_duplicate_pairs` (
                    `pairKey` TEXT NOT NULL,
                    `dismissedAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`pairKey`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
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
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
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
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
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
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `gathering_expenses_new` (
                    `id` TEXT NOT NULL,
                    `gatheringId` TEXT NOT NULL,
                    `amountCents` INTEGER NOT NULL,
                    `paidByParticipant` TEXT NOT NULL DEFAULT '',
                    `description` TEXT,
                    `movementId` TEXT,
                    `recordedAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`gatheringId`) REFERENCES `gatherings`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`movementId`) REFERENCES `movements`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `gathering_expenses_new` (
                    `id`, `gatheringId`, `amountCents`, `paidByParticipant`,
                    `description`, `movementId`, `recordedAtMillis`
                )
                SELECT
                    `id`, `gatheringId`, `amountCents`, '',
                    `description`, NULL, `recordedAtMillis`
                FROM `gathering_expenses`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `gathering_expenses`")
            db.execSQL("ALTER TABLE `gathering_expenses_new` RENAME TO `gathering_expenses`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_gathering_expenses_gatheringId` " +
                    "ON `gathering_expenses` (`gatheringId`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_gathering_expenses_movementId` " +
                    "ON `gathering_expenses` (`movementId`)",
            )
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            DefaultEnvelopeSeed.insertInto(db)
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

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE commitments ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'PEN'
                """.trimIndent(),
            )
        }
    }

    val ALL = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
    )
}
