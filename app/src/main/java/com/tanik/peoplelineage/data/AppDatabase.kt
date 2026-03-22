package com.tanik.peoplelineage.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PersonEntity::class, PersonRelationshipEntity::class, SpouseRelationshipEntity::class, StorageEventEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun storageEventDao(): StorageEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `spouse_relationships` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `personAId` INTEGER NOT NULL,
                        `personBId` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`personAId`) REFERENCES `people`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`personBId`) REFERENCES `people`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_spouse_relationships_personAId` ON `spouse_relationships` (`personAId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_spouse_relationships_personBId` ON `spouse_relationships` (`personBId`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_spouse_relationships_personAId_personBId` ON `spouse_relationships` (`personAId`, `personBId`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `person_relationships` ADD COLUMN `relationType` TEXT NOT NULL DEFAULT 'UNKNOWN'",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `people` ADD COLUMN `age` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `people` ADD COLUMN `normalizedName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `people` ADD COLUMN `normalizedPhone` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `people` ADD COLUMN `normalizedVillage` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `people` ADD COLUMN `normalizedDistrict` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `people` ADD COLUMN `normalizedState` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `people` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `people` ADD COLUMN `lastViewedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `people` ADD COLUMN `remoteId` TEXT")
                db.execSQL("ALTER TABLE `people` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                db.execSQL("ALTER TABLE `people` ADD COLUMN `deletedAt` INTEGER")
                db.execSQL(
                    """
                    UPDATE `people`
                    SET
                        `normalizedName` = lower(trim(`fullName`)),
                        `normalizedPhone` = replace(replace(replace(trim(`phoneNumber`), ' ', ''), '-', ''), '+', ''),
                        `normalizedVillage` = lower(trim(`village`)),
                        `normalizedDistrict` = lower(trim(`district`)),
                        `normalizedState` = lower(trim(`state`))
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_people_normalizedName` ON `people` (`normalizedName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_people_normalizedPhone` ON `people` (`normalizedPhone`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_people_normalizedVillage` ON `people` (`normalizedVillage`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_people_normalizedDistrict` ON `people` (`normalizedDistrict`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_people_normalizedState` ON `people` (`normalizedState`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_people_isFavorite_lastViewedAt` ON `people` (`isFavorite`, `lastViewedAt`)")

                db.execSQL("ALTER TABLE `spouse_relationships` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.execSQL("ALTER TABLE `spouse_relationships` ADD COLUMN `notes` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `spouse_relationships` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `spouse_relationships` SET `updatedAt` = `createdAt` WHERE `updatedAt` = 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `storage_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `storageMode` TEXT NOT NULL,
                        `locationUri` TEXT NOT NULL,
                        `checksum` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `completedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_storage_events_eventType_startedAt` ON `storage_events` (`eventType`, `startedAt`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_storage_events_status_completedAt` ON `storage_events` (`status`, `completedAt`)",
                )
            }
        }
    }
}
