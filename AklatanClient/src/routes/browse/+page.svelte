<script lang="ts">
    import { onMount } from "svelte";
    import Header from "../../header.svelte";
    import { apiFetch } from "$lib/auth";
    import "../../css/basic.css";

    let isLoading = $state(true);


    let allFetchedBooks = $state<any[]>([]); 
    let filteredBooksList = $state<any[]>([]);

    let currentPage = $state(0);
    const ITEMS_PER_PAGE = 100; 
    let pageInput = $state("1");

    let totalPages = $derived(Math.ceil(filteredBooksList.length / ITEMS_PER_PAGE) || 1);

    let visibleBooks = $derived(
        filteredBooksList.slice(
            currentPage * ITEMS_PER_PAGE,
            currentPage * ITEMS_PER_PAGE + ITEMS_PER_PAGE
        )
    );


    $effect(() => {
        pageInput = (currentPage + 1).toString();
    });

    let s_isbn = $state("");
    let s_title = $state("");
    let s_author = $state("");
    let s_year = $state("");

    onMount(async () => {
        await fetchAllBooks();
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
                console.error("Nie udało się pobrać danych");
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
            alert(`Enter a number from 1 to ${totalPages}`);
        }
    }

    function handleSearch(e: Event) {
        e.preventDefault();
        
        filteredBooksList = allFetchedBooks.filter(book => {
            let matches = true;
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
        s_isbn = s_title = s_author = s_year = "";
        filteredBooksList = [...allFetchedBooks];
        currentPage = 0;
    }
</script>

<Header></Header>
<div class="main-container">
    <div class="top-container">
    <div class="header-options">
    <h1>Browse</h1>
    <form onsubmit={handleSearch} class="search-form">
        <input type="text" placeholder="ISBN" bind:value={s_isbn}>
        <input type="text" placeholder="Title" bind:value={s_title}>
        <input type="text" placeholder="Author" bind:value={s_author}>
        <input type="number" placeholder="Year" bind:value={s_year}> 
        <input type="submit" value="SEARCH" disabled={isLoading}>
        <button type="button" onclick={clearFilters} disabled={isLoading}>CLEAR FILTERS</button>
    </form>

    </div>
    </div>

    {#if isLoading}
        <h3>Loading...</h3>
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
            <p>No matching books</p>
        {:else}
            <ul class="book-display">
                {#each visibleBooks as book}
                    <li>
                        <a href="/book/{book.isbn}">
                            {book.title || 'Brak tytułu'} - {book.author} ({book.isbn})
                        </a>
                    </li>
                {/each}
            </ul>
        {/if}
    {/if}
</div>