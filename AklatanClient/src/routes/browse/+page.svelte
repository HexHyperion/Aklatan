<script lang="ts">
    import Header from "../../header.svelte";

    let s_isbn = "";
    let s_title = "";
    let s_author = "";
    let s_year = "";

</script>

<Header></Header>
<h1>Browse book</h1>

<h3>Search books</h3>
<form action="">
<!-- make it change on change not on button and multiple categories at once -->
    <input type="text" placeholder="isbn" bind:value={s_isbn}>
    <input type="text" placeholder="title" bind:value={s_title}>
    <input type="text" placeholder="author" bind:value={s_author}>
    <input type="number" name="" id="" placeholder="year" bind:value={s_year}> 
    <input type="submit" value="SEARCH">
</form>