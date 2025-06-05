const {readFile} = require('fs')
const fs = require('fs')

async function test2(fileName) {
  // ruleid:detect-non-literal-fs-filename
  const data = await fs.promises.mkdir(fileName, {})
  foobar(data)
}