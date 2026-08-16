package pe.kipu.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kipu.core.data.notification.AndroidNotificationAccessChecker
import pe.kipu.core.domain.notification.NotificationAccessChecker

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationAccessChecker(
        impl: AndroidNotificationAccessChecker,
    ): NotificationAccessChecker

    @Binds
    @Singleton
    abstract fun bindFixedExpenseReminderScheduler(
        impl: pe.kipu.core.data.notification.AndroidFixedExpenseReminderScheduler,
    ): pe.kipu.core.domain.notification.FixedExpenseReminderScheduler
}

