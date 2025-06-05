const express = require('express')
const app = express()
const port = 3000

app.post('/ok2', async (req, res) => {
    // ok: raw-html-format
    res.send(`message: ${req.query.message}`);
})

app.listen(port, () => console.log(`Example app listening at http://localhost:${port}`))