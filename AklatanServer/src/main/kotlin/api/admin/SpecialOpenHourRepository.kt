package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.db.SpecialOpenHour
import com.hexhyperion.aklatan.db.SpecialOpenHourEntity
import com.hexhyperion.aklatan.db.SpecialOpenHours
import com.hexhyperion.aklatan.db.withTransaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.v1.core.eq

class SpecialOpenHourRepository {
    suspend fun create(
        date: LocalDate, openTime: LocalTime?, closeTime: LocalTime?, comment: String?
    ): SpecialOpenHour = withTransaction {
        return@withTransaction SpecialOpenHourEntity.new {
            this.date = date
            this.openTime = openTime
            this.closeTime = closeTime
            this.comment = comment
        }.toSpecialOpenHour()
    }

    suspend fun findByDate(date: LocalDate): SpecialOpenHour? = withTransaction {
        return@withTransaction SpecialOpenHourEntity.find { SpecialOpenHours.date eq date }
            .firstOrNull()
            ?.toSpecialOpenHour()
    }

    suspend fun findAll(): List<SpecialOpenHour> = withTransaction {
        return@withTransaction SpecialOpenHourEntity.all()
            .map { it.toSpecialOpenHour() }
    }

    suspend fun update(
        date: LocalDate,
        openTime: LocalTime?,
        closeTime: LocalTime?,
        comment: String?
    ): SpecialOpenHour? = withTransaction {
        SpecialOpenHourEntity.find { SpecialOpenHours.date eq date }
            .firstOrNull()
            ?.apply {
                this.openTime = openTime
                this.closeTime = closeTime
                this.comment = comment
            }
            ?.toSpecialOpenHour()
    }

    suspend fun delete(date: LocalDate) = withTransaction {
        SpecialOpenHourEntity.find { SpecialOpenHours.date eq date }
            .firstOrNull()
            ?.delete()
    }
}