package com.chinesegames.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Deck::class,
        Word::class,
        GameWordStat::class,
        MatchResult::class,
        Favorite::class,
        GameResult::class,
        HskGroupProgress::class,
        HskExam::class,
        HskSentenceProgress::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deckDao(): DeckDao
    abstract fun wordDao(): WordDao
    abstract fun statsDao(): StatsDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun hskDao(): HskDao

    companion object {
        private const val DB_NAME = "chinese_games.db"

        /**
         * v2: появились «Избранное» и результаты новых игр.
         * Словарь пользователя при обновлении не теряется.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites` " +
                        "(`wordId` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`wordId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `game_results` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`game` TEXT NOT NULL, " +
                        "`playedAt` INTEGER NOT NULL, " +
                        "`asked` INTEGER NOT NULL, " +
                        "`correct` INTEGER NOT NULL, " +
                        "`mistakes` INTEGER NOT NULL, " +
                        "`accuracy` REAL NOT NULL, " +
                        "`durationSeconds` INTEGER NOT NULL, " +
                        "`bestCombo` INTEGER NOT NULL, " +
                        "`score` INTEGER NOT NULL, " +
                        "`stars` INTEGER NOT NULL, " +
                        "`deckIds` TEXT NOT NULL)"
                )
            }
        }

        /**
         * v3: курс «Поэтапное изучение» (прогресс групп, экзамены, предложения)
         * и системная папка «Выученное», которую нельзя удалить.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `decks` ADD COLUMN `isSystem` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `hsk_group_progress` (" +
                        "`groupKey` TEXT NOT NULL, " +
                        "`level` INTEGER NOT NULL, " +
                        "`topicId` TEXT NOT NULL, " +
                        "`groupIndex` INTEGER NOT NULL, " +
                        "`passedMask` INTEGER NOT NULL, " +
                        "`attempts` INTEGER NOT NULL, " +
                        "`bestScore` INTEGER NOT NULL, " +
                        "`learned` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`groupKey`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `hsk_exam` (" +
                        "`level` INTEGER NOT NULL, " +
                        "`passed` INTEGER NOT NULL, " +
                        "`bestAccuracy` REAL NOT NULL, " +
                        "`bestScore` INTEGER NOT NULL, " +
                        "`bestCorrect` INTEGER NOT NULL, " +
                        "`asked` INTEGER NOT NULL, " +
                        "`takenAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`level`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `hsk_sentence_progress` (" +
                        "`topicKey` TEXT NOT NULL, " +
                        "`level` INTEGER NOT NULL, " +
                        "`topicId` TEXT NOT NULL, " +
                        "`passedMask` INTEGER NOT NULL, " +
                        "`attempts` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`topicKey`))"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
