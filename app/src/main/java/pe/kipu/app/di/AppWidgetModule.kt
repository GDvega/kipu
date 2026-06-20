package pe.kipu.app.di

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kipu.app.widget.DailyAvailableGlanceWidget
import pe.kipu.core.domain.widget.DailyAvailableWidgetGateway

@Module
@InstallIn(SingletonComponent::class)
object AppWidgetModule {

    @Provides
    @Singleton
    fun provideDailyAvailableWidgetGateway(
        @ApplicationContext context: Context,
    ): DailyAvailableWidgetGateway = object : DailyAvailableWidgetGateway {
        override suspend fun requestRefresh() {
            DailyAvailableGlanceWidget().updateAll(context)
        }
    }
}
