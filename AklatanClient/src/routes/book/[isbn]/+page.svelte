<script lang="ts">
    import Header from '../../../header.svelte';
    import { page } from '$app/state';
    import { onMount } from 'svelte';
    import { apiFetch, role } from '$lib/auth';
    import '../../../css/basic.css';


    let isbn = $derived(page.params.isbn);
    
    let bookDetails = $state<any>(null);
    let bookAvailability = $state<any>(null);
    let available = $derived(bookAvailability?.available || 0);
    let reserved = $derived(bookAvailability?.reserved || 0);


    let isLoading = $state(false);


    let bookReservations = $state<any[]>([]);
    let bookBorrows = $state<any[]>([]);
    let bookCopies = $state<any[]>([]);
    let hasLoadedManagerData = $state(false);


    onMount(async () => {
        const res = await apiFetch(`/inventory/isbn/${isbn}`, { method: 'GET' });
        if (res.ok) {
            bookDetails = await res.json();
            console.log("BOOK DETAILS")
            console.log(bookDetails)
            console.log(bookDetails[0])
        } else {
            console.error("Nie udało się pobrać szczegółów książki");
        }

        const res2 = await apiFetch(`/inventory/isbn/${isbn}/availability`, { method: 'GET' });
        if (res2.ok) {
            bookAvailability = await res2.json();
        } else {
            console.error("Nie udało się pobrać dostępności książki");
        }
    });

    $effect(() => {
        if (($role === "manager" || $role === "librarian") && !hasLoadedManagerData && isbn) {
            hasLoadedManagerData = true;
            loadManagementData();
        }
    });

    async function loadManagementData() {
        try {
            const resReq = await apiFetch(`/reservations/isbn/${isbn}`, { method: 'GET' });
            if (resReq.ok) bookReservations = await resReq.json();

            const borReq = await apiFetch(`/borrows/isbn/${isbn}`, { method: 'GET' });
            if (borReq.ok) bookBorrows = await borReq.json();

            const copiesReq = await apiFetch(`/inventory/isbn/${isbn}`, { method: 'GET' });
            if (copiesReq.ok) bookCopies = await copiesReq.json();
        } catch (err) {
            console.error("Błąd podczas ładowania danych zarządzania:", err);
        }
    }

    async function createReservation() {
        isLoading = true;
        const requestBody = { isbn };

        try {
            const res = await apiFetch('/reservations', {
                method: 'POST',
                body: JSON.stringify(requestBody)
            });

            if (res.status === 201) {
                window.alert("Książka została pomyślnie zarezerwowana!");
                if ($role === "manager" || $role === "librarian") loadManagementData();
                return;
            }
            if (res.status === 409) {
                window.alert("Book already reserved");
            } else {
                window.alert(`Server error ${res.status}.`);
            }
        } catch (error) {
            console.error("Reservation request failed:", error);
            window.alert("Reservation request failed");
        } finally {
            isLoading = false;
        }
    }


    async function cancelReservation(resId: number) {
        if (!confirm("Delete this reservation?")) return;
        try {
            const res = await apiFetch(`/reservations/${resId}/cancel`, { method: 'PATCH' });
            if (res.ok) {
                alert("Reservation deleted/canceled.");
                await loadManagementData();
            } else {
                alert("Failed to cancel reservation.");
            }
        } catch (err) {
            console.error(err);
        }
    }


</script>

<Header></Header>

<h1>Szczegóły książki</h1>
<p>ISBN: <strong>{isbn}</strong></p>

{#if bookDetails}
{#if $role =="user"}
    <div>
        <h2>{bookDetails.title}</h2>
        <p>Autor: {bookDetails.author}</p>
        <p>Rok wydania: {bookDetails.year}</p>
    </div>
{:else}
    <div>
        <h2>{bookDetails[0].title}</h2>
        <p>Autor: {bookDetails[0].author}</p>
        <p>Rok wydania: {bookDetails[0].year}</p>
    </div>
{/if}
{:else}
    <p>Ładowanie danych książki...</p>
{/if}

{#if $role === "user"}
    {#if available > 0}
        <button onclick={createReservation} disabled={isLoading}>Reserve the book</button>
    {:else}
        <p>This book is currently unavailable</p>
        <p>There are {reserved || 0} users waiting for this book.</p>
    {/if}
{/if}

<br>

{#if $role === "manager" || $role === "librarian"}

    <h3>Copies of the book in the library</h3>
    {#if bookCopies.length > 0}
        <ul>
            {#each bookCopies as copy}
                <li>Copy ID: {copy.id}</li>
            {/each}
        </ul>
    {:else}
        <p>No other copies found.</p>
    {/if}

    <h3>List of all reservations for the book (ISBN: {isbn})</h3>
    {#if bookReservations.length > 0}
        <table border="1" >
            <thead>
                <tr>
                    <th>ID</th>
                    <th>User ID</th>
                    <th>Expires At</th>
                    <th>Status</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>
                {#each bookReservations as r}
                    <tr>
                        <td>{r.id}</td>
                        <td>{r.userId}</td>
                        <td>{new Date(r.expiresAt).toLocaleString()}</td>
                        <td>{r.canceled ? 'Canceled' : 'Active'}</td>
                        <td>
                            {#if !r.canceled}
                                <button onclick={() => cancelReservation(r.id)}>Delete</button>
                            {/if}
                        </td>
                    </tr>
                {/each}
            </tbody>
        </table>
    {:else}
        <p>No reservations.</p>
    {/if}
{/if}
<br>
