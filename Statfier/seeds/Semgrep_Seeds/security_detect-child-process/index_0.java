const {exec, spawnSync} = require('child_process');

// ruleid:detect-child-process
exec(`cat *.js ${userInput}| wc -l`, (error, stdout, stderr) => {
  console.log(stdout)
});