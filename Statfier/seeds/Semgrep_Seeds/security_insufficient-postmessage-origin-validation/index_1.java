function oldHandler(evt) {
  console.log('Normal function handler without origin check!');
};

// ruleid: insufficient-postmessage-origin-validation
window.addEventListener("message", oldHandler, false);