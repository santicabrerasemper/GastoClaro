package com.santiago.gastoclaro.data.local

import androidx.room.TypeConverter
import com.santiago.gastoclaro.data.local.entity.MovementType

class Converters {
    @TypeConverter
    fun fromMovementType(value: MovementType): String = value.name

    @TypeConverter
    fun toMovementType(value: String): MovementType = MovementType.valueOf(value)
}
