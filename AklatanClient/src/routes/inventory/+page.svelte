<script lang="ts">
    import { onMount } from "svelte";
    import Header from "../../header.svelte";
    import { apiFetch,role } from "$lib/auth";
    import "../../css/basic.css";
    import { goto } from "$app/navigation";

    let isLoading = $state(true); 

    let allFetchedBooks = $state<any[]>([]);
    let filteredBooksList = $state<any[]>([]); 
    
    let sortOrder = $state<"none" | "asc" | "desc">("none");

    let currentPage = $state(0);
    const ITEMS_PER_PAGE = 100; 
    let pageInput = $state("1"); 

    let processedBooks = $derived.by(() => {
        let list = [...filteredBooksList];
        if (sortOrder === "asc") {
            list.sort((a, b) => a.id - b.id);
        } else if (sortOrder === "desc") {
            list.sort((a, b) => b.id - a.id);
        }
        return list;
    });

    let totalPages = $derived(Math.ceil(processedBooks.length / ITEMS_PER_PAGE) || 1);
    let visibleBooks = $derived(
        processedBooks.slice(
            currentPage * ITEMS_PER_PAGE,
            currentPage * ITEMS_PER_PAGE + ITEMS_PER_PAGE
        )
    );

    $effect(() => {
        pageInput = (currentPage + 1).toString();
    });

    let s_id = $state("");
    let s_isbn = $state("");
    let s_title = $state("");
    let s_author = $state("");
    let s_year = $state("");

    let addDialog: HTMLDialogElement;
    let updateDialog: HTMLDialogElement;

    let n_isbn = $state("");
    let n_title = $state("");
    let n_author = $state("");
    let n_year = $state("");
    let n_quantity = $state(1);

    let selectedBookId = $state<number | null>(null);
    let u_isbn = $state("");
    let u_title = $state("");
    let u_author = $state("");
    let u_year = $state("");

    onMount(() => {
        if ($role == "user"){
            goto("/browse")
        }
        fetchAllBooks();
    });

    async function fetchAllBooks() {
        isLoading = true; 
        try {
            const res = await apiFetch('/inventory', { method: 'GET' });
            if (res.ok) {
                const data = await res.json();
                allFetchedBooks = data;
                filteredBooksList = [...data];
                currentPage = 0;
            } else {
                console.error("Couldn't get books data");
            }
        } finally {
            isLoading = false; 
        }
    }

    function changePage(direction: boolean) {
        if (direction && currentPage < totalPages - 1) {
            currentPage++;
        } else if (!direction && currentPage > 0) {
            currentPage--;
        }
    }

    function handlePageJump(e: Event) {
        e.preventDefault();
        const targetPage = parseInt(pageInput, 10);

        if (!isNaN(targetPage) && targetPage >= 1 && targetPage <= totalPages) {
            currentPage = targetPage - 1; 
        } else {
            pageInput = (currentPage + 1).toString();
            alert(`Insert a number from 1 to ${totalPages}`);
        }
    }

    function handleSearch(e: Event) {
        e.preventDefault();
        
        filteredBooksList = allFetchedBooks.filter(book => {
            let matches = true;

            if (s_id) {
                const searchIds = s_id.split(',').map(s => s.trim());
                matches = matches && searchIds.some(id => 
                    (book.id || '').toString().includes(id)
                );
            }

            if (s_isbn) {
                const searchIsbns = s_isbn.split(',').map(s => s.trim().toLowerCase());
                matches = matches && searchIsbns.some(isbn => 
                    (book.isbn || '').toLowerCase().includes(isbn)
                );
            }

            if (s_title) {
                const searchTitles = s_title.split(',').map(s => s.trim().toLowerCase());
                matches = matches && searchTitles.some(title => 
                    (book.title || '').toLowerCase().includes(title)
                );
            }

            if (s_author) {
                const searchAuthors = s_author.split(',').map(s => s.trim().toLowerCase());
                matches = matches && searchAuthors.some(author => 
                    (book.author || '').toLowerCase().includes(author)
                );
            }

            if (s_year) {
                matches = matches && (book.year || '').toString().includes(s_year.toString().trim());
            }

            return matches;
        });

        currentPage = 0; 
    }

    function clearFilters() {
        s_id = s_isbn = s_title = s_author = s_year = "";
        sortOrder = "none";
        filteredBooksList = [...allFetchedBooks]; 
        currentPage = 0;
    }

    async function handleAddBooks(e: Event) {
        e.preventDefault();
        const payload = {
            books: [{
                isbn: n_isbn,
                title: n_title || undefined,
                author: n_author || undefined,
                year: n_year || undefined,
                quantity: n_quantity
            }]
        };

        const res = await apiFetch('/inventory', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.status === 201) {
            addDialog.close();
            window.alert("Book(s) added succesfully")
            n_isbn = n_title = n_author = n_year = ""; 
            n_quantity = 1;
            fetchAllBooks(); 
        } else {
            console.error("Nie udało się dodać książek");
        }
    }

    function openUpdateDialog(book: any) {
        selectedBookId = book.id;
        u_isbn = book.isbn || "";
        u_title = book.title || "";
        u_author = book.author || "";
        u_year = book.year || "";
        updateDialog.showModal();
    }

    async function handleUpdateBook(e: Event) {
        e.preventDefault();
        if (!selectedBookId) return;

        const payload = {
            isbn: u_isbn || undefined,
            title: u_title || undefined,
            author: u_author || undefined,
            year: u_year || undefined
        };

        const res = await apiFetch(`/inventory/${selectedBookId}`, {
            method: 'PATCH',
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            updateDialog.close();
            fetchAllBooks(); 
        } else {
            console.error("Nie udało się zaktualizować książki");
        }
    }

    async function deleteBook(id: number) {
        if (!confirm("Czy na pewno chcesz usunąć ten egzemplarz z systemu?")) return;
        
        const res = await apiFetch(`/inventory/${id}`, { method: 'DELETE' });
        
        if (res.ok) {
            allFetchedBooks = allFetchedBooks.filter(book => book.id !== id);
            filteredBooksList = filteredBooksList.filter(book => book.id !== id);
        } else {
            console.error("Nie udało się usunąć książki");
        }
    }
</script>

<Header></Header>
<div class="main-container">
    <div class="top-container">
    <div class="header-options">
        <h1>Browse books</h1>
        <div>       
            <button onclick={() => addDialog.showModal()}>Add new books to the system</button> 
            <div>
                <label for="sort-order">Sort by Book ID:</label>
                <select id="sort-order" bind:value={sortOrder} disabled={isLoading} >
                    <option value="none">Default</option>
                    <option value="asc">ID: Low to High ↑</option>
                    <option value="desc">ID: High to Low ↓</option>
                </select>
            </div>
        </div>
    </div>

    <div>
        <form onsubmit={handleSearch} class="search-form">
            <input type="text" placeholder="ID" bind:value={s_id}>
            <input type="text" placeholder="isbn" bind:value={s_isbn}>
            <input type="text" placeholder="title" bind:value={s_title}>
            <input type="text" placeholder="author" bind:value={s_author}>
            <input type="number" placeholder="year" bind:value={s_year}> 
            <input type="submit" value="SEARCH" disabled={isLoading}>
            <button type="button" onclick={clearFilters} disabled={isLoading}>CLEAR FILTERS</button>
        </form>
    </div>

        
    </div>

    {#if isLoading}
        <div style="text-align: center; padding: 2rem;">
            <h1>Loading...</h1>
        </div>
    {:else}
        <div class="pagination-controls">
            <button onclick={() => changePage(false)} disabled={currentPage === 0 || isLoading}>
                Previous
            </button>
            
            <span>Page {currentPage + 1} of {totalPages}</span>
            
            <button onclick={() => changePage(true)} disabled={currentPage === totalPages - 1 || isLoading}>
                Next
            </button>

            <form onsubmit={handlePageJump} >
                <label for="page-jump">Go to page:</label>
                <input 
                    id="page-jump"
                    type="number" 
                    min="1" 
                    max={totalPages} 
                    bind:value={pageInput} 
                    disabled={isLoading}
                />
                <button type="submit" disabled={isLoading}>Go</button>
            </form>
        </div>

        {#if visibleBooks.length === 0}
            <p>No books</p>
        {:else}
            <ul class="book-display">
                {#each visibleBooks as book (book.id)}
                    <li>
                        <a href="/book/{book.isbn}">
                            <strong>[ID: {book.id}]</strong> {book.isbn} - {book.title || 'Brak tytułu'} ({book.author}) - {book.year}
                        </a>
                        <button onclick={() => openUpdateDialog(book)} >Update info</button> 
                        <button onclick={() => deleteBook(book.id)}>Delete</button> 
                    </li>
                {/each}
            </ul>
        {/if}
    {/if}

    <dialog bind:this={addDialog}>
        <h2>Add New Books</h2>
        <form onsubmit={handleAddBooks}>
            <div><label>ISBN: <input type="text" bind:value={n_isbn} required></label></div>
            <div><label>Title: <input type="text" bind:value={n_title}></label></div>
            <div><label>Author: <input type="text" bind:value={n_author}></label></div>
            <div><label>Year: <input type="text" bind:value={n_year}></label></div>
            <div><label>Quantity: <input type="number" min="1" bind:value={n_quantity}></label></div>
            <div>
                <button type="submit" disabled={isLoading}>Add Book(s)</button>
                <button type="button" onclick={() => addDialog.close()}>Cancel</button>
            </div>
        </form>
    </dialog>

    <dialog bind:this={updateDialog}>
        <h2>Update Book Info</h2>
        <form onsubmit={handleUpdateBook}>
            <div><label>ISBN: <input type="text" bind:value={u_isbn}></label></div>
            <div><label>Title: <input type="text" bind:value={u_title}></label></div>
            <div><label>Author: <input type="text" bind:value={u_author}></label></div>
            <div><label>Year: <input type="text" bind:value={u_year}></label></div>
            <div>
                <button type="submit" disabled={isLoading}>Save Changes</button>
                <button type="button" onclick={() => updateDialog.close()}>Cancel</button>
            </div>
        </form>
    </dialog>

</div>