const express = require('express')
const app = express()
const port = 3000

app.post('/test4', async (req, res) => {
    var html = "<h1> message"
    // ruleid: raw-html-format
    html = html.concat(req.query.message)
    html = html.concat("</h1>")
    res.send(html);
})

app.listen(port, () => console.log(`Example app listening at http://localhost:${port}`))