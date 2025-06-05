const {readFile} = require('fs')
const fs = require('fs')

function okTest1(data) {
  const data = new Uint8Array(Buffer.from('Hello Node.js'));
  // ok:detect-non-literal-fs-filename
  fs.writeFile('message.txt', data, (err) => {
    if (err) throw err;
    console.log('The file has been saved!');
  });
}