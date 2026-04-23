package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.db.SpecialOpenHour
import com.hexhyperion.aklatan.utility.exception.SpecialOpenHourNotFoundException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class SpecialOpenHourService(private val specialOpenHourRepository: SpecialOpenHourRepository) {
    suspend fun changeOrCreate(
        date: LocalDate, openTime: LocalTime?, closeTime: LocalTime?, comment: String?
    ): SpecialOpenHour {
        return if (specialOpenHourRepository.findByDate(date) != null) {
            specialOpenHourRepository.update(date, openTime, closeTime, comment)
                ?: throw SpecialOpenHourNotFoundException()
        } else {
            specialOpenHourRepository.create(date, openTime, closeTime, comment)
        }
    }

    suspend fun getByDate(date: LocalDate): SpecialOpenHour {
        return specialOpenHourRepository.findByDate(date) ?: throw SpecialOpenHourNotFoundException()
    }

    suspend fun getAll(): List<SpecialOpenHour> {
        return specialOpenHourRepository.findAll()
    }

    suspend fun delete(date: LocalDate) {
        specialOpenHourRepository.findByDate(date) ?: throw SpecialOpenHourNotFoundException()
        specialOpenHourRepository.delete(date)
    }
}