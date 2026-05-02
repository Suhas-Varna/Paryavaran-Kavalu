package com.example.paryavaran_kavalu.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ReportEntity::class, UserEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun userDao(): UserDao

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

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paryavaran.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
