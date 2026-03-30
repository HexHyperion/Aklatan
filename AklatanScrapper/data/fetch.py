import psycopg

MAX_ISBN_COUNT = 50000

with psycopg.connect("dbname=openlibrary user=postgres password=postgres") as sourceConnection:

    with sourceConnection.cursor() as sourceCursor:
        with open("isbn.txt", "r", encoding="utf-8") as file:
            with open("isbn-random.txt", "r", encoding="utf-8") as randomFile:
                with open("isbn-data.txt", "a", encoding="utf-8") as outputFile:
                    isbns = [line.strip() for line in file]
                    isbns.extend([line.strip() for line in randomFile])

                    count = 1
                    totalCount = len(isbns)

                    for isbn in isbns:
                        sourceCursor.execute("""
                            select
                                e.data->>'title',
                                a.data->>'name',
                                e.data->>'publish_date'
                            from editions e 
                            join edition_isbns ei
                                on ei.edition_key = e.key                 
                            join works w
                                on w.key = e.work_key
                            join author_works a_w
                                on a_w.work_key = w.key
                            join authors a
                                on a_w.author_key = a.key
                            where ei.isbn = %s;                     
                        """, (isbn,))

                        output = sourceCursor.fetchone()
                        data = f"({count}/{totalCount}) {isbn}"

                        if output is None:
                            print(f"{data}\tunknown\t\t")
                            pass
                        else:
                            title, author, year = output
                            print(f"{data}\t{title}\t{author}\t{year}")
                            outputFile.write(f"{isbn}\t{title}\t{author}\t{year}\n")
                            count += 1

                        if count > MAX_ISBN_COUNT:
                            break
                    
                    print(f"Total ISBNs: {totalCount}")
                    print(f"Valid ISBNs: {count}")

    sourceConnection.commit()