import logging.config
from semgrep import semgrep_validate_in_tempdir, OK, SYNTAXERROR, COMMANDERROR
from semgrep_prompt import postprocess
import os
import json
from jinja2 import Template
from doubao import chat2
import logging
import sys

FIX_SYNTAX_PROMPT = Template(
"""Here is a semgrep rule with syntax error:
```yaml
{{rule}}
```

The syntax validation error is as follows:
```text
{{error}}
```

Please fix the syntax error in the rule. 
And note that,  you should never change the content out of the syntax error.

The answer should follow the format below:
<THOUGHTS>
YOUR_THOUGHTS_HERE
</THOUGHTS>

<FINAL_ANSWER>
```yaml
YOUR_SYNTAX_ERROR_FREE_RULE_HERE
```
</FINAL_ANSWER>
""")


cheat_sheet = """
# Pattern CheatSheet

1. **Deep (Recursive) Matching**  
   - Use `<... 42 ...>` to match `42` anywhere in expressions/statements, even nested (e.g., `baz + 42`).

2. **Expression/Statement Sequences**  
   - `foo(); bar();` matches sequences ignoring intermediate code (e.g., multiple `foo()` calls interspersed with other statements).

3. **Exact Matches**  
   - `foo(1, 2)` matches exact arguments, accounting for code formatting/comments but not argument order changes.

4. **Helpful Features**  
   - **Constant Propagation**: Track constants (e.g., `final String pwd = "password"` → `foo(pwd)` matches `foo("password")`).  
   - **Named Placeholders (`$X`)**:  
     - Annotations: `@$X` matches any annotation name.  
     - Arguments: `foo($X, 2)` matches any first argument with `2` as the second.  
     - Classes: `class $X { ... }` matches any class name.  

5. **Conditionals & Functions**  
   - **Conditionals**: `if ($E) foo();` matches any condition followed by `foo()`.  
   - **Function Calls**: `$F(1,2)` matches any function name with arguments `1,2`.  
   - **Function Definitions**: `void $X(...) { ... }` captures method names and structures.

6. **Imports & Statements**  
   - `import java.util.$X;` matches imports from `java.util`.  
   - `if ($X > $Y) $S;` matches conditional statements comparing variables.

7. **Typed Metavariables**  
   - `$X == (String $Y)` ensures type checks (e.g., `String x == String y`).

8. **Regular Expressions**  
   - `$X = "=~/[lL]ocation.*/"` matches strings like `"/location/1"`.

9. **Reoccurring Expressions**  
   - `$X == $X` detects self-comparisons (e.g., `a+b == a+b`).

10. **Wildcards (`...`)**  
    - **Arguments**: `foo(..., 5)` matches calls ending with `5`.  
    - **Method Chaining**: `$X = $O.foo(). ... .bar()` matches chains starting with `foo()` and ending with `bar()`.  
    - **Nested Statements**: `if (...) ...` matches any `if` structure.

11. **Variables & Sequences**  
    - `$V = get(); ... eval($V);` matches variable assignment followed by usage after intermediate code.
    ```
"""

def syntax_error_info(rule, rule_path):
    output, error = semgrep_validate_in_tempdir(rule, rule_path)
    try:
        output = json.loads(output)
    except json.decoder.JSONDecodeError as e:
        return None, f"broken json : {error}"
    return output, error

def gen_fix_syntax_prompt(rule, error):
    return FIX_SYNTAX_PROMPT.render(rule=rule, error=error)

def do_fix(rule, rpath='a.yaml'):
    output, error = syntax_error_info(rule, rpath)
    if output is None:
        return None
    logging.debug("do_fix output: %s", output)

    error = ""
    for x in output['errors']:
        if "message" in x:
            error += x['message'] + '\n'
            continue
        if "long_msg" in x:
            error += x['long_msg'] + '\n'
            continue
    prompt = gen_fix_syntax_prompt(rule, error)
    msg = [
        {"role" : "system", "content" : f"You are an expert of static analysis tools -- Semgrep. We want you to help to fix syntax errors in Semgrep rules. {cheat_sheet}"},
        {"role" : "user", "content" : prompt},
    ]

    resp = chat2(msg)
    expl, nrule = postprocess(resp)
    return nrule, prompt, resp

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, stream=sys.stdout, format='%(asctime)s - %(levelname)s - %(message)s')
    with open("examples/syntax_error.json") as f:
        d = json.loads(f.read())
        resp = json.loads(d['response'])
        expl, rule = postprocess(resp['choices'][0]['message']['content'])
    nrule, prompt, resp = do_fix(rule)
    if nrule:
        print(nrule)

    