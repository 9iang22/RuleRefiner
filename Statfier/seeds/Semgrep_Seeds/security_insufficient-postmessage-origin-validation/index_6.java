// ok: insufficient-postmessage-origin-validation
window.addEventListener('message', (evt) => {
  if (evt.origin !== "http://example.com") {
    console.log('Inline arrow function declaration with origin validation');
  }
});