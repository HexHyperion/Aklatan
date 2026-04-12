package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.db.OpenHourEntity
import com.hexhyperion.aklatan.db.withTransaction
import kotlinx.datetime.LocalTime

class OpenHourRepository {
    suspend fun find(weekDay: Int): OpenHourEntity? = withTransaction {
        OpenHourEntity.findById(weekDay)
    }

    suspend fun findAll(): List<OpenHourEntity> = withTransaction {
        OpenHourEntity.all().toList()
    }

    suspend fun update(weekDay: Int, openTime: LocalTime?, closeTime: LocalTime?) = withTransaction {
        OpenHourEntity.findById(weekDay)
            ?.apply {
                this.openTime = openTime
                this.closeTime = closeTime
            }
    }
}