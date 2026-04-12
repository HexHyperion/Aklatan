package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.db.OpenHour
import com.hexhyperion.aklatan.utility.exception.WeekDayNotFoundException
import kotlinx.datetime.LocalTime

class OpenHourService(private val openHourRepository: OpenHourRepository) {
    suspend fun getByDay(weekDay: Int): OpenHour {
        return openHourRepository.find(weekDay)?.toOpenHour() ?: throw WeekDayNotFoundException()
    }

    suspend fun getAll(): List<OpenHour> {
        return openHourRepository.findAll().map { it.toOpenHour() }
    }

    suspend fun change(weekDay: Int, openTime: LocalTime?, closeTime: LocalTime?) {
        openHourRepository.update(weekDay, openTime, closeTime)
    }
}