const genUrlBtn = document.getElementById("gen-url-btn");
const copyBtn = document.getElementById("copy-btn");

genUrlBtn.addEventListener("click", async (event) => {
    event.preventDefault();
    
    const longUrl = document.getElementById("input-url").value;
    console.log(longUrl);
    if (!longUrl.trim()) {
        alert("Please enter a URL.");
        return;
    }   

    const response = await fetch("http://localhost:8080/shortenUrl", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            longUrl: longUrl
        })
    });

    const result = await response.json();

    document.getElementById("short-url").textContent = result.shortUrl;
    document.getElementById("short-url").href = data.shortUrl;
});

copyBtn.addEventListener("click", async (event) => {
    event.preventDefault();
    
    const shortUrl = document.getElementById("short-url").textContent;
    await navigator.clipboard.writeText(shortUrl);
});