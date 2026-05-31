<script lang="ts">
    import Header from "../../header.svelte";
    import { apiFetch } from "$lib/auth";
    import { onMount } from "svelte";
    import "../../css/basic.css";

    let borrows = $state<any[]>([]);
    let reservations = $state<any[]>([]);
    let isLoading = $state(true);

    let extendDialog = $state<HTMLDialogElement | null>(null);
    let selectedBorrowId = $state<number | null>(null);
    let isSubmittingExtend = $state(false);

    onMount(async () => {
        await refreshLibraryData();
        isLoading = false;
    });

    async function refreshLibraryData() {
        try {
            const res = await apiFetch('/borrows', { method: 'GET' });
            if (res.ok) {
                const borrowsData = await res.json();
            
                borrows = await Promise.all(borrowsData.map(async (borrow: any) => {
                    let fee = 0;
                    const isOverdue = borrow.returnedAt === null && new Date(borrow.endsAt).getTime() < Date.now();
                    
                    if (isOverdue) {
                        try {
                            const feeRes = await apiFetch(`/borrows/${borrow.id}/fee`, { method: 'GET' });
                            if (feeRes.ok) {
                                const feeData = await feeRes.json();
                                fee = feeData.fee;
                            }
                        } catch (err) {
                            console.error(`Couldn't fetch overdue fee for borrow #${borrow.id}`, err);
                        }
                    }
                    return { ...borrow, fee, isOverdue };
                }));
            }

            const res2 = await apiFetch('/reservations', { method: 'GET' });
            if (res2.ok) {
                reservations = await res2.json();
                console.log("reservations :")
                console.log(reservations)
            }
        } catch (error) {
            window.alert("Network error occured while loading users data")
            console.error("Error occured", error);
        }
    }

    async function cancelReservation(reservationId: number) {
        if (!confirm("Are you sure you want to cancel this reservation?")) return;

        try {
            const res = await apiFetch(`/reservations/${reservationId}/cancel`, { method: 'PATCH' });
            if (res.ok) {
                alert("Reservation cancelled successfully");
                await refreshLibraryData();
            } else {
                alert("Failed to cancel reservation");
            }
        } catch (err) {
            console.error(err);
        }
    }

    function openExtendModal(borrowId: number) {
        selectedBorrowId = borrowId;
        extendDialog?.showModal();
    }

    async function submitExtension() {
        if (!selectedBorrowId) return;
        isSubmittingExtend = true;

        try {
            const res = await apiFetch(`/borrows/${selectedBorrowId}/extend`, {
                method: 'PATCH'
            });

            if (res.ok) {
                alert("Borrow limit extended successfully!");
                extendDialog?.close();
                await refreshLibraryData();
            } else if (res.status === 409) {
                alert("This book cannot be extended due to pending reservations or critically low availability.");
            } else if (res.status === 404) {
                alert("Borrow record not found or access denied.");
            } else {
                alert(`Request could not be processed.`);
            }
        } catch (err) {
            console.error("Extension request failed:", err);
            alert("Network error, extension failed.");
        } finally {
            isSubmittingExtend = false;
        }
    }
</script>

<Header></Header>
<h1>My library</h1>

{#if isLoading}
    <p>Loading library assets...</p>
{:else}
    <h3>My borrows</h3>
    {#if borrows && borrows.length > 0}
        <ul class="book-display">
            {#each borrows as borrowed_book}
                <li>
                    <div>
                        <strong>{borrowed_book.title}</strong> by {borrowed_book.author} <br>
                        <span >(ISBN: {borrowed_book.isbn})</span>
                    </div>

                    <div>
                        {#if borrowed_book.returnedAt}
                            <span>Returned on: {new Date(borrowed_book.returnedAt).toLocaleDateString()}</span>
                        {:else}
                            <span>Expires at: {new Date(borrowed_book.endsAt).toLocaleDateString()}</span>
                            
                            {#if borrowed_book.isOverdue}
                                <div>
                                    Overdue! Calculated Fee: {borrowed_book.fee.toFixed(2)} PLN
                                </div>
                            {/if}

                            <div style="margin-top: 6px;">
                                <button onclick={() => openExtendModal(borrowed_book.id)}>
                                    Extend Borrow Time
                                </button>
                            </div>
                        {/if}
                    </div>
                </li>
            {/each}
        </ul>
    {:else}
        <p>You don't have any borrowed books.</p>
    {/if}

    <h3>My reservations</h3>
    {#if reservations && reservations.length > 0}
        <ul class="book-display">
            {#each reservations as reserved_book}
                {#if reserved_book.canceled == false}
                <li>
                    <div>
                        <strong>{reserved_book.title}</strong> by {reserved_book.author} <br>
                        <span>(ISBN: {reserved_book.isbn})</span>
                    </div>

                    <div>
                        {#if reserved_book.canceled}
                            <span>Canceled or Fulfilled</span>
                        {:else}
                            <span>Expires at -> {new Date(reserved_book.expiresAt).toLocaleString()}</span>
                            <button onclick={() => cancelReservation(reserved_book.id)} >
                                Cancel
                            </button>
                        {/if}
                    </div>
                </li>
                {/if}
            {/each}
        </ul>
        <h4>Canceled or ended</h4>
        <ul class="book-display">
            {#each reservations as reserved_book}
                {#if reserved_book.canceled}
                <li>
                    <div>
                        <strong>{reserved_book.title}</strong> by {reserved_book.author} <br>
                        <span>(ISBN: {reserved_book.isbn})</span>
                    </div>

                    <div>
                        {#if reserved_book.canceled}
                            <span >Canceled or ended</span>
                        {:else}
                            <span>Expires at -> {new Date(reserved_book.expiresAt).toLocaleString()}</span>
                            <button onclick={() => cancelReservation(reserved_book.id)}>
                                Cancel
                            </button>
                        {/if}
                    </div>
                </li>
                {/if}
            {/each}
        </ul>
    {:else}
        <p>You don't have any reserved books.</p>
    {/if}
{/if}

<dialog bind:this={extendDialog} >
    <h3>Extend Borrow Time</h3>
    <p>Are you sure you want to request an extension for borrow record <strong>#{selectedBorrowId}</strong>?</p>
    <p>Note: Extension might be rejected if other users are waiting for this book.</p>
    
    <div>
        <button type="button" onclick={() => extendDialog?.close()} disabled={isSubmittingExtend}>
            Cancel
        </button>
        <button type="button" onclick={submitExtension} disabled={isSubmittingExtend}>
            {isSubmittingExtend ? "Extending..." : "Confirm"}
        </button>
    </div>
</dialog>