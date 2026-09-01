package pe.kipu.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kipu.core.domain.voice.LocalVoiceIntentAnalyzer
import pe.kipu.core.domain.voice.VoiceIntentAnalyzer

@Module
@InstallIn(SingletonComponent::class)
object VoiceModule {

    @Provides
    @Singleton
    fun provideVoiceIntentAnalyzer(localAnalyzer: LocalVoiceIntentAnalyzer): VoiceIntentAnalyzer = localAnalyzer
}
