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
        GameResult::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deckDao(): DeckDao
    abstract fun wordDao(): WordDao
    abstract fun statsDao(): StatsDao
    abstract fun favoriteDao(): FavoriteDao

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

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
