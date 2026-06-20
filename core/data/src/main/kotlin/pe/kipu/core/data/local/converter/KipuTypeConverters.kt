package pe.kipu.core.data.local.converter

import androidx.room.TypeConverter
import java.time.Instant
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel

/**
 * Room type converters for shared storage formats.
 *
 * Movement and category entities persist enums as [String] and timestamps as epoch millis;
 * these converters centralize parsing for consistency with mappers.
 */
class KipuTypeConverters {

    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun movementTypeToString(value: MovementType?): String? = value?.name

    @TypeConverter
    fun stringToMovementType(value: String?): MovementType? =
        value?.let { raw -> MovementType.entries.firstOrNull { it.name == raw } ?: MovementType.EXPENSE }

    @TypeConverter
    fun paymentChannelToString(value: PaymentChannel?): String? = value?.name

    @TypeConverter
    fun stringToPaymentChannel(value: String?): PaymentChannel? =
        value?.let { raw -> PaymentChannel.entries.firstOrNull { it.name == raw } ?: PaymentChannel.OTHER }

    @TypeConverter
    fun movementSourceToString(value: MovementSource?): String? = value?.name

    @TypeConverter
    fun stringToMovementSource(value: String?): MovementSource? =
        value?.let { raw -> MovementSource.entries.firstOrNull { it.name == raw } ?: MovementSource.MANUAL }

    @TypeConverter
    fun movementStatusToString(value: MovementStatus?): String? = value?.name

    @TypeConverter
    fun stringToMovementStatus(value: String?): MovementStatus? =
        value?.let { raw -> MovementStatus.entries.firstOrNull { it.name == raw } ?: MovementStatus.CONFIRMED }
}
