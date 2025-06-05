const express = require('express')
const app = express()
const port = 3000

app.post('/test3', async (req, res) => {
    // ruleid: raw-html-format
    var html = "<h1>" + "message: " + req.query.message + "</h1>"
    res.send(html);
})

app.listen(port, () => console.log(`Example app listening at http://localhost:${port}`))