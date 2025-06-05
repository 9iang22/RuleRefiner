const {readFile} = require('fs')
const fs = require('fs')

function test3(fileName) {
  const data = new Uint8Array(Buffer.from('Hello Node.js'));
  // ruleid:detect-non-literal-fs-filename
  fs.writeFile(fileName, data, (err) => {
    if (err) throw err;
    console.log('The file has been saved!');
  });
}