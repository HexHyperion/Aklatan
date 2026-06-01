<script lang="ts">
    import { onMount } from "svelte";
    import Header from "../../header.svelte";
    import { apiFetch,role } from "$lib/auth";
    import "../../css/basic.css";
    import { goto } from "$app/navigation";

    let isLoading = $state(true);

    let allReservations = $state<any[]>([]);

    let r_isbn = $state("");
    let r_email = $state("");

    let filterIsbn = $state("");
    let filterEmail = $state("");

    let activeReservations = $derived(
        allReservations.filter(r => !r.canceled && new Date(r.expiresAt).getTime() >= Date.now())
    );

    let pastReservations = $derived(
        allReservations.filter(r => r.canceled || new Date(r.expiresAt).getTime() < Date.now())
    );

    onMount(async () => {
        if ($role == "user"){
            goto("/browse")
        }
        await fetchReservations();
        isLoading = false;
    });


    async function fetchReservations() {
        try {
            const res = await apiFetch('/reservations', { method: 'GET' });
            if (res.ok) {
                allReservations = await res.json();
            } else {
                console.error("Couldn't get reservations list");
            }
        } catch (err) {
            window.alert("Network error while trying to get reservations")
            console.error("Network error while trying to get reservations:", err);
        }
    }

    async function handleCreateReservation(e: Event) {
        e.preventDefault();
        if (!r_isbn) return;

        const payload: any = { isbn: r_isbn };
        if (r_email.trim() !== "") {
            payload.email = r_email;
        }

        try {
            const res = await apiFetch('/reservations', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            if (res.status === 201) {
                alert("Reservation created successfully!");
                r_isbn = "";
                r_email = "";
                await fetchReservations();
            } else if (res.status === 404) {
                alert("Error: Book not found in inventory.");
            } else if (res.status === 409) {
                alert("User already has an active reservation for this book.");
            } else {
                alert("Failed to create reservation.");
            }
        } catch (err) {
            console.error(err);
        }
    }

    async function cancelReservation(id: number) {
        if (!confirm(`Are you sure you want to cancel reservation #${id}?`)) return;

        try {
            const res = await apiFetch(`/reservations/${id}/cancel`, { method: 'PATCH' });
            if (res.ok) {
                alert("Reservation canceled.");
                await fetchReservations(); 
            } else {
                alert("Failed to cancel reservation or it is already canceled/expired.");
            }
        } catch (err) {
            console.error(err);
        }
    }


    async function handleSearchByIsbn(e: Event) {
        e.preventDefault();
        if (!filterIsbn) return;
        isLoading = true;

        try {
            const res = await apiFetch(`/reservations/isbn/${filterIsbn}`, { method: 'GET' });
            if (res.ok) {
                allReservations = await res.json();
            } else {
                alert("No reservations found for this ISBN or Book not found.");
                allReservations = [];
            }
        } catch (err) {
            console.error(err);
        } finally {
            isLoading = false;
        }
    }

    async function handleSearchByEmail(e: Event) {
        e.preventDefault();
        if (!filterEmail) return;
        isLoading = true;

        try {
            const res = await apiFetch(`/reservations/user/${filterEmail}`, { method: 'GET' });
            if (res.ok) {
                allReservations = await res.json();
            } else {
                alert("No reservations found for this user or User not found.");
                allReservations = [];
            }
        } catch (err) {
            console.error(err);
        } finally {
            isLoading = false;
        }
    }

    function clearFilters() {
        filterIsbn = "";
        filterEmail = "";
        fetchReservations();
    }
</script>


<Header></Header>
<div class="main-container neutral-container">
    <h1>Reservations Desk</h1>

    {#if isLoading}
        <h2>Loading reservations...</h2>
    {:else}
        <div class="top-container space-out">
            <div >
                <h3>Create a New Reservation</h3>
                <form onsubmit={handleCreateReservation} class="search-form">
                    <div>
                        <label>Book ISBN: <input type="text" bind:value={r_isbn} required></label>
                    </div>
                    <div>
                        <label>User Email: <input type="email" bind:value={r_email}></label>
                    </div>
                    <button type="submit">Reserve Book</button>
                </form>
            </div>

            <div >
                <h3>Search & Filters</h3>
                
                <form onsubmit={handleSearchByIsbn} >
                    <input type="text" placeholder="Search by ISBN" bind:value={filterIsbn} required>
                    <button type="submit">Search ISBN</button>
                </form>

                {#if filterIsbn || filterEmail}
                    <button type="button" onclick={clearFilters}>Clear Filters</button>
                {/if}
            </div>

        </div>


        <div class="tables-container">
            <h3>Active Reservations</h3>
            {#if activeReservations.length === 0}
                <p>No active reservations found.</p>
            {:else}
                <table border="1" >
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Book ISBN</th>
                            <th>User</th>
                            <th>Reserved At</th>
                            <th>Expires At</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {#each activeReservations as res (res.id)}
                            <tr>
                                <td>{res.id}</td>
                                <td><a href="/book/{res.isbn}">{res.isbn}</a></td>
                                <td>{res.email}</td>
                                <td>{new Date(res.reservedAt).toLocaleString()}</td>
                                <td>{new Date(res.expiresAt).toLocaleString()}</td>
                                <td>
                                    <button onclick={() => cancelReservation(res.id)}>
                                        Cancel Reservation
                                    </button>
                                </td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            {/if}
        </div>

        <div class="tables-container">
            <h3>Past & Canceled Reservations</h3>
            {#if pastReservations.length === 0}
                <p>No historical reservations.</p>
            {:else}
                <table border="1" >
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Book ISBN</th>
                            <th>User</th>
                            <th>Status</th>
                            <th>Expires At</th>
                        </tr>
                    </thead>
                    <tbody>
                        {#each pastReservations as res (res.id)}
                            <tr>
                                <td>{res.id}</td>
                                <td>{res.isbn}</td>
                                <td>{res.email}</td>
                                <td>
                                    {#if res.canceled}
                                        <span style=" font-weight: bold;">Canceled</span>
                                    {:else}
                                        <span>Expired / Picked up</span>
                                    {/if}
                                </td>
                                <td>{new Date(res.expiresAt).toLocaleString()}</td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            {/if}
        </div>
    {/if}
</div>