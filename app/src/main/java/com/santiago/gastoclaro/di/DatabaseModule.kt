package com.santiago.gastoclaro.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.santiago.gastoclaro.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS payment_methods (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    profileId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    kind TEXT NOT NULL,
                    lastDigits TEXT NOT NULL DEFAULT '',
                    closingDay INTEGER,
                    dueDay INTEGER,
                    isArchived INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(profileId) REFERENCES profiles(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_methods_profileId ON payment_methods(profileId)")
            db.execSQL(
                """
                CREATE TABLE movements_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    profileId INTEGER NOT NULL,
                    categoryId INTEGER NOT NULL,
                    paymentMethodId INTEGER,
                    type TEXT NOT NULL,
                    amountCents INTEGER NOT NULL,
                    monthlyImpactCents INTEGER NOT NULL,
                    annualizedMonths INTEGER NOT NULL DEFAULT 1,
                    note TEXT NOT NULL,
                    occurredEpochDay INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(profileId) REFERENCES profiles(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(paymentMethodId) REFERENCES payment_methods(id) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO movements_new (
                    id, profileId, categoryId, paymentMethodId, type, amountCents,
                    monthlyImpactCents, annualizedMonths, note, occurredEpochDay, createdAt, updatedAt
                )
                SELECT id, profileId, categoryId, NULL, type, amountCents,
                    amountCents, 1, note, occurredEpochDay, createdAt, updatedAt
                FROM movements
                """.trimIndent()
            )
            db.execSQL("DROP TABLE movements")
            db.execSQL("ALTER TABLE movements_new RENAME TO movements")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_movements_profileId ON movements(profileId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_movements_categoryId ON movements(categoryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_movements_paymentMethodId ON movements(paymentMethodId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_movements_occurredEpochDay ON movements(occurredEpochDay)")
            db.execSQL(
                """
                INSERT INTO payment_methods (profileId, name, kind, lastDigits, closingDay, dueDay, isArchived, createdAt, updatedAt)
                SELECT id, 'Efectivo', 'CASH', '', NULL, NULL, 0, strftime('%s','now') * 1000, strftime('%s','now') * 1000
                FROM profiles
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO payment_methods (profileId, name, kind, lastDigits, closingDay, dueDay, isArchived, createdAt, updatedAt)
                SELECT id, 'Cuenta bancaria', 'BANK', '', NULL, NULL, 0, strftime('%s','now') * 1000, strftime('%s','now') * 1000
                FROM profiles
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE movements ADD COLUMN installmentGroupId TEXT")
            db.execSQL("ALTER TABLE movements ADD COLUMN installmentIndex INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE movements ADD COLUMN installmentCount INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE movements ADD COLUMN originalAmountCents INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE movements SET originalAmountCents = amountCents WHERE originalAmountCents = 0")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                UPDATE payment_methods
                SET lastDigits = '', closingDay = NULL, dueDay = NULL
                WHERE kind = 'CASH'
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                UPDATE movements
                SET paymentMethodId = (
                    SELECT MIN(cash.id)
                    FROM payment_methods cash
                    WHERE cash.profileId = movements.profileId
                      AND cash.kind = 'CASH'
                )
                WHERE paymentMethodId IN (
                    SELECT id FROM payment_methods WHERE kind = 'CASH'
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE payment_methods
                SET isArchived = 1
                WHERE kind = 'CASH'
                  AND id NOT IN (
                      SELECT MIN(id)
                      FROM payment_methods
                      WHERE kind = 'CASH'
                      GROUP BY profileId
                  )
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE payment_methods
                SET name = 'Efectivo',
                    lastDigits = '',
                    closingDay = NULL,
                    dueDay = NULL
                WHERE kind = 'CASH'
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                UPDATE movements
                SET paymentMethodId = (
                    SELECT MIN(cash.id)
                    FROM payment_methods cash
                    WHERE cash.profileId = movements.profileId
                      AND cash.kind = 'CASH'
                )
                WHERE paymentMethodId IN (
                    SELECT id FROM payment_methods WHERE kind = 'CASH'
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE payment_methods
                SET isArchived = 1
                WHERE kind = 'CASH'
                  AND id NOT IN (
                      SELECT MIN(id)
                      FROM payment_methods
                      WHERE kind = 'CASH'
                      GROUP BY profileId
                  )
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE payment_methods
                SET name = 'Efectivo',
                    lastDigits = '',
                    closingDay = NULL,
                    dueDay = NULL,
                    isArchived = 0
                WHERE kind = 'CASH'
                  AND id IN (
                      SELECT MIN(id)
                      FROM payment_methods
                      WHERE kind = 'CASH'
                      GROUP BY profileId
                  )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                INSERT INTO payment_methods (profileId, name, kind, lastDigits, closingDay, dueDay, isArchived, createdAt, updatedAt)
                SELECT profiles.id, 'Debito', 'DEBIT_CARD', '', NULL, NULL, 0,
                       strftime('%s','now') * 1000,
                       strftime('%s','now') * 1000
                FROM profiles
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM payment_methods
                    WHERE payment_methods.profileId = profiles.id
                      AND payment_methods.kind = 'DEBIT_CARD'
                      AND payment_methods.isArchived = 0
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                INSERT INTO categories (profileId, name, emoji, colorArgb, type, isArchived)
                SELECT profiles.id, 'Educacion', '🎓', -6982195, 'EXPENSE', 0
                FROM profiles
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM categories
                    WHERE categories.profileId = profiles.id
                      AND categories.name = 'Educacion'
                      AND categories.type = 'EXPENSE'
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO categories (profileId, name, emoji, colorArgb, type, isArchived)
                SELECT profiles.id, 'Suscripciones', '🔁', -30107, 'EXPENSE', 0
                FROM profiles
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM categories
                    WHERE categories.profileId = profiles.id
                      AND categories.name = 'Suscripciones'
                      AND categories.type = 'EXPENSE'
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                UPDATE categories
                SET colorArgb = -6982195
                WHERE name = 'Educacion'
                  AND type = 'EXPENSE'
                  AND colorArgb = -11549705
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE movements ADD COLUMN subcategoryName TEXT NOT NULL DEFAULT ''")
            db.execSQL(
                """
                INSERT INTO categories (profileId, name, emoji, colorArgb, type, isArchived)
                SELECT profiles.id, 'Actividad fisica', '🏋️', -1023342, 'EXPENSE', 0
                FROM profiles
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM categories
                    WHERE categories.profileId = profiles.id
                      AND categories.name = 'Actividad fisica'
                      AND categories.type = 'EXPENSE'
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE movements ADD COLUMN currency TEXT NOT NULL DEFAULT 'ARS'")
            db.execSQL("ALTER TABLE movements ADD COLUMN currencyAmountCents INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE movements ADD COLUMN exchangeRateCents INTEGER")
            db.execSQL("UPDATE movements SET currencyAmountCents = amountCents WHERE currencyAmountCents = 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gasto_claro.db"
        ).addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11
        ).build()
}
