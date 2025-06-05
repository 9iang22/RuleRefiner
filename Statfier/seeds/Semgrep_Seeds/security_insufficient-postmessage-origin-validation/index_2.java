// ruleid: insufficient-postmessage-origin-validation
window.addEventListener('message', (evt) => {
  console.log('Inline arrow function without origin check!');
});