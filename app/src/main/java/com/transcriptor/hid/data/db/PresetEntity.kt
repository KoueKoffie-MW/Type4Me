package com.transcriptor.hid.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.transcriptor.hid.ai.PromptPreset

/**
 * Room database entity representing an AI prompt preset.
 */
@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "system_prompt")
    val systemPrompt: String,

    @ColumnInfo(name = "is_built_in")
    val isBuiltIn: Boolean = false,

    @ColumnInfo(name = "temperature")
    val temperature: Float = 0.2f,

    @ColumnInfo(name = "user_prompt_template")
    val userPromptTemplate: String = "{INPUT_TEXT}",

    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts this database entity into a domain [PromptPreset].
     */
    fun toDomain(): PromptPreset = PromptPreset(
        id = id,
        title = title,
        description = description,
        systemPrompt = systemPrompt,
        isBuiltIn = isBuiltIn,
        temperature = temperature,
        userPromptTemplate = userPromptTemplate
    )

    companion object {
        /**
         * Creates a [PresetEntity] from a domain [PromptPreset].
         */
        fun fromDomain(preset: PromptPreset, orderIndex: Int = 0): PresetEntity = PresetEntity(
            id = preset.id,
            title = preset.title,
            description = preset.description,
            systemPrompt = preset.systemPrompt,
            isBuiltIn = preset.isBuiltIn,
            temperature = preset.temperature,
            userPromptTemplate = preset.userPromptTemplate,
            orderIndex = orderIndex
        )
    }
}
