from semgrep2nx import yaml2ast, ast2yaml

rule = """
rules:
- id: missing-pip-no-cache-dir
  languages:
  - generic
  message: Add '--no-cache-dir' to the '$PIP install' command to prevent unnecessary
    package installation and reduce image size.
  patterns:
  - patterns:
    - pattern: RUN ... $PIP install ... $SOMETHING
    - pattern-not-inside: RUN ... $PIP install ... --no-cache-dir
    - pattern-not-inside: RUN ... $PIP install . ... $SOMETHING
    - pattern-not-inside: |
        ENV PIP_NO_CACHE_DIR=...
        ...
        RUN ... $PIP install ... $SOMETHING
  - metavariable-regex:
      metavariable: $PIP
      regex: (pip|pip3)
  severity: INFO
"""

ast = yaml2ast(rule)
yaml = ast2yaml(ast)
print(yaml)