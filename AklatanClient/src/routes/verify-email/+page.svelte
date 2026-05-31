<script lang="ts">
    import { onMount } from 'svelte';
    import { page } from '$app/state';
    import { redirect } from '@sveltejs/kit';
    import { goto } from '$app/navigation';
    import Header from '../../header.svelte';

    onMount(async () => {
        const tokenFromUrl = page.url.searchParams.get('token');
        console.log(typeof tokenFromUrl)

        if (!tokenFromUrl) {
            window.alert("Bad verification link")
            goto("/")
            return;
        }

        const res = await fetch('http://localhost:8080/auth/verify-email', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ token: tokenFromUrl })
        });
        console.log(res)

        if (res.ok) {
            window.alert("Your account has been verified")
            goto("/")
        } else {
            window.alert("Bad verification link or the token has expired");
            goto("/")
        }
    });
</script>
<Header></Header>
<h1>Verifying...</h1>