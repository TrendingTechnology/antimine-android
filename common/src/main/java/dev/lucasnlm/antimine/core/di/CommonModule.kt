package dev.lucasnlm.antimine.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lucasnlm.antimine.core.analytics.IAnalyticsManager
import dev.lucasnlm.antimine.core.analytics.DebugAnalyticsManager
import dev.lucasnlm.antimine.core.preferences.IPreferencesRepository
import dev.lucasnlm.antimine.core.preferences.PreferencesManager
import dev.lucasnlm.antimine.core.preferences.PreferencesRepository
import dev.lucasnlm.antimine.core.sound.ISoundManager
import dev.lucasnlm.antimine.core.sound.SoundManager

@Module
@InstallIn(ApplicationComponent::class)
class CommonModule {
    @Provides
    fun providePreferencesRepository(
        preferencesManager: PreferencesManager
    ): IPreferencesRepository = PreferencesRepository(preferencesManager)

    @Provides
    fun providePreferencesInteractor(
        @ApplicationContext context: Context
    ): PreferencesManager = PreferencesManager(context)

    @Provides
    fun provideAnalyticsManager(): IAnalyticsManager = DebugAnalyticsManager()

    @Provides
    fun provideSoundManager(
        @ApplicationContext context: Context
    ): ISoundManager = SoundManager(context)
}
