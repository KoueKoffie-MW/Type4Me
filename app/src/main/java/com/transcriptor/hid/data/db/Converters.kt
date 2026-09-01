package com.transcriptor.hid.data.db

import androidx.room.TypeConverter
import com.transcriptor.hid.engine.KeyLayout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room Type Converters for lists and enums using kotlinx.serialization.
 */
class Converters {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return json.encodeToString(value ?: emptyList())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromSyntaxType(value: SyntaxType?): String {
        return (value ?: SyntaxType.SHELL).name
    }

    @TypeConverter
    fun toSyntaxType(value: String?): SyntaxType {
        if (value.isNullOrBlank()) return SyntaxType.SHELL
        return try {
            SyntaxType.valueOf(value)
        } catch (_: Throwable) {
            SyntaxType.SHELL
        }
    }

    @TypeConverter
    fun fromHostOsType(value: HostOsType?): String {
        return (value ?: HostOsType.WINDOWS).name
    }

    @TypeConverter
    fun toHostOsType(value: String?): HostOsType {
        if (value.isNullOrBlank()) return HostOsType.WINDOWS
        return try {
            HostOsType.valueOf(value)
        } catch (_: Throwable) {
            HostOsType.WINDOWS
        }
    }

    @TypeConverter
    fun fromKeyLayout(value: KeyLayout?): String {
        return (value ?: KeyLayout.US_QWERTY).name
    }

    @TypeConverter
    fun toKeyLayout(value: String?): KeyLayout {
        if (value.isNullOrBlank()) return KeyLayout.US_QWERTY
        return try {
            KeyLayout.valueOf(value)
        } catch (_: Throwable) {
            KeyLayout.US_QWERTY
        }
    }
}
