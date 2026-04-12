package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.db.OpenHourEntity
import com.hexhyperion.aklatan.db.OpenHours
import com.hexhyperion.aklatan.db.withTransaction
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.v1.core.eq

class OpenHourRepository {
    suspend fun findByDay(weekDay: Int): OpenHourEntity? = withTransaction {
        OpenHourEntity.find { OpenHours.weekDay eq weekDay }
            .firstOrNull()
    }

    suspend fun findAll(): List<OpenHourEntity> = withTransaction {
        OpenHourEntity.all().toList()
    }

    suspend fun update(weekDay: Int, openTime: LocalTime?, closeTime: LocalTime?) = withTransaction {
        OpenHourEntity.find { OpenHours.weekDay eq weekDay }
            .firstOrNull()
            ?.apply {
                this.openTime = openTime
                this.closeTime = closeTime
            }
    }
}