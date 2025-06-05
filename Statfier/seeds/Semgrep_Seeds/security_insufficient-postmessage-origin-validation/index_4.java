const handler = (evt) => {
  console.log('Arrow function handler without origin check!');
};

// ruleid: insufficient-postmessage-origin-validation
window.addEventListener("message", handler, false);