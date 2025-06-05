// ruleid: insufficient-postmessage-origin-validation
window.addEventListener("message", function (evt) {
  console.log('Inline without origin check!');
});