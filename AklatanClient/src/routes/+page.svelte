<script lang="ts">
    import { onMount } from "svelte";
    import Header from "../header.svelte";

    const API_BASE = "http://localhost:8080";


    let openHours = $state<any[]>([]);
    let upcomingExceptions = $state<any[]>([]);
    let isLoadingInfo = $state(true);
    let sortedOpenHours = $derived([...openHours].sort((a, b) => a.weekDay - b.weekDay));


    let s_isbn = $state("");
    let s_title = $state("");
    let s_author = $state("");
    let s_year = $state("");
    
    let searchResults = $state<any[] | null>(null);
    let isSearching = $state(false);

    function getWeekdayName(dayIndex: number): string {
        const days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
        return days[dayIndex - 1] || "Unknown";
    }

    onMount(() => {
        fetchLibraryInfo();
    });

    async function fetchLibraryInfo() {
        isLoadingInfo = true;
        try {
            const hoursRes = await fetch(`${API_BASE}/open-hours`, { method: 'GET' });
            if (hoursRes.ok) {
                openHours = await hoursRes.json();
            }

            const exceptionsRes = await fetch(`${API_BASE}/open-hours/exceptions`, { method: 'GET' });
            if (exceptionsRes.ok) {
                const allExceptions = await exceptionsRes.json();
                
                const today = new Date().toISOString().split('T')[0];
                upcomingExceptions = allExceptions
                    .filter((ex: any) => ex.date >= today)
                    .sort((a: any, b: any) => new Date(a.date).getTime() - new Date(b.date).getTime())
                    .slice(0, 3);
            }
        } catch (error) {
            window.alert("Couldn't load library informations")
        } finally {
            isLoadingInfo = false;
        }
    }

    async function handleSearch(e: Event) {
        e.preventDefault();
        isSearching = true;

        try {
            const params = new URLSearchParams();
            if (s_isbn) params.append("isbn", s_isbn);
            if (s_title) params.append("title", s_title);
            if (s_author) params.append("author", s_author);
            if (s_year) params.append("year", s_year);

            // no search params
            if (Array.from(params.keys()).length === 0) {
                searchResults = [];
                return;
            }

            const res = await fetch(`${API_BASE}/inventory/search?${params.toString()}`, { method: 'GET' });
            
            if (res.ok) {
                searchResults = await res.json();
            } else {
                window.alert("Couldn't load search results")
                searchResults = [];
            }
        } catch (error) {
            window.alert("Couldn't load search results")
            searchResults = [];
        } finally {
            isSearching = false;
        }
    }
</script>

<Header></Header>
<h1>Welcome to Aklatan library!</h1>

<div class="library-info">
    <div class="open-hours">
        <h2>Open Hours</h2>
        {#if isLoadingInfo}
            <p>Loading hours...</p>
        {:else if sortedOpenHours.length > 0} 
            <ul>
                {#each sortedOpenHours as day} 
                    <li>
                        <strong>{getWeekdayName(day.weekDay)}:</strong> 
                        {#if day.openTime && day.closeTime}
                            {day.openTime} - {day.closeTime}
                        {:else}
                            <em>Closed</em>
                        {/if}
                    </li>
                {/each}
            </ul>
        {:else}
            <p>No open hours information available.</p>
        {/if}
    </div>

    <div class="exceptions">
        <h2>Upcoming Holidays & Exceptions</h2>
        {#if isLoadingInfo}
            <p>Loading exceptions...</p>
        {:else if upcomingExceptions.length > 0}
            <ul>
                {#each upcomingExceptions as exception}
                    <li>
                        <strong>{exception.date}</strong> 
                        {#if exception.comment}
                            ({exception.comment})
                        {/if}:
                        
                        {#if exception.openTime && exception.closeTime}
                            {exception.openTime} - {exception.closeTime}
                        {:else}
                            <em>Closed</em>
                        {/if}
                    </li>
                {/each}
            </ul>
        {:else}
            <p>No upcoming exceptions. Regular hours apply.</p>
        {/if}
    </div>
</div>

<hr>

<h2>Check if we have the book you are looking for</h2>
<form onsubmit={handleSearch} style="margin-bottom: 1rem;">
    <input type="text" placeholder="ISBN" bind:value={s_isbn} disabled={isSearching}>
    <input type="text" placeholder="Title" bind:value={s_title} disabled={isSearching}>
    <input type="text" placeholder="Author" bind:value={s_author} disabled={isSearching}>
    <input type="number" placeholder="Year" bind:value={s_year} disabled={isSearching}> 
    <input type="submit" value="SEARCH" disabled={isSearching}>
</form>

<div class="search-results">
    {#if isSearching}
        <p>Searching...</p>
    {:else if searchResults !== null}
        <h3>Search Results:</h3>
        {#if searchResults.length > 0}
            <ul>
                {#each searchResults as book}
                    <li>
                        <strong>{book.title || 'Unknown Title'}</strong> by {book.author || 'Unknown Author'} 
                        (Year: {book.year}, ISBN: {book.isbn})
                        <br>
                        <a href="/account">Log in to reserve or see details</a>
                    </li>
                {/each}
            </ul>
        {:else}
            <p>No books found matching your criteria.</p>
        {/if}
    {/if}
</div>