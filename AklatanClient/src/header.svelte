<script lang="ts">
    import { isAuthenticated,role } from "$lib/auth";
    import { get } from "svelte/store";
    import "./css/header.css";
    import "./css/basic.css";

    // if user logged in add my library
</script>


<header>
    <ul>
        {#if $isAuthenticated && $role=="user"}
            <li><a href="/browse">Browse</a></li>
            <li><a href="/my-library">My library</a></li>
            <li><a href="/dashboard">My account</a></li>
        {:else if $isAuthenticated && ($role=="librarian" || $role == "manager")}
            <li><a href="/inventory">Inventory panel</a></li>
            <li><a href="/circulation">Circulation desk</a></li>
            <li><a href="/reservations">Reservations</a></li>
             <li><a href="/dashboard">My account</a></li>
        {:else}
            <li><a href="/">See the library</a></li>
            <li><a href="/account">Log in</a></li>
        {/if}
    </ul>
</header>