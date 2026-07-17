const genUrlBtn = document.getElementById("gen-url-btn");
const copyBtn = document.getElementById("copy-btn");

genUrlBtn.addEventListener("click", async (event) => {
    event.preventDefault();
    
    const longUrl = document.getElementById("input-url").value;
    console.log(longUrl);

    document.getElementById("short-url").textContent = longUrl;
    document.getElementById("short-url").href = longUrl;
});

copyBtn.addEventListener("click", async (event) => {
    event.preventDefault();
    
    const shortUrl = document.getElementById("short-url").textContent;
    await copyTextToClipboard(shortUrl);
});

async function copyTextToClipboard(text) {
    await navigator.clipboard.writeText(text);
}