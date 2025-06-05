import styled, { keyframes } from "styled-components";

function Vulnerable2(userInput) {
  const input = fooBar(userInput)

// ruleid: react-styled-components-injection
  return styled.div`
    background: url(${input});
  `
}