package com.transcriptor.hid.data.db

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relation wrapper linking a [CategoryEntity] with its list of associated [SnippetEntity]s.
 */
data class CategoryWithSnippets(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "category_id"
    )
    val snippets: List<SnippetEntity>
)

/**
 * Relation wrapper linking a [CategoryEntity] with its list of associated [MacroEntity]s.
 */
data class CategoryWithMacros(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "category_id"
    )
    val macros: List<MacroEntity>
)
