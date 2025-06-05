const express = require('express')
const app = express()
const port = 3000

app.post('/test2', async (req, res) => {
    // ruleid: raw-html-format
    res.send(`<h1>message: ${req.query.message}</h1>`);
})

app.listen(port, () => console.log(`Example app listening at http://localhost:${port}`))