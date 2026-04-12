package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.db.OpenHourException
import com.hexhyperion.aklatan.db.OpenHourExceptionEntity
import com.hexhyperion.aklatan.db.OpenHourExceptions
import com.hexhyperion.aklatan.db.withTransaction
import com.hexhyperion.aklatan.utility.exception.OpenHourExceptionNotFoundException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.v1.core.eq

class OpenHourExceptionRepository {
    suspend fun create(date: LocalDate, openTime: LocalTime?, closeTime: LocalTime?, comment: String?): OpenHourException = withTransaction {
        return@withTransaction OpenHourExceptionEntity.new {
            this.date = date
            this.openTime = openTime
            this.closeTime = closeTime
            this.comment = comment
        }.toOpenHourException()
    }

    suspend fun findByDate(date: LocalDate): OpenHourException? = withTransaction {
        return@withTransaction OpenHourExceptionEntity.find { OpenHourExceptions.date eq date }
            .firstOrNull()
            ?.toOpenHourException()
    }

    suspend fun findAll(): List<OpenHourException> = withTransaction {
        return@withTransaction OpenHourExceptionEntity.all()
            .map { it.toOpenHourException() }
    }

    suspend fun update(date: LocalDate, openTime: LocalTime?, closeTime: LocalTime?, comment: String?): OpenHourException = withTransaction {
        OpenHourExceptionEntity.find { OpenHourExceptions.date eq date }
            .firstOrNull()
            ?.apply {
                this.openTime = openTime
                this.closeTime = closeTime
                this.comment = comment
            }
            ?.toOpenHourException() ?: throw OpenHourExceptionNotFoundException()
    }

    suspend fun delete(date: LocalDate) = withTransaction {
        OpenHourExceptionEntity.find { OpenHourExceptions.date eq date }
            .firstOrNull()
            ?.delete()
    }
}