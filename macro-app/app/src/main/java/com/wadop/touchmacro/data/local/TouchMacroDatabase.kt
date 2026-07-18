package com.wadop.touchmacro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * アプリのローカルDB。
 *
 * exportSchema=true にしてスキーマ JSON を書き出し、
 * Room のマイグレーションテスト（androidTest）で利用できるようにする。
 *
 * マイグレーション方針:
 * - 破壊的マイグレーション（fallbackToDestructiveMigration）は使わない。
 * - スキーマ変更時は version を上げ、[Migrations] に Migration を追加する。
 * - 操作データは JSON 1列に集約しているため、操作フォーマットの拡張は
 *   JSON 内で吸収でき、テーブルスキーマの変更を最小化できる。
 */
@Database(
    entities = [
        RecordingEntity::class,
        UnitEntity::class,
        SequenceCounterEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TouchMacroDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun unitDao(): UnitDao
    abstract fun sequenceDao(): SequenceDao

    companion object {
        const val NAME = "touchmacro.db"

        /**
         * 全マイグレーション。将来 version を上げる際はここに追加する。
         * 例（v1→v2 で列を追加する場合）:
         * val MIGRATION_1_2 = object : Migration(1, 2) {
         *     override fun migrate(db: SupportSQLiteDatabase) {
         *         db.execSQL("ALTER TABLE recordings ADD COLUMN note TEXT")
         *     }
         * }
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            // v1 が初版のため現時点では空。
        )
    }
}

/**
 * マイグレーション記述例の置き場（未使用テンプレート）。
 * 実際に使う際は [TouchMacroDatabase.ALL_MIGRATIONS] へ登録する。
 */
@Suppress("unused")
private object MigrationTemplates {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 例: db.execSQL("ALTER TABLE recordings ADD COLUMN note TEXT DEFAULT NULL")
        }
    }
}
