<script lang="ts">
    import { onMount } from "svelte";
    import { apiFetch,logout } from "$lib/auth";
    import Header from "../../header.svelte";
    import "../../css/basic.css"

    let user = $state<any>(null);
    let isLoading = $state(true);

    let update_name = $state('');
    let update_old_password = $state('');
    let update_new_password = $state('');

    async function fetchAccountDetails(): Promise<void> {
        try {
            const res = await apiFetch('/account', {
                method: 'GET'
            });

            if (res.status === 200) {
                user = await res.json();
            } else if (res.status === 401) {
                window.alert("Sesja wygasła lub brak autoryzacji. Zaloguj się ponownie.");
            } else {
                console.error("Błąd podczas pobierania danych użytkownika");
            }
        } catch (error) {
            console.error("Network error fetching account:", error);
        } finally {
            isLoading = false;
        }
    }

    onMount(() => {
        fetchAccountDetails();
    });

    async function updateAccount(): Promise<void> {
        if (update_new_password !== "" && update_old_password === "") {
            window.alert("Old password is required to set a new password.");
            return;
        }

        const body: Record<string, string> = {};
        if (update_name !== "") body.newName = update_name;
        if (update_old_password !== "") body.password = update_old_password;
        if (update_new_password !== "") body.newPassword = update_new_password;

        if (Object.keys(body).length === 0) {
            window.alert("Please fill at least one field to update.");
            return;
        }

        try {
            const res = await apiFetch('/account', {
                method: 'PATCH',
                body: JSON.stringify(body)
            });

            if (res.status === 200) {
                window.alert("Account details updated successfully!");
                
                update_old_password = '';
                update_new_password = '';
                update_name = '';

                await fetchAccountDetails();
            } else if (res.status === 400) {
                window.alert("Invalid request.");
            } else if (res.status === 401) {
                window.alert("Incorrect old password!");
            }
        } catch (error) {
            console.error("Failed to update account:", error);
        }
    }
</script>

<Header></Header>
<div class="main-container">
    <h1>Your Account</h1>
    <button onclick={logout} style="font-size:1.2em">LOGOUT</button>

    <section class="profile-info">
        <h3>Profile Information</h3>

        {#if isLoading}
            <p>Loading account details...</p>
        {:else}
            {#if user}
                <table border="1" class="tables-container">
                    <tbody>
                        <tr>
                            <th>Full Name</th>
                            <td>{user.name}</td>
                        </tr>
                        <tr>
                            <th>Email Address</th>
                            <td>{user.email}</td>
                        </tr>
                        <tr>
                            <th>Role</th>
                            <td ><strong>{user.role}</strong></td>
                        </tr>
                    </tbody>
                </table>
            {:else}
                <p style="color: red;">Could not load user data. Please ensure you are logged in.</p>
            {/if}
        {/if}
    </section>

    <section class="user-form">
        <h3>Update Account Details</h3>
        <form class="search-form user-form" onsubmit={(e) => e.preventDefault()}>
                <label for="up-name">New Full Name:</label>
                <input id="up-name" type="text" placeholder="Leave empty if no change" bind:value={update_name}>
                <label for="up-old-pass">Current Password (required for password changes):</label>
                <input id="up-old-pass" type="password" placeholder="Current password" bind:value={update_old_password}>
                <label for="up-new-pass">New Password:</label>
                <input id="up-new-pass" type="password" placeholder="New password" bind:value={update_new_password}>
            <input type="submit" value="Save Changes" onclick={updateAccount} >
        </form>
    </section>
</div>