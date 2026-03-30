import psycopg

BOOK_COUNT = 4000

with psycopg.connect("dbname=openlibrary user=postgres password=postgres") as sourceConnection:
    with sourceConnection.cursor() as sourceCursor:
        with open("isbn-random.txt", "a", encoding="utf-8") as file:
            print(f"Fetching {BOOK_COUNT} random ISBNs from the database...")
            
            sourceCursor.execute("""
                select
                    ei.isbn
                from edition_isbns ei
                order by random()
                limit %s;
            """, (BOOK_COUNT,))

            for row in sourceCursor:
                isbn = row[0]
                print(isbn)
                file.write(isbn + "\n")

    sourceConnection.commit()