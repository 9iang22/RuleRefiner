const jwt = require('jsonwebtoken');

function notOk(token) {
  // ruleid: jwt-decode-without-verify
  if (jwt.decode(token, true).param === true) {
    console.log('token is valid');
  }
}