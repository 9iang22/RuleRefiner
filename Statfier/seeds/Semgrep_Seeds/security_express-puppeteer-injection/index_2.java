const express = require('express')
const app = express()
const port = 3000
const puppeteer = require('puppeteer')

const controller = async (req, res) => {
    const browser = await puppeteer.launch();
    const page = await browser.newPage();
// ruleid: express-puppeteer-injection
    const body = req.body.foo;
    await page.setContent('<html>' + body + '</html>');

    await page.screenshot({path: 'example.png'});
    await browser.close();

    res.send('Hello World!');
}