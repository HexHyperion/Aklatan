package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.db.OpenHourException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class OpenHourExceptionService(private val openHourExceptionRepository: OpenHourExceptionRepository) {
    suspend fun create(date: LocalDate, openTime: LocalTime, closeTime: LocalTime, comment: String): OpenHourException {
        return openHourExceptionRepository.create(date, openTime, closeTime, comment)
    }

    suspend fun getByDate(date: LocalDate): OpenHourException? {
        return openHourExceptionRepository.findByDate(date)
    }

    suspend fun getAll(): List<OpenHourException> {
        return openHourExceptionRepository.findAll()
    }

    suspend fun change(date: LocalDate, openTime: LocalTime, closeTime: LocalTime, comment: String) {
        openHourExceptionRepository.update(date, openTime, closeTime, comment)
    }
}