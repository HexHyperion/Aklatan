# Aklatan - the best library management service
Hello there

Some day there will be some description

Thank you


## Planned features (WIP)
### For clients
- self registration
- account management
- catalog/inventory browsing
  - search
  - filters???
  - topic/author/tag/etc. relations???
  - reviews???
- book reservation
  - multiple/basket
  - expiring after some time
  - for available and borrowed books
  - user queue ordered by reservation time
  - no extension if someone reserves while it's borrowed
  - e-mail notification when reserved book becomes available
- borrowed book list
  - current, past, reserved, watched???
- rental time extension (e.g. +1 month)
- e-mail return reminders
- late return fee

### For librarians
- account management
- catalog/inventory browsing
  - with borrow data (count, users)
- book add/edit???/delete to inventory
  - as barcode scanning out of question, openLibrary API search when adding
- book reservation for clients
  - with override
- book borrow/return
  - barcodes would be cool, but again, out of question, so just relying on trust lol
- rental time extension
  - with custom time???
- e-mail sending to users???

### For admins???
- librarian account creation/deletion
- library open hours???
- all librarian permissions


## Technicalities
### Technologies
- ??? for frontend
- Kotlin/Ktor for backend
- PostgreSQL/Exposed for DB
- OpenLibrary API for ISBNs

### Application
- system for one library
- books identified by ISBN
- WIP...


## API Endpoints
### Authentication
`/login` (Users, Librarians) - user must send an object with name and password<br>
Method: `POST`

`/register` (U) - with e-mail verification<br>
Method: `POST`

`/account` (UL) - fetches and updates account information<br>
Method: `GET`, `PATCH`

`/logout` (UL)<br>
Method: `POST`

### Browsing
`/catalog` (L) - all books overall (probably from OpenLibrary API), used mainly for search while adding books to inventory<br>
Method: `GET`

`/inventory` (UL) - all books in library, allows simple GET requests for users and CRUD for librarians, maybe grant edit/delete only to admins???<br>
Method: `GET`, `POST`, `PATCH`, `DELETE`

### Borrowing
`/books` (UL) - fetches info about books of different status for a user, return object contains basic book data, statuses like "borrowed" or "returned" and additional info like whether someone reserved the book (cannot extend)<br>
Method: `GET`

`/reserve` (UL) - puts a user in the reservation queue for a book, the reservation has a preset time for users, and librarians can set an expiration date for the reservation<br>
Method: `POST`

`/borrow` (L) - sets one or more books as borrowed by a user, first checking if someone else didn't reserve the last piece<br>
Method: `POST`

`/extend` (UL) - changes the return time of a specific book for a user, librarians can set custom time/return date<br>
Method: `POST`

`/return` (L) - sets one or more borrowed books as returned, responds with a fee if any were returned late<br>
Method: `POST`

### Administration
`/roles` (A) - show all user roles with permissions<br>
Method: `GET`

`/users` (A) - create and delete accounts of all roles<br>
Method: `POST`, `DELETE`

`/hours` (A) - set library open hours, maybe with some exceptions for holidays or just 2 weeks ahead<br>
Method: `POST`