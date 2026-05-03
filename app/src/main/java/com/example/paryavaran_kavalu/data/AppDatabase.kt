package com.example.paryavaran_kavalu.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ReportEntity::class,
        UserEntity::class,
        RedeemItemEntity::class,
        RedemptionTransactionEntity::class,
    ],
    version = 9,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun userDao(): UserDao
    abstract fun redeemItemDao(): RedeemItemDao
    abstract fun redemptionDao(): RedemptionDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reports ADD COLUMN cleanedImageUri TEXT")
                db.execSQL("ALTER TABLE reports ADD COLUMN cleanedAt INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE user_profile_new (
                        userId INTEGER NOT NULL PRIMARY KEY,
                        nickname TEXT NOT NULL,
                        userType TEXT NOT NULL,
                        bio TEXT NOT NULL,
                        ecoPoints INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO user_profile_new (userId, nickname, userType, bio, ecoPoints)
                    SELECT userId,
                           CASE WHEN TRIM(name) = '' THEN 'Eco Warrior' ELSE TRIM(name) END,
                           '${UserTypes.REPORTER}',
                           '',
                           ecoPoints
                    FROM user_profile
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE user_profile")
                db.execSQL("ALTER TABLE user_profile_new RENAME TO user_profile")

                db.execSQL("ALTER TABLE reports ADD COLUMN reporterUserId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE reports ADD COLUMN reporterNickname TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Keeps installs aligned when the on-device DB was already bumped to 4 (e.g. older branch).
         * Schema matches v3 for this codebase; version must not be lower than existing DB or Room
         * attempts a destructive downgrade.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No SQL changes — identity migration for version parity with prior builds.
            }
        }

        /**
         * Ensures [ReportEntity.description] exists on disk. Older upgrades stopped at v4 without
         * this column; v5 was previously registered with no migration, which triggered destructive
         * fallback for some installs.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = linkedSetOf<String>()
                db.query("PRAGMA table_info(reports)").use { cursor ->
                    val nameIdx = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIdx >= 0) columns.add(cursor.getString(nameIdx))
                    }
                }
                if ("description" !in columns) {
                    db.execSQL(
                        "ALTER TABLE reports ADD COLUMN description TEXT NOT NULL DEFAULT ''",
                    )
                }
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS redeem_items (
                        id INTEGER NOT NULL PRIMARY KEY,
                        category TEXT NOT NULL,
                        title TEXT NOT NULL,
                        subtitle TEXT NOT NULL,
                        costPoints INTEGER NOT NULL,
                        iconName TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS redemption_transactions (
                        userId INTEGER NOT NULL,
                        itemId INTEGER NOT NULL,
                        timesRedeemed INTEGER NOT NULL,
                        PRIMARY KEY (userId, itemId),
                        FOREIGN KEY (userId) REFERENCES user_profile (userId) ON DELETE CASCADE,
                        FOREIGN KEY (itemId) REFERENCES redeem_items (id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_redemption_transactions_userId " +
                        "ON redemption_transactions (userId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_redemption_transactions_itemId " +
                        "ON redemption_transactions (itemId)",
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO redeem_items (id, category, title, subtitle, costPoints, iconName) VALUES
                    (1, 'Food & drink', 'Neighbourhood café perk', 'Sample voucher - partner café', 120, 'LocalCafe'),
                    (2, 'Cleanup & gear', 'Park cleanup kit', 'Gloves and bags for group drives', 200, 'Park'),
                    (3, 'Merch', 'Green market tote', 'Reusable bag - pick-up location TBD', 80, 'ShoppingBag'),
                    (4, 'Retail partner', 'Local store discount', '10% off at a partner shop', 150, 'Storefront'),
                    (5, 'Community', 'Community event pass', 'Entry to a local eco meet-up', 100, 'Groups'),
                    (6, 'Surprise', 'Mystery reward', 'Rotating surprise from sponsors', 300, 'CardGiftcard')
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reports ADD COLUMN cleanerUserId INTEGER")
                db.execSQL(
                    "ALTER TABLE reports ADD COLUMN cleanerNickname TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        /**
         * Rows marked Cleaned before cleaner columns existed (or inserts without cleaner) had null
         * cleanerUserId — backfill local user id 1 so leaderboard → map shows those pins by user.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE reports
                    SET cleanerUserId = 1
                    WHERE cleanerUserId IS NULL
                      AND LOWER(TRIM(status)) = 'cleaned'
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE reports
                    SET cleanerNickname = 'Anonymous'
                    WHERE LOWER(TRIM(status)) = 'cleaned'
                      AND TRIM(cleanerNickname) = ''
                    """.trimIndent(),
                )
            }
        }

        /**
         * Align stored cleaner/reporter nicknames on reports with [user_profile.nickname] for the
         * local user (id 1), replacing placeholders like Anonymous / Seeded cleanup where applicable.
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE reports
                    SET cleanerNickname = COALESCE(
                      (SELECT NULLIF(TRIM(nickname), '') FROM user_profile WHERE userId = 1 LIMIT 1),
                      'Anonymous'
                    )
                    WHERE cleanerUserId = 1
                      AND LOWER(TRIM(status)) = 'cleaned'
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE reports
                    SET reporterNickname = COALESCE(
                      (SELECT NULLIF(TRIM(nickname), '') FROM user_profile WHERE userId = 1 LIMIT 1),
                      'Anonymous'
                    )
                    WHERE reporterUserId = 1
                      AND TRIM(reporterNickname) != 'Demo patrol'
                    """.trimIndent(),
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paryavaran.db",
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                    )
                    /**
                     * If the on-disk schema does not match current entities (identity hash mismatch),
                     * Room recreates the DB on upgrade instead of crashing — e.g. after experimental
                     * branches left `paryavaran.db` at v4 with a different shape than this code.
                     */
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
