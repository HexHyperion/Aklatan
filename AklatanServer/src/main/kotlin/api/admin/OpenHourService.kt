package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.db.OpenHour
import com.hexhyperion.aklatan.utility.exception.WeekDayNotFoundException
import io.ktor.util.date.*
import kotlinx.datetime.LocalTime

class OpenHourService(private val openHourRepository: OpenHourRepository) {
    suspend fun getByDay(weekDay: WeekDay): OpenHour {
        return openHourRepository.findByDay(weekDay.ordinal)?.toOpenHour() ?: throw WeekDayNotFoundException()
    }

    suspend fun getAll(): List<OpenHour> {
        return openHourRepository.findAll().map { it.toOpenHour() }
    }

    suspend fun change(weekDay: WeekDay, openTime: LocalTime, closeTime: LocalTime) {
        openHourRepository.update(weekDay.ordinal, openTime, closeTime)
    }
}