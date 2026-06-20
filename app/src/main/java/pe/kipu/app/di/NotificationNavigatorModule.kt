package pe.kipu.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kipu.app.presentation.AndroidNotificationAccessSettingsNavigator
import pe.kipu.core.domain.notification.NotificationAccessSettingsNavigator

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationNavigatorModule {

    @Binds
    @Singleton
    abstract fun bindNotificationAccessSettingsNavigator(
        impl: AndroidNotificationAccessSettingsNavigator,
    ): NotificationAccessSettingsNavigator
}
