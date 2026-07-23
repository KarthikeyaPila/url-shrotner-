const genUrlBtn = document.getElementById("gen-url-btn");
const copyBtn = document.getElementById("copy-btn");

genUrlBtn.addEventListener("click", async (event) => {
    event.preventDefault();
    
    const longUrl = document.getElementById("input-url").value;
    console.log(longUrl);

    const response = await fetch("http://localhost:8080/shortenUrl", {
        method: "POST",
        headers: {
            "Content-Type": "text/plain",
        },
        body: longUrl,
    });

    const data = await response.text();

    document.getElementById("short-url").textContent = data;
    document.getElementById("short-url").href = data;
});

copyBtn.addEventListener("click", async (event) => {
    event.preventDefault();
    
    const shortUrl = document.getElementById("short-url").textContent;
    await navigator.clipboard.writeText(shortUrl);
});