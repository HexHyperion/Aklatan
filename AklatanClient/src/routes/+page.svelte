<script lang="ts">
    let register_email = '';
    let register_name = '';
    let register_password = '';
    let register_password_2 = '';

    let login_email = '';
    let login_password = '';

    async function sendRegister(): Promise<void>{
        if(register_email == ""){
            window.alert("Please enter a valid email")
            return
        }
        if(register_password == ""){
            window.alert("Please enter a valid password")
            return
        }
        if(register_name == ""){
            window.alert("Please enter a valid name")
        }
        if(register_password != register_password_2){
            window.alert("Passwords do not match")
            return
        }
        console.log("yeah")
        const res = await fetch(`http://localhost:8080/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                email : register_email, 
                password : register_password,
                name : register_name
            })
        });

        if (res.status === 201) {
            window.alert("User registered, check your email")
        } else {
            window.alert("User already exists")
        }
    }

    async function sendLogin(){
        // ADD CHECKING 2 PASS INPUTS
        if(login_email == ""){
            window.alert("Please enter a valid email")
            return
        }
        if(login_password == ""){
            window.alert("Please enter a valid password")
            return
        }
        const res = await fetch(`http://localhost:8080/auth/login`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                email :login_email, 
                password : login_password})
        });
        if(res.status === 200){
            const data = await res.json();
            const jwtToken = data.token; 
            console.log("JWT token:", jwtToken);
        }else{
            if(res.status == 401){
                const errorData = await res.json();
                const errorCode = errorData.error.code;
                if(errorCode == 'INCORRECT_USER_CREDENTIALS'){
                    window.alert("Incorrect user credentials")
                }
                if(errorCode === 'USER_NOT_VERIFIED'){
                    window.alert("Email is not verified")
                }
                
            }
            console.log("nie jest g")
            console.log(res)
        }
    }

    async function requestAgain(){
        let email = register_email
        const res = await fetch(`http://localhost:8080/auth/request-email-verification`, {
        method: 'POST',
        
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email })
        });
        if(res.status===200){
            console.log("spoko")
        }
        else{
            console.log(res.status)
        }
    }

    
</script>


<h1>Welcome to Aklatan!</h1>
 
<h3>Logowanie</h3>
<input type="email" placeholder="Login" bind:value={login_email}>
<input type="password" placeholder="Password" bind:value={login_password}>
<input type="submit" value="Log in" onclick={sendLogin}>

<h3>Rejestracja</h3>
<form>
    <input type="email" placeholder="Email" name="email" bind:value={register_email}>
    <input type="text" placeholder="Name" name="name" bind:value={register_name}>
    <input type="password" placeholder="Password" name="password" bind:value={register_password}>
    <input type="password" name="" id="" placeholder="pass2" bind:value={register_password_2}>
    <input type="submit" value="Sign in" onclick={sendRegister}>
</form>
<button onclick={requestAgain}>Request the link again</button>





