package pe.kipu.core.data.mapper

import pe.kipu.core.data.local.entity.CategoryEntity
import pe.kipu.core.domain.model.Category

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    iconKey = iconKey,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    iconKey = iconKey,
)
