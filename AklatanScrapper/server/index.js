const express = require("express");
const app = express();
const fs = require("fs");
const path = require("path");
const port = 3000;
const isbnFilePath = path.join(__dirname, "..", "data", "isbn.txt");

// 49 496 total
// server shows at most 417 pages A -> Z and Z -> A, 24/page
// workaround with using categories pages
// combining both approaches gave 39 889 total

app.use(express.json());

app.get("/", (req, res) => {
    res.status(405).send("Use POST!");
});

fs.open(isbnFilePath, "a", (err, fd) => {
    if (err) {
        console.error("Error opening file:", err);
        return;
    }
    fs.close(fd, (err) => {
        if (err) {
            console.error("Error closing file:", err);
        }
    });
});
var stream = fs.createWriteStream(isbnFilePath, { flags: "a" });

app.post("/", (req, res) => {
    console.log("Received POST request with body:", req.body);
    const isbns = req.body.isbns;
    isbns.forEach(isbn => {
        stream.write(isbn + "\n");
    });
    res.status(200).send("ISBNs received and saved.");
});

app.listen(port, () => {
  console.log(`ISBN receiver listening at http://localhost:${port}`);
});