const express = require('express')
const app = express()
const port = 3000

app.post('/ok4', async (req, res) => {
    var data = "message: "
    // ok: raw-html-format
    data = data.concat(req.query.message)
    res.send(data);
})

app.listen(port, () => console.log(`Example app listening at http://localhost:${port}`))