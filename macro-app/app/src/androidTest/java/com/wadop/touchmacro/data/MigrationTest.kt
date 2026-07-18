package com.wadop.touchmacro.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wadop.touchmacro.data.local.TouchMacroDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Room マイグレーションのテスト骨組み（instrumented）。
 *
 * exportSchema=true により schemas/ にスキーマ JSON が出力される前提。
 * v1 が初版のため現時点では「v1 でDBを作成できる」ことのみ検証する。
 * 将来 version を上げた際は、ここに v1→v2 等のマイグレーション検証を追加する。
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TouchMacroDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun createVersion1() {
        // v1 スキーマでDBを作成できることを確認。
        helper.createDatabase(dbName, 1).close()
    }

    // 例: 将来の v1→v2 マイグレーション検証
    // @Test fun migrate1To2() {
    //     helper.createDatabase(dbName, 1).apply { /* v1 データ投入 */ close() }
    //     helper.runMigrationsAndValidate(dbName, 2, true, TouchMacroDatabase.MIGRATION_1_2)
    // }
}
