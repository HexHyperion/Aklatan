<script lang="ts">
    import { onMount } from 'svelte';
    import { page } from '$app/state';
    import { goto } from '$app/navigation';
    import Header from '../../header.svelte';

    let token = $state("");
    let newPassword = $state("");
    let confirmPassword = $state("");
    
    let isLoading = $state(true);
    let isSubmitting = $state(false);

    onMount(() => {
        const tokenFromUrl = page.url.searchParams.get('token');

        if (!tokenFromUrl) {
            window.alert("Bad or missing password reset link.");
            goto("/");
            return;
        }

        token = tokenFromUrl;
        isLoading = false;
    });

    async function handleResetPassword(e: Event) {
        e.preventDefault();

        if (newPassword !== confirmPassword) {
            window.alert("Passwords do not match!");
            return;
        }

        isSubmitting = true;

        try {
            const res = await fetch('http://localhost:8080/auth/reset-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ 
                    token: token, 
                    newPassword: newPassword 
                })
            });

            if (res.status === 200) {
                window.alert("Your password has been successfully reset.");
                goto("/account"); 
            } else if (res.status === 401) {
                window.alert("The password reset token is invalid or has expired ");
            } else {
                window.alert("An unexpected error occurred. Please try again later.");
            }
        } catch (err) {
            window.alert("A network error occured. Could not connect to the authentication server.");
        } finally {
            isSubmitting = false;
        }
    }
</script>

<Header></Header>

{#if isLoading}
    <div >
        <h2>Validating your reset token...</h2>
    </div>
{:else}
    <div >
        <h1 style="margin-top: 0;">Input the new password</h1>
        
        <form onsubmit={handleResetPassword}>
            <div style="margin-bottom: 15px;">
                <label for="new-password" >
                    New Password:
                </label>
                <input 
                    id="new-password"
                    type="password" 
                    bind:value={newPassword} 
                    required 
                    minlength="6"
                />
            </div>
            
            <div style="margin-bottom: 20px;">
                <label for="confirm-password" >
                    Confirm New Password:
                </label>
                <input 
                    id="confirm-password"
                    type="password" 
                    bind:value={confirmPassword} 
                    required 
                />
            </div>

            <button 
                type="submit" 
                disabled={isSubmitting} 
                >
                {#if isSubmitting}
                    Resetting password...
                {:else}
                    Change Password
                {/if}
            </button>
        </form>
    </div>
{/if}