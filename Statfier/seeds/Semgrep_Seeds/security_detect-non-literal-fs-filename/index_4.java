const {readFile} = require('fs')
const fs = require('fs')

async function okTest2() {
  let filehandle;
  try {
    // ok:detect-non-literal-fs-filename
    filehandle = await fs.promises.open('thefile.txt', 'r');
  } finally {
    if (filehandle !== undefined)
      await filehandle.close();
  }
}