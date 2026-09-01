package pe.kipu.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pe.kipu.core.data.local.seed.DefaultEnvelopeSeed
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.plan.PlanEnvelopeTemplates

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

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE movements ADD COLUMN commitmentId TEXT DEFAULT NULL
                REFERENCES commitments(id) ON DELETE SET NULL
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_movements_commitmentId ON movements(commitmentId)
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE commitments ADD COLUMN savingsHorizonMonths INTEGER DEFAULT NULL
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE financial_plans ADD COLUMN incomeProfile TEXT NOT NULL DEFAULT 'FIXED'
                """.trimIndent(),
            )
            db.execSQL(
                """
                ALTER TABLE financial_plans ADD COLUMN payFrequency TEXT NOT NULL DEFAULT 'MONTHLY'
                """.trimIndent(),
            )
            val repairedEnvelopeIds = PlanEnvelopeTemplates.wizardEnvelopeIdsCsv()
            db.execSQL(
                """
                UPDATE financial_plans
                SET envelopeIds = ?
                WHERE id = ? AND (envelopeIds = '' OR envelopeIds IS NULL)
                """.trimIndent(),
                arrayOf(repairedEnvelopeIds, FinancialPlanIds.PRIMARY),
            )
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN initialBalanceCents INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE gatherings ADD COLUMN isSettled INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN budgetCycle TEXT NOT NULL DEFAULT 'WEEKLY'",
            )
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN antSpendingLimitCents INTEGER DEFAULT NULL",
            )
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN antSpendingAlertEnabled INTEGER NOT NULL DEFAULT 1",
            )
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN antSpendingAlertPercent INTEGER NOT NULL DEFAULT 80",
            )
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN antSpendingTrackedCategoryIds TEXT NOT NULL DEFAULT ''",
            )
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN electricityExpensesCents INTEGER DEFAULT NULL",
            )
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN waterExpensesCents INTEGER DEFAULT NULL",
            )
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN internetExpensesCents INTEGER DEFAULT NULL",
            )
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN rentExpensesCents INTEGER DEFAULT NULL",
            )
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN phoneExpensesCents INTEGER DEFAULT NULL",
            )
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN debtsExpensesCents INTEGER DEFAULT NULL",
            )
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN educationExpensesCents INTEGER DEFAULT NULL",
            )
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN customFixedExpensesJson TEXT DEFAULT NULL",
            )
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `monthly_service_receipts` (
                    `id` TEXT NOT NULL,
                    `monthKey` TEXT NOT NULL,
                    `serviceKeyIdentifier` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `configuredAmountCents` INTEGER NOT NULL,
                    `isPaid` INTEGER NOT NULL,
                    `paidMovementId` TEXT DEFAULT NULL,
                    `paidAtEpochMs` INTEGER DEFAULT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_monthly_service_receipts_monthKey` ON `monthly_service_receipts` (`monthKey`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_monthly_service_receipts_monthKey_serviceKeyIdentifier` ON `monthly_service_receipts` (`monthKey`, `serviceKeyIdentifier`)")
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `movement_audit_logs` (
                    `id` TEXT NOT NULL,
                    `movementId` TEXT NOT NULL,
                    `action` TEXT NOT NULL,
                    `movementType` TEXT NOT NULL,
                    `amountCents` INTEGER NOT NULL,
                    `categoryId` TEXT NOT NULL,
                    `categoryName` TEXT DEFAULT NULL,
                    `channel` TEXT NOT NULL,
                    `description` TEXT DEFAULT NULL,
                    `counterpartyName` TEXT DEFAULT NULL,
                    `details` TEXT DEFAULT NULL,
                    `timestampEpochMs` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_movement_audit_logs_movementId` ON `movement_audit_logs` (`movementId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_movement_audit_logs_timestampEpochMs` ON `movement_audit_logs` (`timestampEpochMs`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_movement_audit_logs_action` ON `movement_audit_logs` (`action`)")
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE movements ADD COLUMN envelopeId TEXT DEFAULT NULL")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_movements_envelopeId` ON `movements` (`envelopeId`)",
            )
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE financial_plans ADD COLUMN " +
                    "reserveMonthlyContributionCents INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reserve_events` (
                    `id` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `amountCents` INTEGER NOT NULL,
                    `sourceMovementId` TEXT DEFAULT NULL,
                    `reversesEventId` TEXT DEFAULT NULL,
                    `occurredAtMillis` INTEGER NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_reserve_events_sourceMovementId_type` " +
                    "ON `reserve_events` (`sourceMovementId`, `type`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_reserve_events_reversesEventId` " +
                    "ON `reserve_events` (`reversesEventId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_reserve_events_occurredAtMillis` " +
                    "ON `reserve_events` (`occurredAtMillis`)",
            )
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS `index_reserve_events_sourceMovementId_type`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_reserve_events_sourceMovementId` " +
                    "ON `reserve_events` (`sourceMovementId`)",
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
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
        MIGRATION_15_16,
        MIGRATION_16_17,
        MIGRATION_17_18,
        MIGRATION_18_19,
        MIGRATION_19_20,
        MIGRATION_20_21,
        MIGRATION_21_22,
    )
}
