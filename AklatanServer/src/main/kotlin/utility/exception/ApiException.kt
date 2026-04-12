package com.hexhyperion.aklatan.utility.exception

open class ApiException(message: String? = null) : Exception(message)


open class AuthenticationException(message: String? = null) : ApiException(message)

class BadCredentialsException(message: String? = null) : AuthenticationException(message)

class BadAccessTokenException(message: String? = null) : AuthenticationException(message)

class BadDeeplinkTokenException(message: String? = null) : AuthenticationException(message)

class BadRefreshTokenException(message: String? = null) : AuthenticationException(message)

class UserExistsException(message: String? = null) : AuthenticationException(message)

class UserNotVerifiedException(message: String? = null) : AuthenticationException(message)


open class NotFoundException(message: String? = null) : ApiException(message)

class UserNotFoundException(message: String? = null) : NotFoundException(message)

class RoleNotFoundException(message: String? = null) : NotFoundException(message)

class WeekDayNotFoundException(message: String? = null) : NotFoundException(message)

class OpenHourExceptionNotFoundException(message: String? = null) : NotFoundException(message)