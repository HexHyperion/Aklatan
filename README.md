# Aklatan - your awesome and absolutely not boring library management service
Hello there

Some day there will be some description

Thank you


## Installation & Running

The server is normally built using Gradle, but the project uses Docker Compose, so, having ensured that you have Docker installed and running, **just launch the following command in the project root to build and start all services:**

```bash
docker compose up --build
```

This will download/build the client, server, DB and scheduler images and run the necessary migrations to set up the database with starter data. The service will be accessible on localhost at the configured port.

**The services depend on multiple environment variables, which must be set in an `.env` file in the project root.** If your file has a different name, make sure to specify it inside `compose.yaml`. A template file is provided as `.env.template`, which you can copy and modify as needed. Most of the variables are self-explanatory, but below is a brief description of the less obvious ones:

- `JWT_SECRET`: A random string used to sign JWT tokens, which you can generate using a password manager or an online generator. It shouldn't be shorter than 32 characters, and a longer one is more secure.
- `INTERNAL_API_KEY`: A random string used to authenticate requests between the server and the scheduler, which you can also generate with different tools (or just smash the keyboard). It can be of any length, but obviously longer is safer.
- `DEVELOPMENT_MODE`: Toggles the built-in Ktor development mode. Additionally, it determines whether the server has CORS policy enabled and if the cookies are set with the `Secure` flag.
- `ALLOWED_ORIGINS`: A comma-separated list of allowed origins for CORS policy, required if the development mode is disabled. The addreses should not contain the protocol prefix.
- `VERIFY_EMAIL_URL` and `RESET_PASSWORD_URL`: The URLs included in the verification and password reset emails sent to users, pointing to frontend routes able to process the tokens. For an example frontend running at `http://localhost:3000`, they can look like `http://localhost:3000/verify-email` or `http://localhost:3000/reset-password`.

The server also has a configuration file `application.yaml` in the `/src/main/resources` folder, where you can configure additional settings such as token expiration times, overdue fines and durations for various reservation and borrowing rules.


## Features?
- User registration and JWT authentication
- Role-based access control (users, librarians, managers)
- Self and administrator account management
- E-mail verification and password reset messages
- Library inventory browsing with search
- Inventory management for librarians
- Book reservation with batch options, expiration time and queue
- Book borrowing with availability checks, batch options, extensions and late return fees
- E-mail book availability notifications and return reminders
- Reservation and borrow history for users
- Information about library open hours and holidays

## Technologies
- Svelte for frontend
- Kotlin/Ktor for backend
- PostgreSQL/Exposed for DB
- Docker Compose for deployment
- Supercronic as scheduler
