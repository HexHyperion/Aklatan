<script>
    import Header from "../header.svelte";

    // check if already logged in if yes then browse ifno thnlogin and change menubar

    // fetch all of the books and display them (take filters into account)
    let s_isbn = "";
    let s_title = "";
    let s_author = "";
    let s_year = "";

    async function getOpenHours(){
        console.log("get hours")
        const res = await fetch(`http://localhost:8080/open-hours`, {
            method: 'GET',
        });

        const data = await res.json
        console.log(data)

    }
    // getOpenHours()


    
</script>

<Header></Header>
<h1>Hello! you can browse books</h1>
<h3>Today is ... open hours ... click to open more hours (popup) (full week timetable - holdays listed below) </h3>
<h4>Sort books</h4>
<form action="">
<!-- make it change on change not on button and multiple categories at once -->
    <input type="text" placeholder="isbn" bind:value={s_isbn}>
    <input type="text" placeholder="title" bind:value={s_title}>
    <input type="text" placeholder="author" bind:value={s_author}>
    <input type="number" name="" id="" placeholder="year" bind:value={s_year}> 
    <input type="submit" value="SEARCH">
</form>
<h1>if we have your book log in to borrow it!</h1>