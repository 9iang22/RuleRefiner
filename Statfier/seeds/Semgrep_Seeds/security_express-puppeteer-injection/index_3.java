const express = require('express')
const app = express()
const port = 3000
const puppeteer = require('puppeteer')

app.post('/ok-test', async (req, res) => {
    const browser = await puppeteer.launch();
    const page = await browser.newPage();
// ok: express-puppeteer-injection
    await page.goto('https://example.com');

    await page.screenshot({path: 'example.png'});
    await browser.close();

    res.send('Hello World!');
})