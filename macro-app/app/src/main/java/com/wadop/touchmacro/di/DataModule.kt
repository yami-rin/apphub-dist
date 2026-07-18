package com.wadop.touchmacro.di

import android.content.Context
import androidx.room.Room
import com.wadop.touchmacro.data.local.RecordingDao
import com.wadop.touchmacro.data.local.SequenceDao
import com.wadop.touchmacro.data.local.TouchMacroDatabase
import com.wadop.touchmacro.data.local.UnitDao
import com.wadop.touchmacro.data.repository.MacroRepositoryImpl
import com.wadop.touchmacro.domain.repository.MacroRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DB と DAO を提供する DI モジュール。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TouchMacroDatabase =
        Room.databaseBuilder(context, TouchMacroDatabase::class.java, TouchMacroDatabase.NAME)
            // 破壊的マイグレーションはしない。スキーマ変更時は Migration を追加する。
            .addMigrations(*TouchMacroDatabase.ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideRecordingDao(db: TouchMacroDatabase): RecordingDao = db.recordingDao()

    @Provides
    fun provideUnitDao(db: TouchMacroDatabase): UnitDao = db.unitDao()

    @Provides
    fun provideSequenceDao(db: TouchMacroDatabase): SequenceDao = db.sequenceDao()
}

/**
 * リポジトリ実装を束縛する DI モジュール。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMacroRepository(impl: MacroRepositoryImpl): MacroRepository
}
