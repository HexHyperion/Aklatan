# Aklatan - the best library management service
Hello there

Some day there will be some description

Thank you


## Planned features (WIP)
### For clients
- self registration
- account management
- catalog/inventory browsing
  + search
  + filters???
  + topic/author/tag/etc. relations???
  + reviews???
- book reservation
  + multiple/basket
  + expiring after some time
  + for available and borrowed books
  + no extension if someone reserves while it's borrowed
- borrowed book list
  + current, past, reserved, watched???
- rental time extension (e.g. +1 month)
- e-mail return reminders
- late return fee

### For librarians
- account management???
- catalog/inventory browsing
  + with borrow data (count, users)
- book add/edit???/delete to inventory
  + as barcode scanning out of question, openLibrary API search when adding
- book reservation for clients
  + with override
- book borrow/return
  + barcodes would be cool, but again, out of question
- rental time extension
  + with custom time???
- e-mail sending to users???

### For admins???
- librarian account creation/deletion
- multiple library management???
- library open hours???
- all librarian permissions


## Technicalities
### Technologies
- ??? for frontend
- Kotlin/Ktor for backend
- Postgres/Exposed for DB
- OpenLibrary API for ISBNs

### Application
- system for one library OR general system for multiple libraries (sort of like Librus is for many schools)?????
- books identified by ISBN
- WIP...


## API Endpoints
### Authentication
`/login` (Users, Librarians) - self explanatory

`/register` (U) - with e-mail verification

`/account`??? (UL) - account info management

### Browsing
`/catalog` (L) - all books (probably from OpenLibrary API), used mainly for search while adding books to inventory

`/inventory` (UL) - allows simplified GET for users, supports GET, PUT, PATCH and DELETE for librarians

### Borrowing
`/reserve` (UL) - preset time for users, librarians can set an expiration date for the reservation

`/borrow` (L) - sets one or more books as borrowed

`/extend` (UL) - librarians can set custom time/return date

`/return` (L) - responds with a fee if returned late