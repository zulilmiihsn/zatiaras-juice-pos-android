package com.zatiaras.pos.feature.pos.data.mapper

import com.zatiaras.pos.core.data.local.entity.CashRecordEntity
import com.zatiaras.pos.feature.pos.domain.model.CashRecord
import com.zatiaras.pos.feature.pos.domain.model.CashRecordType

/**
 * Convert CashRecordEntity to domain CashRecord.
 */
fun CashRecordEntity.toDomain(): CashRecord {
    return CashRecord(
        id = id,
        type = CashRecordType.valueOf(type),
        amount = amount,
        description = description,
        category = category,
        notes = notes,
        createdAt = createdAt,
        isSynced = isSynced
    )
}

/**
 * Convert list of CashRecordEntity to domain list.
 */
fun List<CashRecordEntity>.toDomainList(): List<CashRecord> {
    return map { it.toDomain() }
}

/**
 * Convert domain CashRecord to CashRecordEntity.
 */
fun CashRecord.toEntity(): CashRecordEntity {
    return CashRecordEntity(
        id = id,
        type = type.name,
        amount = amount,
        description = description,
        category = category,
        notes = notes,
        createdAt = createdAt,
        isSynced = isSynced
    )
}
