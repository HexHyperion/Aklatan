package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.db.OpenHourException
import com.hexhyperion.aklatan.utility.exception.OpenHourExceptionNotFoundException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class OpenHourExceptionService(private val openHourExceptionRepository: OpenHourExceptionRepository) {
    suspend fun changeOrCreate(date: LocalDate, openTime: LocalTime?, closeTime: LocalTime?, comment: String?): OpenHourException {
        return if (openHourExceptionRepository.findByDate(date) != null) {
            openHourExceptionRepository.update(date, openTime, closeTime, comment)
        } else {
            openHourExceptionRepository.create(date, openTime, closeTime, comment)
        }
    }

    suspend fun getByDate(date: LocalDate): OpenHourException {
        return openHourExceptionRepository.findByDate(date) ?: throw OpenHourExceptionNotFoundException()
    }

    suspend fun getAll(): List<OpenHourException> {
        return openHourExceptionRepository.findAll()
    }

    suspend fun delete(date: LocalDate) {
        openHourExceptionRepository.delete(date)
    }
}