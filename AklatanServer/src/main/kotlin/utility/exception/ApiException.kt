package com.hexhyperion.aklatan.utility.exception

open class ApiException(message: String? = null) : Exception(message)


open class AuthenticationException(message: String? = null) : ApiException(message)

class BadCredentialsException(message: String? = null) : AuthenticationException(message)

class BadAccessTokenException(message: String? = null) : AuthenticationException(message)

class BadDeeplinkTokenException(message: String? = null) : AuthenticationException(message)

class BadRefreshTokenException(message: String? = null) : AuthenticationException(message)

class UserExistsException(message: String? = null) : AuthenticationException(message)

class UserNotVerifiedException(message: String? = null) : AuthenticationException(message)


open class BorrowException(message: String? = null) : ApiException(message)

class BookAlreadyReservedException(message: String? = null) : BorrowException(message)

class BookAlreadyBorrowedException(message: String? = null) : BorrowException(message)


open class NotFoundException(message: String? = null) : ApiException(message)

class RoleNotFoundException(message: String? = null) : NotFoundException(message)

class UserNotFoundException(message: String? = null) : NotFoundException(message)

class BookNotFoundException(message: String? = null) : NotFoundException(message)

class ReservationNotFoundException(message: String? = null) : NotFoundException(message)

class BorrowNotFoundException(message: String? = null) : NotFoundException(message)

class WeekDayNotFoundException(message: String? = null) : NotFoundException(message)

class OpenHourExceptionNotFoundException(message: String? = null) : NotFoundException(message)