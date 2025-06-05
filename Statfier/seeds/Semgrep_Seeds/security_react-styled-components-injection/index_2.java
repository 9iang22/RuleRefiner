import styled, { keyframes } from "styled-components";

function Vulnerable3(nevermind, {userInput}) {
  const input = '#' + userInput;

// ruleid: react-styled-components-injection
  return styled.div`
    background: ${input};
  `
}