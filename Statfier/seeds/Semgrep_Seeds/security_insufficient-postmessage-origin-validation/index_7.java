const globalRegex = RegExp('/^http://www\.example\.com$/', 'g');

// ok: insufficient-postmessage-origin-validation
window.addEventListener("message", (evt) => {
  if (globalRegex.test(evt.origin)) {
    console.log(message.data);
  }
});