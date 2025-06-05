const express = require('express')
const app = express()
const port = 3000
const puppeteer = require('puppeteer')

app.post('/test', async (req, res) => {
    const browser = await puppeteer.launch()
    const page = await browser.newPage()
// ruleid: express-puppeteer-injection
    await page.setContent(`${req.body.foo}`)

    await page.screenshot({path: 'example.png'})
    await browser.close()

    res.send('Hello World!')
})