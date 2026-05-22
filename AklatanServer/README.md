# Aklatan Library Server

This is the server/backend component of Aklatan, a **RESTful API-based library management system backed by Ktor**, written as a project for a web development class.

It provides endpoints for account management by both users and administrators, setting open hours with holiday exceptions, inventory search and modification, book reservations with queue and complex borrowing logic. Additionally, it features an email system with tokens for account verification and password resetting, as well as with cron-based notifications and return reminders.

The server implements JWT role-based access control, and uses a PostgreSQL DB for the persistence layer. I'd even dare to say it's quite well-designed, with clean, modular code and logical endpoint structure, and it even didn't explode in my face under testing!

For the curious, the name _Aklatan_ means _library_ in Cebuano. Creative and totally surprising, isn't it? :P


## Installation & Running

The server is normally built using Gradle, but the project uses Docker Compose, so, having ensured that you have Docker installed and running, **just launch the following command in the project root to build and start all services:**

```bash
docker compose up --build
```

This will download/build the server, DB and scheduler images and run the necessary migrations to set up the database with starter data. The server will be accessible at http://localhost:8080.

**The services depend on multiple environment variables, which must be set in an `.env` file in the project root.** If your file has a different name, make sure to specify it inside `compose.yaml`. A template file is provided as `.env.template`, which you can copy and modify as needed. Most of the variables are self-explanatory, but below is a brief description of the less obvious ones:

- `JWT_SECRET`: A random string used to sign JWT tokens, which you can generate using a password manager or an online generator. It shouldn't be shorter than 32 characters, and a longer one is more secure.
- `INTERNAL_API_KEY`: A random string used to authenticate requests between the server and the scheduler, which you can also generate with different tools (or just smash the keyboard). It can be of any length, but obviously longer is safer.
- `DEVELOPMENT_MODE`: Toggles the built-in Ktor development mode. Additionally, it determines whether the server has CORS policy enabled and if the cookies are set with the `Secure` flag.
- `ALLOWED_ORIGINS`: A comma-separated list of allowed origins for CORS policy, required if the development mode is disabled. The addreses should not contain the protocol prefix.
- `VERIFY_EMAIL_URL` and `RESET_PASSWORD_URL`: The URLs included in the verification and password reset emails sent to users, pointing to frontend routes able to process the tokens. For an example frontend running at `http://localhost:3000`, they can look like `http://localhost:3000/verify-email` or `http://localhost:3000/reset-password`.

The server also has a configuration file `application.yaml` in the `/src/main/resources` folder, where you can configure additional settings such as token expiration times, overdue fines and durations for various reservation and borrowing rules.


## API Usage

The API operates on JWT-based authentication with role-based access control. The available roles are `user`, the default role for new accounts, `librarian` with permissions to manage the inventory and reservations, and `manager` having permissions to manage user accounts and open hours. Each endpoint specifies the required role for access, with "Public" indicating that no authentication is required.

**To authenticate requests, clients must include the JWT in the `Authorization` header as a Bearer token**, like this: `Authorization: Bearer <token>`. The API also utilizes refresh tokens stored in cookies for token rotation once the JWT expires, so **clients should handle the refresh flow** by calling the `/auth/refresh` endpoint when they receive a token expiration error. Non-browser clients must take care to preserve the token cookie.

The entire data transfer (except for the `/` route, query params, refresh token etc.) is performed in JSON format, with the request body containing the necessary fields for each endpoint as specified below, for example for `POST /reservations/batch`:

```json
{
  "userId": 1138,
  "isbns": ["9780099410584", "9788328053595"]
}
```

and the response body containing either the requested data formatted as specified, or a uniform error message formatted as follows:

```json
{
  "error": {
    "code": "USER_NOT_AUTHORIZED",
    "message": "You are on this council, but we do not grant you the rank of master."
  }
}
```

The below sections describe the available API endpoints, their expected request parameters and responses, and the possible error codes they can return. **Error codes written in UPPER_SNAKE_CASE are custom application-level errors**, while successes and codes in TitleCase are standard HTTP statuses.

Note that almost every endpoint can also return `400 INVALID_REQUEST` if the request body doesn't conform to the expected format, `401 ACCESS_TOKEN_INVALID` if the JWT token is missing, different `404 *_NOT_FOUND` errors from various services (although if not specified as expected, they should be treated as internal/bugs) and `500 UNKNOWN_ERROR` if something unexpected happens on the server. **These errors are not mentioned below, but they should be handled by clients as well.**

The `requests.http` file contains examples of requests to most of the endpoints as a demo, or as a starting point for manual testing.

## General

### `GET` `/` (Public):
Check if the server is running by receiving a human-readable welcome message with authentication state.
- Expected responses:
  - `200 OK` with a text message indicating whether the request comes from a user authenticated with a valid JWT.


## User Registration & Authentication

### `POST` `/auth/register` (Public):
Register a new user account and send a verification email upon successful registration.
- Request body parameters:
  - `email` (string, required): The user's email address.
  - `password` (string, required): The user's password.
  - `name` (string, required): The user's full name.
- Expected responses:
  - `201 Created` if the registration is successful and the verification email is sent.
  - `409 USER_ALREADY_EXISTS` if an account with the provided email already exists.

### `POST` `/auth/login` (User, Librarian, Manager):
Authenticate a user and return a short-lived JWT token in the response body, and a long-lived refresh token in an HTTP-only cookie.
- Request body parameters:
  - `email` (string, required): The user's email address.
  - `password` (string, required): The user's password.
- Expected responses:
  - `200 OK` with a refresh token in a cookie if the credentials are valid.
    - `token` (string): The JWT token to be used for authenticated requests, valid for a short period.
  - `401 INCORRECT_USER_CREDENTIALS` if the credentials are incorrect or the email is not verified.
  - `401 USER_NOT_VERIFIED` if the email is not verified.

### `POST` `/auth/request-email-verification` (Public):
Resend the email verification link if the account exists.
- Request body parameters:
  - `email` (string, required): The email address of the account to verify.
- Expected responses:
  - `200 OK` even if the account doesn't exist to prevent enumeration.

### `POST` `/auth/verify-email` (Public):
Verify a user's email using the token sent in the verification email.
- Request body parameters:
  - `token` (string, required): The verification token from the email.
- Expected responses:
  - `200 OK` if the email is successfully verified.
  - `401 DEEPLINK_TOKEN_INVALID` if the token is invalid or expired.

### `POST` `/auth/request-password-reset` (Public):
Request a password reset link to be sent to the user's email.
- Request body parameters:
  - `email` (string, required): The email address of the account to reset the password for.
- Expected responses:
  - `200 OK` even if the account doesn't exist to prevent enumeration.

### `POST` `/auth/reset-password` (Public):
Reset a user's password using the token sent in the password reset email.
- Request body parameters:
  - `token` (string, required): The password reset token from the email.
  - `newPassword` (string, required): The new password to set for the account.
- Expected responses:
  - `200 OK` if the password is successfully reset.
  - `401 DEEPLINK_TOKEN_INVALID` if the token is invalid or expired.

### `POST` `/auth/refresh` (User, Librarian, Manager):
Rotate both authentication tokens using the refresh token from the cookie.
- Expected responses:
  - `200 OK` with a new refresh token in a cookie if the refresh token is valid.
    - `token` (string): The new JWT token to be used for requests.
  - `401 REFRESH_TOKEN_INVALID` if the refresh token is invalid or expired.

### `POST` `/auth/logout` (User, Librarian, Manager):
Invalidate the refresh token to log the user out.
- Expected responses:
  - `200 OK` if the logout is successful.
  - `401 REFRESH_TOKEN_INVALID` if the refresh token is invalid or expired.


## User Account Management

### `GET` `/account` (User, Librarian, Manager):
Get the authenticated user's account details.
- Expected responses:
  - `200 OK` with the user's account information if authenticated.
    - `id` (int): The user's unique identifier.
    - `email` (string): The user's email address.
    - `name` (string): The user's full name.
    - `role` (string): The user's role name.
    - `registeredAt` (instant): The timestamp of when the account was created.
    - `verified` (boolean): Whether the user's email is verified.

### `PATCH` `/account` (User, Librarian, Manager):
Update the authenticated user's account details.
- Request body parameters:
    - `newName` (string, optional): The user's new full name.
    - `password` (string, optional): The user's old password, required if changing the password.
    - `newPassword` (string, optional): The user's new password.
- Expected responses:
  - `200 OK` if the account is successfully updated.
  - `400 INVALID_REQUEST` if the request body is invalid or missing required fields (e.g. old password).
  - `401 INCORRECT_USER_CREDENTIALS` if the provided password is incorrect, checked only when attempting to change it.


## Administrator Account Management

### `GET` `/admin/roles` (Manager):
Get a list of all available roles in the system.
- Expected responses:
  - `200 OK` with a list of roles.
    - list of roles consisting of:
      - `id` (int): The role's unique identifier.
      - `name` (string): The name of the role.

### `GET` `/admin/users` (Manager):
Get a list of all user accounts in the system.
- Expected responses:
  - `200 OK` with a list of user accounts.
    - list of users consisting of:
      - `id` (int): The user's unique identifier.
      - `email` (string): The user's email address.
      - `name` (string): The user's full name.
      - `role` (string): The user's role name.
      - `registeredAt` (instant): The timestamp of when the account was created.
      - `verified` (boolean): Whether the user's email is verified.
    
### `POST` `/admin/users` (Manager):
Create a new user account with a specified role and immediately verify their email.
- Request body parameters:
  - `email` (string, required): The new user's email address.
  - `name` (string, required): The new user's full name.
  - `password` (string, required): The new user's password.
  - `role` (string, optional): The new user's role name, defaults to "user" if not provided.
- Expected responses:
  - `201 Created` if the account is successfully created.
  - `404 ROLE_NOT_FOUND` if the provided role name does not exist.
  - `409 USER_ALREADY_EXISTS` if an account with the provided email already exists.

### `GET` `/admin/users/{userId}` (Manager):
Get the details of a specific user account by ID.
- Path parameters:
  - `userId` (int, required): The unique identifier of the user account.
- Expected responses:
  - `200 OK` with the user's account information if found.
    - `id` (int): The user's unique identifier.
    - `email` (string): The user's email address.
    - `name` (string): The user's full name.
    - `role` (string): The user's role name.
    - `registeredAt` (instant): The timestamp of when the account was created.
    - `verified` (boolean): Whether the user's email is verified.
  - `400 INVALID_REQUEST` if the provided ID is not a valid integer.
  - `404 USER_NOT_FOUND` if no account with the provided ID exists.

### `PATCH` `/admin/users/{userId}` (Manager):
Update the details of a specific user account by ID, including changing their role and email.
- Path parameters:
  - `userId` (int, required): The unique identifier of the user account.
- Request body parameters:
  - `newName` (string, optional): The user's new full name.
  - `newEmail` (string, optional): The user's new email address, which must be unique if provided.
  - `newPassword` (string, optional): The user's new password.
  - `newRole` (string, optional): The user's new role name, which must exist if provided.
  - `verified` (boolean, optional): Whether the user's email is verified.
- Expected responses:
  - `200 OK` if the account is successfully updated.
  - `400 INVALID_REQUEST` if the provided ID is not a valid integer.
  - `404 USER_NOT_FOUND` if no account with the provided ID exists.
  - `404 ROLE_NOT_FOUND` if the provided new role name does not exist.
  - `409 USER_ALREADY_EXISTS` if the new email is already in use by another account.

### `DELETE` `/admin/users/{userId}` (Manager):
Delete a specific user account by ID.
- Path parameters:
  - `userId` (int, required): The unique identifier of the user account.
- Expected responses:
  - `200 OK` if the account is successfully deleted.
  - `400 INVALID_REQUEST` if the provided ID is not a valid integer.
  - `404 USER_NOT_FOUND` if no account with the provided ID exists.


## Open Hours & Holidays

### `GET` `/open-hours` (Public):
Get the library's open hours for each day of the week.
- Expected responses:
  - `200 OK` with the open hours information.
    - list of open hours consisting of:
      - `weekDay` (int): The day of the week (1 for Monday, 7 for Sunday).
      - `openTime` (localtime): The opening time in HH:mm format or null if closed.
      - `closeTime` (localtime): The closing time in HH:mm format or null if closed.

### `GET` `/open-hours/{day}` (Public):
Get the library's open hours for a specific day of the week.
- Path parameters:
  - `day` (int, required): The day of the week (1 for Monday, 7 for Sunday).
- Expected responses:
  - `200 OK` with the open hours information for the specified day.
    - `weekDay` (int): The day of the week (1 for Monday, 7 for Sunday).
    - `openTime` (localtime): The opening time in HH:mm format or null if closed.
    - `closeTime` (localtime): The closing time in HH:mm format or null if closed.
  - `400 INVALID_REQUEST` if the provided day is not a valid integer.
  - `404 WEEK_DAY_NOT_FOUND` if the provided day is not between 1 and 7.

### `PATCH` `/open-hours/{day}` (Manager):
Update the library's open hours for a specific day of the week.
- Path parameters:
    - `day` (int, required): The day of the week (1 for Monday, 7 for Sunday).
- Request body parameters:
    - `openTime` (localtime, optional): The new opening time in HH:mm format, or null to set the day as closed.
    - `closeTime` (localtime, optional): The new closing time in HH:mm format, or null to set the day as closed.
- Expected responses:
    - `200 OK` if the open hours are successfully updated.
    - `400 INVALID_REQUEST` if the provided day is not a valid integer.
    - `400 DATE_TIME_FORMAT_INVALID` if the hours' time formats are invalid.
    - `404 WEEK_DAY_NOT_FOUND` if the provided day is not between 1 and 7.

### `GET` `/open-hours/exceptions` (Public):
Get a list of all holiday exceptions when the open hours are changed.
- Expected responses:
  - `200 OK` with a list of holiday exceptions.
    - list of exceptions consisting of:
      - `date` (localdate): The date of the exception in YYYY-MM-DD format.
      - `openTime` (localtime): The opening time for the exception in HH:mm format or null if closed.
      - `closeTime` (localtime): The closing time for the exception in HH:mm format or null if closed.
      - `comment` (string): A comment describing the reason for the exception.

### `GET` `/open-hours/exceptions/{date}` (Public):
Get the holiday exception for a specific date.
- Path parameters:
  - `date` (localdate, required): The date of the exception in YYYY-MM-DD format.
- Expected responses:
  - `200 OK` with the holiday exception information for the specified date.
    - `date` (localdate): The date of the exception in YYYY-MM-DD format.
    - `openTime` (localtime): The opening time for the exception in HH:mm format or null if closed.
    - `closeTime` (localtime): The closing time for the exception in HH:mm format or null if closed.
    - `comment` (string): A comment describing the reason for the exception.
  - `400 DATE_TIME_FORMAT_INVALID` if the provided date is not in a valid format.
  - `404 OPEN_HOUR_EXCEPTION_NOT_FOUND` if no exception exists for the provided date.

### `PUT` `/open-hours/exceptions/{date}` (Manager):
Create or update a holiday exception for a specific date.
- Path parameters:
  - `date` (localdate, required): The date of the exception in YYYY-MM-DD format.
- Request body parameters:
  - `openTime` (localtime, optional): The opening time for the exception in HH:mm format, or null to set the day as closed.
  - `closeTime` (localtime, optional): The closing time for the exception in HH:mm format, or null to set the day as closed.
  - `comment` (string, optional): A comment describing the reason for the exception.
- Expected responses:
  - `200 OK` if the holiday exception is successfully created or updated.
  - `400 DATE_TIME_FORMAT_INVALID` if the provided date or time formats are invalid.
  
### `DELETE` `/open-hours/exceptions/{date}` (Manager):
Delete a holiday exception for a specific date.
- Path parameters:
  - `date` (localdate, required): The date of the exception in YYYY-MM-DD format.
- Expected responses:
  - `200 OK` if the holiday exception is successfully deleted.
  - `400 DATE_TIME_FORMAT_INVALID` if the provided date is not in a valid format.
  - `404 OPEN_HOUR_EXCEPTION_NOT_FOUND` if no exception exists for the provided date.


## Inventory Search & Management

### `GET` `/inventory/search` (Public):
Search for books in the inventory by title, author, year or ISBN.
- Query parameters:
  - `isbn` (string, optional): Search for books with ISBNs matching strings created by separating this string by commas.
  - `title` (string, optional): Search for books with a title containing strings created by separating this string by commas.
  - `author` (string, optional): Search for books with an author containing strings created by separating this string by commas.
  - `year` (string, optional): Search for books published in years matching strings created by separating this string by commas.
  - `yearFrom` (string, optional): Search for books published from this year onwards.
  - `yearTo` (string, optional): Search for books published up to this year.
- Expected responses:
  - `200 OK` with a list of books matching the search criteria.
    - list of books consisting of:
      - `isbn` (string): The book's ISBN.
      - `title` (string): The title of the book.
      - `author` (string): The author of the book.
      - `year` (string): The publication year of the book.

### `GET` `/inventory` (User, Librarian, Manager):
Get a list of all books in the inventory, with different levels of detail based on the user's role.
- Expected responses:
  - `200 OK` with a list of books.
    - for users a list of unique books consisting of:
      - `isbn` (string): The book's ISBN.
      - `title` (string): The title of the book.
      - `author` (string): The author of the book.
      - `year` (string): The publication year of the book.
    - for librarians and managers a list of all book copies consisting of:
      - `id` (int): The unique identifier of the book copy.
      - `isbn` (string): The book's ISBN.
      - `title` (string): The title of the book.
      - `author` (string): The author of the book.
      - `year` (string): The publication year of the book.

### `POST` `/inventory` (Librarian, Manager):
Add a set of book copies to the inventory by providing their ISBNs, data and quantity.
- Request body parameters:
  - `books` (set, required) of objects consisting of:
    - `isbn` (string, required): The book's ISBN.
    - `title` (string, optional): The title of the book.
    - `author` (string, optional): The author of the book.
    - `year` (string, optional): The publication year of the book.
    - `quantity` (int, optional): The number of copies to add for this book, defaults to 1 if not provided.
- Expected responses:
  - `201 Created` if the book copies are successfully added to the inventory.

### `DELETE` `/inventory` (Librarian, Manager):
Remove a set of book copies from the inventory by providing their IDs.
- Request body parameters:
  - `ids` (set:int, required): The unique identifiers of the book copies to remove.
- Expected responses:
  - `200 OK` if the book copies are successfully removed from the inventory.
  - `404 BOOK_NOT_FOUND` if any of the provided IDs do not correspond to existing book copies.

### `GET` `/inventory/{bookId}` (Librarian, Manager):
Get information about a specific book copy by its ID.
- Path parameters:
  - `bookId` (int, required): The unique identifier of the book copy.
- Expected responses:
  - `200 OK` with the book copy information if found.
    - `id` (int): The unique identifier of the book copy.
    - `isbn` (string): The book's ISBN.
    - `title` (string): The title of the book.
    - `author` (string): The author of the book.
    - `year` (string): The publication year of the book.
  - `400 INVALID_REQUEST` if the provided ID is not a valid integer.
  - `404 BOOK_NOT_FOUND` if no book copy with the provided ID exists in the inventory.

### `PATCH` `/inventory/{bookId}` (Librarian, Manager):
Update the information of a specific book copy by its ID.
- Path parameters:
  - `bookId` (int, required): The unique identifier of the book copy.
- Request body parameters:
  - `isbn` (string, optional): The new ISBN of the book.
  - `title` (string, optional): The new title of the book.
  - `author` (string, optional): The new author of the book.
  - `year` (string, optional): The new publication year of the book.
- Expected responses:
  - `200 OK` if the book copy information is successfully updated.
  - `400 INVALID_REQUEST` if the provided ID is not a valid integer.
  - `404 BOOK_NOT_FOUND` if no book copy with the provided ID exists in the inventory.

### `DELETE` `/inventory/{bookId}` (Librarian, Manager):
Remove a specific book copy from the inventory by its ID.
- Path parameters:
  - `bookId` (int, required): The unique identifier of the book copy.
- Expected responses:
  - `200 OK` if the book copy is successfully removed from the inventory.
  - `400 INVALID_REQUEST` if the provided ID is not a valid integer.
  - `404 BOOK_NOT_FOUND` if no book copy with the provided ID exists in the inventory.

### `GET` `/inventory/isbn/{isbn}` (User, Librarian, Manager):
Get information about a book by its ISBN, with different levels of detail based on the user's role.
- Path parameters:
  - `isbn` (string, required): The book's ISBN.
- Expected responses:
  - `200 OK` with the book information if found.
    - for users:
      - `isbn` (string): The book's ISBN.
      - `title` (string): The title of the book.
      - `author` (string): The author of the book.
      - `year` (string): The publication year of the book.
    - for librarians and managers:
      - list of all book copies consisting of:
        - `id` (int): The unique identifier of the book copy.
        - `isbn` (string): The book's ISBN.
        - `title` (string): The title of the book.
        - `author` (string): The author of the book.
        - `year` (string): The publication year of the book.
  - `404 BOOK_NOT_FOUND` if no book with the provided ISBN exists in the inventory and the request is from a user.


### `GET` `/inventory/isbn/{isbn}/availability` (User, Librarian, Manager):
Get the availability information of a book by its ISBN, including the number of available copies and the number of users currently waiting for it.
- Path parameters:
  - `isbn` (string, required): The book's ISBN.
- Expected responses:
  - `200 OK` with the book availability information if found.
    - `available` (int): The number of available copies of the book.
    - `reserved` (int): The number of active reservations for the book.
  - `404 BOOK_NOT_FOUND` if no book with the provided ISBN exists in the inventory.

### `PATCH` `/inventory/isbn/{isbn}` (Librarian, Manager):
Update the information of a book by its ISBN.
- Path parameters:
  - `isbn` (string, required): The book's ISBN.
- Request body parameters:
  - `isbn` (string, optional): The new ISBN of the book.
  - `title` (string, optional): The new title of the book.
  - `author` (string, optional): The new author of the book.
  - `year` (string, optional): The new publication year of the book.
- Expected responses:
  - `200 OK` if the book information is successfully updated.
  - `404 BOOK_NOT_FOUND` if no book with the provided ISBN exists in the inventory.


## Reservations

### `GET` `/reservations` (User, Librarian, Manager):
Get a list of the authenticated user's active and past reservations if the request is from a user, or of all reservations if the request is from a librarian or manager.
- Expected responses:
  - `200 OK` with a list of reservations.
    - list of reservations consisting of:
      - `id` (int): The unique identifier of the reservation.
      - `isbn` (string): The ISBN of the reserved book.
      - `userId` (int): The unique identifier of the user who made the reservation.
      - `reservedAt` (instant): The timestamp of when the reservation was made.
      - `expiresAt` (instant): The timestamp of when the reservation expires if not picked up.
      - `canceled` (boolean): Whether the reservation was canceled by the user (also when borrowing the reserved book).

### `POST` `/reservations` (User, Librarian, Manager):
Create a new reservation for a book by its ISBN.
- Request body parameters:
  - `isbn` (string, required): The ISBN of the book to reserve.
  - `userId` (int, required for librarians): The unique identifier of the user for whom to create the reservation, required only if the request is from a librarian or manager.
- Expected responses:
  - `201 Created` if the reservation is successfully created.
  - `404 BOOK_NOT_FOUND` if no book with the provided ISBN exists in the inventory.
  - `409 BOOK_ALREADY_RESERVED` if the user already has an active reservation for the same book.

### `POST` `/reservations/batch` (User, Librarian, Manager):
Create multiple reservations for books by their ISBNs in a single request.
- Request body parameters:
  - `isbns` (set:string, required): The ISBNs of the books to reserve.
  - `userId` (int, required for librarians): The unique identifier of the user for whom to create the reservation, required only if the request is from a librarian or manager.
- Expected responses:
  - `201 Created` if the reservations are successfully created.
  - `404 BOOK_NOT_FOUND` if any of the provided ISBNs do not correspond to existing books in the inventory.
  - `409 BOOK_ALREADY_RESERVED` if the user already has an active reservation for any of the provided books.

### `GET` `/reservations/{reservationId}` (User, Librarian, Manager):
Get the details of a specific reservation by its ID if it belongs to the authenticated user or if the request is from a librarian or manager.
- Path parameters:
  - `reservationId` (int, required): The unique identifier of the reservation.
- Expected responses:
  - `200 OK` with the reservation information if found and accessible.
    - `id` (int): The unique identifier of the reservation.
    - `isbn` (string): The ISBN of the reserved book.
    - `userId` (int): The unique identifier of the user who made the reservation.
    - `reservedAt` (instant): The timestamp of when the reservation was made.
    - `expiresAt` (instant): The timestamp of when the reservation expires if not picked up.
    - `canceled` (boolean): Whether the reservation was canceled by the user.
  - `400 INVALID_REQUEST` if the provided ID is not a valid integer.
  - `404 RESERVATION_NOT_FOUND` if no reservation with the provided ID exists or if it belongs to another user and the request is from a regular user.

### `PATCH` `/reservations/{reservationId}/cancel` (User, Librarian, Manager):
Cancel a specific reservation by its ID if it belongs to the authenticated user or if the request is from a librarian or manager.
- Path parameters:
  - `reservationId` (int, required): The unique identifier of the reservation.
- Expected responses:
  - `200 OK` if the reservation is successfully canceled.
  - `400 INVALID_REQUEST` if the provided ID is not a valid integer.
  - `404 RESERVATION_NOT_FOUND` if no reservation with the provided ID exists, it belongs to another user and the request is from a regular user, or is already canceled.

### `GET` `/reservations/isbn/{isbn}` (Librarian, Manager):
Get a list of all reservations for a book by its ISBN.
- Path parameters:
  - `isbn` (string, required): The book's ISBN.
- Expected responses:
  - `200 OK` with a list of reservations for the specified book if found.
    - list of reservations consisting of:
      - `id` (int): The unique identifier of the reservation.
      - `isbn` (string): The ISBN of the reserved book.
      - `userId` (int): The unique identifier of the user who made the reservation.
      - `reservedAt` (instant): The timestamp of when the reservation was made.
      - `expiresAt` (instant): The timestamp of when the reservation expires if not picked up.
      - `canceled` (boolean): Whether the reservation was canceled by the user.
  - `404 BOOK_NOT_FOUND` if no book with the provided ISBN exists in the inventory.

### `GET` `/reservations/user/{userId}` (Librarian, Manager):
Get a list of all reservations made by a specific user.
- Path parameters:
  - `userId` (int, required): The unique identifier of the user.
- Expected responses:
  - `200 OK` with a list of reservations made by the specified user if found.
    - list of reservations consisting of:
      - `id` (int): The unique identifier of the reservation.
      - `isbn` (string): The ISBN of the reserved book.
      - `userId` (int): The unique identifier of the user who made the reservation.
      - `reservedAt` (instant): The timestamp of when the reservation was made.
      - `expiresAt` (instant): The timestamp of when the reservation expires if not picked up.
      - `canceled` (boolean): Whether the reservation was canceled by the user.
  - `404 USER_NOT_FOUND` if no user with the provided ID exists.


## Borrowing & Returning

### `GET` `/borrows` (User, Librarian, Manager):
Get a list of the authenticated user's active and past borrows if the request is from a user, or of all borrows if the request is from a librarian or manager.
- Expected responses:
  - `200 OK` with a list of borrows.
    - list of borrows consisting of:
      - `id` (int): The unique identifier of the borrow.
      - `bookId` (int): The unique identifier of the borrowed book copy.
      - `userId` (int): The unique identifier of the user who made the borrow.
      - `borrowedAt` (instant): The timestamp of when the borrow was made.
      - `endsAt` (instant): The timestamp of when the borrow is due.
      - `returnedAt` (instant): The timestamp of when the book was returned, or null if not returned yet.

### `POST` `/borrows` (Librarian, Manager):
Create a new borrow for a book copy by its ID, if the book is available for the user.
- Request body parameters:
  - `isbn` (string, required): The ISBN of the book to borrow, used to find an available copy.
  - `userId` (int): The unique identifier of the user for whom to create the borrow.
- Expected responses:
  - `201 Created` if the borrow is successfully created.
  - `404 BOOK_NOT_FOUND` if no book with the provided ISBN exists in the inventory.
  - `409 NO_BORROWABLE_BOOKS_LEFT` if there are no available copies of the book to borrow.

### `POST` `/borrows/batch` (Librarian, Manager):
Create multiple borrows for book copies by their ISBNs in a single request, if the books are available for the user.
- Request body parameters:
  - `isbns` (set:string, required): The ISBNs of the books to borrow, used to find available copies.
  - `userId` (int): The unique identifier of the user for whom to create the borrows.
- Expected responses:
  - `201 Created` if the borrows are successfully created.
  - `404 BOOK_NOT_FOUND` if any of the provided ISBNs do not correspond to existing books in the inventory.
  - `409 NO_BORROWABLE_BOOKS_LEFT` if there are not enough available copies of any of the books to borrow.

### `GET` `/borrows/{borrowId}` (User, Librarian, Manager):
Get the details of a specific borrow by its ID if it belongs to the authenticated user or if the request is from a librarian or manager.
- Path parameters:
  - `borrowId` (int, required): The unique identifier of the borrow.
- Expected responses:
  - `200 OK` with the borrow information if found and accessible.
    - `id` (int): The unique identifier of the borrow.
    - `bookId` (int): The unique identifier of the borrowed book copy.
    - `userId` (int): The unique identifier of the user who made the borrow.
    - `borrowedAt` (instant): The timestamp of when the borrow was made.
    - `endsAt` (instant): The timestamp of when the borrow is due.
    - `returnedAt` (instant): The timestamp of when the book was returned, or null if not returned yet.
  - `400 INVALID_REQUEST` if the provided ID is not a valid integer.
  - `404 BORROW_NOT_FOUND` if no borrow with the provided ID exists or if it belongs to another user and the request is from a regular user.

### `GET` `/borrows/{borrowId}/fee` (User, Librarian, Manager):
Calculate the overdue fee for a specific borrow by its ID if it belongs to the authenticated user or if the request is from a librarian or manager.
- Path parameters:
  - `borrowId` (int, required): The unique identifier of the borrow.
- Expected responses:
  - `200 OK` with the fee information if the borrow is found and accessible.
    - `fee` (double): The calculated fee for the borrow in PLN, 0 if the borrow is not overdue.
  - `400 INVALID_REQUEST` if the provided ID is not a valid integer.
  - `404 BORROW_NOT_FOUND` if no borrow with the provided ID exists or if it belongs to another user and the request is from a regular user.

### `PATCH` `/borrows/{borrowId}/extend` (User, Librarian, Manager):
Extend the due date of a specific borrow by its ID if it belongs to the authenticated user or if the request is from a librarian or manager.
- Path parameters:
  - `borrowId` (int, required): The unique identifier of the borrow.
- Expected responses:
  - `200 OK` if the borrow is successfully extended.
  - `400 INVALID_REQUEST` if the provided ID is not a valid integer.
  - `404 BORROW_NOT_FOUND` if no borrow with the provided ID exists or if it belongs to another user and the request is from a regular user.
  - `409 BORROW_EXTENSION_FORBIDDEN` if the borrow cannot be extended due to existing reservations and low availability of the book.

### `PATCH` `/borrows/{borrowId}/return` (Librarian, Manager):
Mark a specific borrow as returned by its ID.
- Path parameters:
  - `borrowId` (int, required): The unique identifier of the borrow.
- Expected responses:
  - `200 OK` if the borrow is successfully marked as returned.
  - `400 INVALID_REQUEST` if the provided ID is not a valid integer.
  - `404 BORROW_NOT_FOUND` if no borrow with the provided ID exists or if it is already marked as returned.

### `GET` `/borrows/book/{bookId}` (Librarian, Manager):
Get a list of all borrows for a specific book copy by its ID.
- Path parameters:
  - `bookId` (int, required): The unique identifier of the book copy.
- Expected responses:
  - `200 OK` with a list of borrows for the specified book copy if found.
    - list of borrows consisting of:
      - `id` (int): The unique identifier of the borrow.
      - `bookId` (int): The unique identifier of the borrowed book copy.
      - `userId` (int): The unique identifier of the user who made the borrow.
      - `borrowedAt` (instant): The timestamp of when the borrow was made.
      - `endsAt` (instant): The timestamp of when the borrow is due.
      - `returnedAt` (instant): The timestamp of when the book was returned, or null if not returned yet.
  - `404 BOOK_NOT_FOUND` if no book copy with the provided ID exists in the inventory.

### `GET` `/borrows/user/{userId}` (Librarian, Manager):
Get a list of all borrows made by a specific user.
- Path parameters:
  - `userId` (int, required): The unique identifier of the user.
- Expected responses:
  - `200 OK` with a list of borrows made by the specified user if found.
    - list of borrows consisting of:
      - `id` (int): The unique identifier of the borrow.
      - `bookId` (int): The unique identifier of the borrowed book copy.
      - `userId` (int): The unique identifier of the user who made the borrow.
      - `borrowedAt` (instant): The timestamp of when the borrow was made.
      - `endsAt` (instant): The timestamp of when the borrow is due.
      - `returnedAt` (instant): The timestamp of when the book was returned, or null if not returned yet.
  - `404 USER_NOT_FOUND` if no user with the provided ID exists.


## Internal API

### `POST` `/internal/send-email-notifications` (Internal):
Send email notifications for available book reservations, upcoming return deadlines and overdue borrows.
- Expected responses:
  - `200 OK` if the notifications are successfully sent.
  - `401 Unauthorized` if the request does not include a valid internal API key in the `Authorization` header.

### `POST` `/internal/cleanup-expired-tokens` (Internal):
Delete expired refresh, email verification and password reset tokens from the database.
- Expected responses:
  - `200 OK` if the expired tokens are successfully deleted.
  - `401 Unauthorized` if the request does not include a valid internal API key in the `Authorization` header.
