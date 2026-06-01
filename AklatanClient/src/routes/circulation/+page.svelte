<script lang="ts">
    import { onMount } from "svelte";
    import Header from "../../header.svelte";
    import { apiFetch,role} from "$lib/auth";
    import "../../css/basic.css";
    import { goto } from "$app/navigation";

    let isLoading = $state(true);
    let isManager = $state(false);


    let allBorrows = $state<any[]>([]);
    let allUsers = $state<any[]>([]);

    let b_isbn = $state("");
    let b_email = $state("");


    let searchEmail = $state("");
    let searchIsbn = $state("");

    let extendDialog: HTMLDialogElement;
    let selectedBorrowId = $state<number | null>(null);
    let isSubmittingExtend = $state(false);

    let filteredBorrows = $derived(
        searchIsbn.trim() !== "" 
            ? allBorrows.filter(b => b.isbn && b.isbn.toLowerCase().includes(searchIsbn.toLowerCase()))
            : allBorrows
    );

    let activeBorrows = $derived(
        filteredBorrows.filter(b => b.returnedAt === null && new Date(b.endsAt).getTime() >= Date.now())
    );

    let overdueBorrows = $derived(
        filteredBorrows.filter(b => b.returnedAt === null && new Date(b.endsAt).getTime() < Date.now())
    );

    let returnedBorrows = $derived(
        filteredBorrows.filter(b => b.returnedAt !== null)
    );

    onMount(async () => {
        if ($role == "user"){
            goto("/browse")
        }
        await Promise.all([
            fetchBorrows(),
        ]);
        isLoading = false;
    });


    async function fetchBorrows() {
        try {
            const res = await apiFetch('/borrows', { method: 'GET' });
            if (res.ok) {
                allBorrows = await res.json();
            } else {
                console.error("Couldn't get borrows");
            }
        } catch (err) {
            window.alert("Network error couldn't get borrows")
            console.error("Network error couldn't get borrows", err);
        }
    }

    async function handleSearchByUser(e: Event) {
        e.preventDefault();
        if (!searchEmail.trim()) return;
        isLoading = true;

        try {
            const res = await apiFetch(`/borrows/user/${searchEmail.trim()}`, { method: 'GET' });
            if (res.ok) {
                allBorrows = await res.json();
            } else if (res.status === 404) {
                alert("User not found or has no borrows.");
                allBorrows = [];
            } else {
                alert("Error fetching user borrows.");
            }
        } catch (err) {
            console.error(err);
        } finally {
            isLoading = false;
        }
    }

    async function clearUserSearch() {
        searchEmail = "";
        isLoading = true;
        await fetchBorrows();
        isLoading = false;
    }

    async function handleBorrow(e: Event) {
        e.preventDefault();
        if (!b_isbn || !b_email) return;

        try {
            const res = await apiFetch('/borrows', {
                method: 'POST',
                body: JSON.stringify({ isbn: b_isbn, email: b_email })
            });

            if (res.status === 201) {
                alert("Book borrowed successfully!");
                b_isbn = "";
                b_email = "";
                await fetchBorrows();
            } else if (res.status === 409) {
                alert("Error: No borrowable copies left for this book.");
            } else {
                alert("Error: Book or user not found.");
            }
        } catch (err) {
            console.error(err);
        }
    }


    function openExtendDialog(borrowId: number) {
        selectedBorrowId = borrowId;
        extendDialog.showModal();
    }

    async function handleExtendSubmit(e: Event) {
        e.preventDefault();
        if (!selectedBorrowId) return;
        isSubmittingExtend = true;

        try {
            const res = await apiFetch(`/borrows/${selectedBorrowId}/extend`, {
                method: 'PATCH'
            });

            if (res.ok) {
                alert(`Borrow #${selectedBorrowId} extended successfully!`);
                extendDialog.close();
                await fetchBorrows();
            } else if (res.status === 409) {
                alert("Borrow extension forbidden due to existing reservations or low availability.");
            } else {
                alert(`Failed to extend borrow.`);
            }
        } catch (err) {
            console.error(err);
            alert("Network error, extension failed.");
        } finally {
            isSubmittingExtend = false;
        }
    }

    async function executeReturn(borrowId: number) {
        if (!confirm(`Are you sure you want to return borrow #${borrowId}?`)) return;
        try {
            const res = await apiFetch(`/borrows/${borrowId}/return`, { method: 'PATCH' });
            if (res.ok) {
                alert(`Borrow #${borrowId} marked as returned.`);
                await fetchBorrows();
            } else {
                alert(`Failed to return borrow #${borrowId}. Already returned or not found.`);
            }
        } catch (err) {
            console.error(err);
        }
    }

    async function checkFee(borrowId: number) {
        try {
            const res = await apiFetch(`/borrows/${borrowId}/fee`, { method: 'GET' });
            if (res.ok) {
                const data = await res.json();
                alert(`Current overdue fee for borrow #${borrowId} is: ${data.fee} PLN`);
            }
        } catch (err) {
            console.error(err);
        }
    }
</script>

<Header></Header>
<div class="main-container neutral-container">
    <h1>Circulation desk</h1>

    {#if isLoading}
        <h2>Loading desk data...</h2>
    {:else}
        <div class="top-container space-out">
            <div>
                <h3>Borrow a book to user</h3>
                <form onsubmit={handleBorrow} class="search-form">
                    <div>
                        <label>ISBN: <input type="text" bind:value={b_isbn} required></label>
                    </div>
                    <div >
                        <label>User Email: <input type="email" bind:value={b_email} required></label>
                    </div>
                    <button type="submit">Borrow Book</button>
                </form>
            </div>

            <div >
                <h3>Get specific user's borrows</h3>
                <form onsubmit={handleSearchByUser} class="search-form">
                    <input type="email" placeholder="Enter user email..." bind:value={searchEmail} required>
                    <button type="submit">Search User</button>
                    {#if searchEmail}
                        <button type="button" onclick={clearUserSearch} >Clear</button>
                    {/if}
                    
                </form>
            </div>

        </div>


        <div class="tables-container" style="margin-bottom: 30px;">
            <h3>Active Borrows list</h3>
            {#if activeBorrows.length === 0}
                <p>No active borrows at the moment.</p>
            {:else}
                <table border="1" style="width: 100%; text-align: left; border-collapse: collapse;">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Book Copy ID</th>
                            <th>User ID</th>
                            <th>Borrowed At</th>
                            <th>Due Date</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {#each activeBorrows as borrow (borrow.id)}
                            <tr>
                                <td>{borrow.id}</td>
                                <td>{borrow.bookId}</td>
                                <td>{borrow.userId}</td>
                                <td>{new Date(borrow.borrowedAt).toLocaleString()}</td>
                                <td>{new Date(borrow.endsAt).toLocaleDateString()}</td>
                                <td>
                                    <button onclick={() => executeReturn(borrow.id)}>Mark as returned</button>
                                    <button onclick={() => openExtendDialog(borrow.id)} style="margin-left: 5px;border: none; cursor: pointer; border-radius: 3px;">
                                        Extend Borrow
                                    </button>
                                </td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            {/if}
        </div>

        <div class="tables-container" style="margin-bottom: 30px;">
            <h3>Overdue borrows list</h3>
            {#if overdueBorrows.length === 0}
                <p style="color: green; font-weight: bold;">No overdue borrows! Everyone is on time.</p>
            {:else}
                <table border="1" style="width: 100%; text-align: left; border-collapse: collapse; border-color: red;">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Book Copy ID</th>
                            <th>User ID</th>
                            <th>Due Date</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {#each overdueBorrows as borrow (borrow.id)}
                            <tr style="color: red;">
                                <td><strong>{borrow.id}</strong></td>
                                <td>{borrow.bookId}</td>
                                <td>{borrow.userId}</td>
                                <td>{new Date(borrow.endsAt).toLocaleDateString()} (Overdue)</td>
                                <td>
                                    <button onclick={() => checkFee(borrow.id)}>Check Fee</button>
                                    <button onclick={() => executeReturn(borrow.id)} style="font-weight: bold; margin-left: 5px;">Mark as returned</button>
                                    <button onclick={() => openExtendDialog(borrow.id)} style="margin-left: 5px; color: white; border: none; padding: 4px 8px; cursor: pointer; border-radius: 3px;">
                                        Extend Borrow
                                    </button>
                                </td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            {/if}
        </div>

        <div class="tables-container" style="margin-bottom: 30px;">
            <h3>Returned Borrows history</h3>
            {#if returnedBorrows.length === 0}
                <p style="color: #666;">No returned borrows found.</p>
            {:else}
                <table border="1" style="width: 100%; text-align: left; border-collapse: collapse;">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Book Copy ID</th>
                            <th>User ID</th>
                            <th>Borrowed At</th>
                            <th>Returned At</th>
                        </tr>
                    </thead>
                    <tbody>
                        {#each returnedBorrows as borrow (borrow.id)}
                            <tr>
                                <td>{borrow.id}</td>
                                <td>{borrow.bookId}</td>
                                <td>{borrow.userId}</td>
                                <td>{new Date(borrow.borrowedAt).toLocaleString()}</td>
                                <td style="color: green; font-weight: bold;">{new Date(borrow.returnedAt).toLocaleString()}</td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            {/if}
        </div>
    {/if}

    <dialog bind:this={extendDialog} style="padding: 20px; border-radius: 8px; border: 1px solid #ccc; width: 350px;">
        <h2>Extend Borrow #{selectedBorrowId}</h2>
        <form onsubmit={handleExtendSubmit}>
            <p>Are you sure you want to extend the due date for this borrow record?</p>
            <p style="font-size: 0.85em; color: #666;">
                Note: The new due date will be calculated automatically by the system. Extension might be forbidden if other users are waiting for this book.
            </p>
            <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
                <button type="button" onclick={() => extendDialog.close()} disabled={isSubmittingExtend} style="background: #ccc;">
                    Cancel
                </button>
                <button type="submit" disabled={isSubmittingExtend}>
                    {isSubmittingExtend ? "Extending..." : "Confirm Extension"}
                </button>
            </div>
        </form>
    </dialog>
</div>