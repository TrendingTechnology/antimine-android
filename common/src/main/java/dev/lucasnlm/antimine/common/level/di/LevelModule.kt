package dev.lucasnlm.antimine.common.level.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lucasnlm.antimine.common.level.database.AppDataBase
import dev.lucasnlm.antimine.common.level.database.dao.SaveDao
import dev.lucasnlm.antimine.common.level.database.dao.StatsDao
import dev.lucasnlm.antimine.common.level.repository.IMinefieldRepository
import dev.lucasnlm.antimine.common.level.repository.ISavesRepository
import dev.lucasnlm.antimine.common.level.repository.IStatsRepository
import dev.lucasnlm.antimine.common.level.repository.MinefieldRepository
import dev.lucasnlm.antimine.common.level.repository.SavesRepository
import dev.lucasnlm.antimine.common.level.repository.StatsRepository
import dev.lucasnlm.antimine.common.level.utils.Clock
import dev.lucasnlm.antimine.common.level.utils.HapticFeedbackManager
import dev.lucasnlm.antimine.common.level.utils.IHapticFeedbackManager

@Module
@InstallIn(ActivityComponent::class)
open class LevelModule {
    @Provides
    fun provideAppDataBase(
        @ApplicationContext context: Context
    ): AppDataBase {
        return Room.databaseBuilder(context, AppDataBase::class.java, DATA_BASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSavesDao(
        appDataBase: AppDataBase
    ): SaveDao = appDataBase.saveDao()

    @Provides
    fun provideStatsDao(
        appDataBase: AppDataBase
    ): StatsDao = appDataBase.statsDao()

    @Provides
    open fun provideClock(): Clock = Clock()

    @Provides
    open fun provideSavesRepository(
        savesDao: SaveDao
    ): ISavesRepository = SavesRepository(savesDao)

    @Provides
    open fun provideStatsRepository(
        statsDao: StatsDao
    ): IStatsRepository = StatsRepository(statsDao)

    @Provides
    open fun provideMinefieldRepository(): IMinefieldRepository = MinefieldRepository()

    @Provides
    open fun provideHapticFeedbackInteractor(
        @ApplicationContext context: Context
    ): IHapticFeedbackManager = HapticFeedbackManager(context)

    companion object {
        private const val DATA_BASE_NAME = "saves-db"
    }
}
