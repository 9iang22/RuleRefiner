function ok (name) {
  //ok: detect-non-literal-regexp
  const reg = new RegExp("\\w+")
  return reg.exec(name)
}