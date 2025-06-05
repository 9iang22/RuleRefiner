function bad (name) {
  //ruleid: detect-non-literal-regexp
  const reg = new RegExp("\\w+" + name)
  return reg.exec(name)
}