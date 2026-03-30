import re
import psycopg
import random

MIN_SINGLE_BOOK_COUNT = 1
MAX_SINGLE_BOOK_COUNT = 25

with psycopg.connect("dbname=aklatan user=postgres password=postgres") as targetConnection:
    with targetConnection.cursor() as targetCursor:

        with open("isbn-data.txt", "r", encoding="utf-8") as file:

            count = 0
            for line in file:
                try:
                    isbn, title, author, year = line.strip().split("\t")
                    year = re.findall(r"\d{4}", year)[0] if re.findall(r"\d{4}", year) else None

                    if title == "None" or author == "None" or year == "None" or year is None:
                        print(f"({isbn})\tMISSING DATA, SKIPPING!")
                        continue

                except ValueError:
                    print(f"({isbn})\tINVALID LINE FORMAT, SKIPPING!")
                    continue

                print(f"({isbn})\t{title}\t{author}\t{year}")
                count += 1

                inserts = random.randint(MIN_SINGLE_BOOK_COUNT, MAX_SINGLE_BOOK_COUNT)

                for _ in range(inserts):
                    targetCursor.execute("""
                        insert into books (isbn, title, author, year)
                        values (%s, %s, %s, %s);
                    """, (isbn, title, author, year))

            print(f"Total unique valid entries inserted: {count}")

            rows = targetCursor.execute("select count(*) from books;").fetchone()[0]
            print(f"Total rows in books table: {rows}")

    targetConnection.commit()