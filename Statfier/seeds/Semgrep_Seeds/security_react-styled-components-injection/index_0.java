import styled, { keyframes } from "styled-components";

function Vulnerable1(userInput) {
// ruleid: react-styled-components-injection
  const ArbitraryComponent = styled.div`
    background: url(${userInput});
  `
  return ArbitraryComponent
}