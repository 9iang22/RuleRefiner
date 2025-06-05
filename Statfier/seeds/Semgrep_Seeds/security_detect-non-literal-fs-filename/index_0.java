const {readFile} = require('fs')
const fs = require('fs')

function test1(fileName) {
  // ruleid:detect-non-literal-fs-filename
  readFile(fileName)
    .then((resolve, reject) => {
      foobar()
    })
}