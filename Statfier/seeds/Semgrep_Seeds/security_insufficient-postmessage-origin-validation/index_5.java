// ok: insufficient-postmessage-origin-validation
window.addEventListener("message", function (evt) {
  if (evt.origin == "http://example.com") {
    console.log('Normal inline function declaration with origin validation');
  }
});