import styled, { keyframes } from "styled-components";

function OkTest(input) {
  const css = 'red';
// ok: react-styled-components-injection
  const ArbitraryComponent = styled.div`
    background: ${css};
  `
  return ArbitraryComponent
}