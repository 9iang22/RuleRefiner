from jinja2 import Template
import logging

TEMPLATE_OUTPUT = """
## Output Format
The answer should follow the format below:
<THOUGHTS>
YOUR_THOUGHTS_HERE
</THOUGHTS>

<FINAL_ANSWER>
```yaml
YOUR_FINAL_ANSWER_HERE
```
</FINAL_ANSWER>
"""

TEMPLATE_SIMPLE = """
# Semgrep Rule Refinement Task

## Input Parameters
1. **Original Rule** (needs refinement)
```yaml
{{rule}}
```

2. **Problem Case** (misclassified example)
- Case type: {{typ}} (FP: False Positive / FN: False Negative)
- Problematic code example:
```code
{{e}}

## Task Requirements
Please fix the rule to correctly classify the {{typ}} code example.
"""

TEMPLATE_OVER_MATCH_LOCALIZATION = """
# Semgrep Rule Refinement Task

## Input Parameters
1. **Original Rule** (needs refinement)
```yaml
{{rule}}
```

2. **Problem Case** (misclassified example)
- Case type: {{typ}} (FP: False Positive / FN: False Negative)
- Problematic code example:
```code
{{e}}
```
- Faulty matching pattern (current problematic rule fragment):
```yaml
{{ep_xpat}}
```

3. **Reference Case** (similar to current {{typ}} example but correctly handled example)
- Valid code example:
```code
{{s}}
```
- Working exclusion pattern (differentiating fragment):
```yaml
{{sp_xpat}}
```

## Task Requirements
Follow these analysis steps:
1. Root Cause Analysis
   - Explain why the case is misclassified as {{typ}}.
   - Compare key differences between the correct and incorrect cases.
   - Review the faulty matching pattern and the working exclusion pattern.

2. Modification Strategy
   - Describe pattern matching adjustments
   - Maintain original detection capabilities
   - Ensure rule readability and efficiency
   - Use standard Semgrep syntax

3. Fix the Rule
    - Provide a refined Semgrep rule that addresses the misclassification.
"""

TEMPLATE_MISS_MATCH_LOCALIZATION ="""
# Semgrep Rule Refinement Task

## Input Parameters
1. **Original Rule** (needs refinement)
```yaml
{{rule}}
```

2. **Problem Case** (misclassified example)
- Case type: {{typ}} (FP: False Positive / FN: False Negative)
- Problematic code example:
```code
{{e}}
```
- Faulty matching pattern (current problematic rule fragment):
```yaml
{{ep_xpat}}
```

3. **Reference Case** (similar to current {{typ}} example but correctly handled example)
- Valid code example:
```code
{{s}}
```
- Working exclusion pattern (differentiating fragment):
```yaml
{{sp_xpat}}
```

## Task Requirements
Follow these analysis steps:
1. Root Cause Analysis
   - Explain why the case is misclassified as {{typ}}.
   - Compare key differences between the correct and incorrect cases.
   - Review the faulty matching pattern and the working exclusion pattern.

2. Modification Strategy
   - Describe pattern matching adjustments
   - Maintain original detection capabilities
   - Ensure rule readability and efficiency
   - Use standard Semgrep syntax

3. Fix the Rule
    - Provide a refined Semgrep rule that addresses the misclassification.

""" 

TEMPLATE_MISS_MATCH_TEMPLATE = """
## FIX HINT
You can follow the hint below to fix it:
1. (Recommanded) Add a new pattern to match the case and combine it with the existing pattern using `pattern-either`.
2. Changing the content of `{{loc}}` to make it match the case.
"""

TEMPLATE_OVER_MATCH_TEMPLATE = """
## FIX HINT
You can follow the hint below to fix it:
1. Add a new whitelist pattern with `pattern-not` to match the case and combine it with the existing pattern using `patterns`.
    e.g.
    patterns:
        - pattern: request.$W
        - pattern-not: request.get_full_path
2. Add a new context pattern with `pattern-inside` to exclude the context of the case and combine it with the existing pattern using `patterns`.
    e.g.
    rules:
    - id: return-in-init
        patterns:
        - pattern: return ...
        - pattern-inside: |
            def __init__(...):
                ...
        message: return should never appear inside a class __init__ function
        languages:
        - python
        severity: ERROR
3. Add a new filter context pattern with `pattern-not-inside` to matching the context of the case and combine it with the existing pattern using `patterns`.
    e.g.
rules:
  - id: open-never-closed
    patterns:
      - pattern: $F = open(...)
      - pattern-not-inside: |
          $F = open(...)
          ...
          $F.close()
    message: file object opened without corresponding close
    languages:
      - python
    severity: ERROR
4. Add a filter pattern with `metavariable-regex` to exclude the case and combine it with the existing pattern using `patterns`.
    e.g.
    patterns:
        - pattern: request.$W
        - metavariable-regex:
            metavariable: $W
            regex: (?!get_full_path)
5. Add a filter pattern with `metavariable-comparison` to exclude the case and combine it with the existing pattern using `patterns`,
the comparison key accepts Python expression using (literals, boolean, arithmetic, comparison  operators, int(), str()).
    patterns:
      - pattern: set_port($ARG)
      - metavariable-comparison:
          comparison: $ARG < 1024 and $ARG % 2 == 0
          metavariable: $ARG

6. Add a filter pattern with `metavariable-pattern` to exclude the case and combine it with the existing pattern using `patterns`.
    e.g.
    patterns:
      - pattern: |
          $CONST = require('crypto');
          ...
          $OPTIONS = $OPTS;
          ...
          https.createServer($OPTIONS, ...);
      - metavariable-pattern:
          metavariable: $OPTS
          patterns:
            - pattern-not: >
                {secureOptions: $CONST.SSL_OP_NO_SSLv2 | $CONST.SSL_OP_NO_SSLv3
                | $CONST.SSL_OP_NO_TLSv1}

7. (Not Recommanded) Changing the content of original pattern to make it exclude the case.
"""

NAIVE_OUTPUT ="""
## Output Format
The answer should follow the format below:
<FINAL_ANSWER>
```yaml
YOUR_FINAL_ANSWER_HERE
```
</FINAL_ANSWER>
"""

EXAMPLE="""
## A fixed Semgrep rule example
Here is the rule before refinement:
```yaml
rules:
  - id: direct-use-of-httpresponse
    message: >-
      Detected data rendered directly to the end user via 'HttpResponse'
      or a similar object. This bypasses Django's built-in cross-site scripting
      (XSS) defenses and could result in an XSS vulnerability. Use Django's
      template engine to safely render HTML.
    metadata:
      cwe: "CWE-79: Improper Neutralization of Input During Web Page Generation ('Cross-site Scripting')"
      owasp: "A7: Cross-Site Scripting (XSS)"
      references:
        - https://docs.djangoproject.com/en/3.1/intro/tutorial03/#a-shortcut-render
        - https://docs.djangoproject.com/en/3.1/topics/http/shortcuts/#render
      category: security
      technology:
        - django
    languages:
      - python
    severity: WARNING
    patterns:
      - pattern-not: django.http.$ANY("...", ...)
      - pattern-not: django.http.$ANY()
      - pattern-not: django.http.$ANY(..., content=None, ...)
      - pattern-not: django.http.$ANY(status=...)
      - pattern-not: django.http.HttpResponseNotAllowed([...])
      - pattern-either:
          - pattern: django.http.HttpResponse(...)
          - pattern: django.http.HttpResponseBadRequest(...)
          - pattern: django.http.HttpResponseNotFound(...)
          - pattern: django.http.HttpResponseForbidden(...)
          - pattern: django.http.HttpResponseNotAllowed(...)
          - pattern: django.http.HttpResponseGone(...)
          - pattern: django.http.HttpResponseServerError(...)
```
This rule catches a wrong case which should not be caught thus causing a ** false positive **,the case is below:
```python
import urllib
import json
from django.db.models import Q
from django.auth import User
from django.http import HttpResponse, HttpResponseBadRequest
from django.utils.translation import ugettext as _
from org import engines, manageNoEngine, genericApiException

def endpoint():
    # ok:direct-use-of-httpresponse
    return HttpResponse(json.dumps({ 'status': 'ERROR', 'error': str(e) }), content_type='text/json')
```
After ** fixing ** the rule, it comes to this:
```yaml
rules:
  - id: direct-use-of-httpresponse
    message: >-
      Detected data rendered directly to the end user via 'HttpResponse'
      or a similar object. This bypasses Django's built-in cross-site scripting
      (XSS) defenses and could result in an XSS vulnerability. Use Django's
      template engine to safely render HTML.
    metadata:
      cwe: "CWE-79: Improper Neutralization of Input During Web Page Generation ('Cross-site Scripting')"
      owasp: "A7: Cross-Site Scripting (XSS)"
      references:
        - https://docs.djangoproject.com/en/3.1/intro/tutorial03/#a-shortcut-render
        - https://docs.djangoproject.com/en/3.1/topics/http/shortcuts/#render
      category: security
      technology:
        - django
    languages:
      - python
    severity: WARNING
    patterns:
      - pattern-not: django.http.$ANY("...", ...)
      - pattern-not: django.http.$ANY()
      - pattern-not: django.http.$ANY(..., content=None, ...)
      - pattern-not: django.http.$ANY(status=...)
      - pattern-not: django.http.HttpResponseNotAllowed([...])
      - pattern-either:
          - patterns:
            - pattern-either:
              - pattern: django.http.HttpResponse(...)
              - pattern: django.http.HttpResponseBadRequest(...)
              - pattern: django.http.HttpResponseNotFound(...)
              - pattern: django.http.HttpResponseForbidden(...)
              - pattern: django.http.HttpResponseNotAllowed(...)
              - pattern: django.http.HttpResponseGone(...)
              - pattern: django.http.HttpResponseServerError(...)
            - pattern-not: django.http.$ANY(...,content_type=$TYPE,...)
          - patterns:
            - pattern-either:
              - pattern: django.http.HttpResponse(...,content_type=$TYPE,...)
              - pattern: django.http.HttpResponseBadRequest(...,content_type=$TYPE,...)
              - pattern: django.http.HttpResponseNotFound(...,content_type=$TYPE,...)
              - pattern: django.http.HttpResponseForbidden(...,content_type=$TYPE,...)
              - pattern: django.http.HttpResponseNotAllowed(...,content_type=$TYPE,...)
              - pattern: django.http.HttpResponseGone(...,content_type=$TYPE,...)
              - pattern: django.http.HttpResponseServerError(...,content_type=$TYPE,...)
            - metavariable-regex:
                metavariable: $TYPE
                regex: '.*[tT][eE][xX][tT]/[hH][tT][mM][lL].*'
```
"""

EXAMPLE_BE_RULE="""
rules:
- id: useless-assignment
  patterns:
  - pattern-not: |
      $X = $Y;
      $X = $X.$METHOD(...);
  - pattern-not: |
      $X = $Y;
      $X = $PREPEND + $X;
  - pattern-not: |
      $X = $Y;
      $X = $X + $POSTPEND;
  - pattern: |
      $X = $Y;
      $X = $Z;
  message: '`$X` is assigned twice; the first assignment is useless'
  languages: [javascript]
  severity: WARNING
"""

FEW_SHOT_EXAMPLE_RESPONSE ="""
<FINAL_ANSWER>
```yaml
rules:
- id: useless-assignment
  patterns:
  - pattern-not: |
      $X = $Y;
      $X = $X.$METHOD(...);
  - pattern-not: |
      $X = $Y;
      $X = $FUNC(..., <... $X ...>, ...);
  - pattern-not: |
      $X = $Y;
      $X = $PREPEND + $X;
  - pattern-not: |
      $X = $Y;
      $X = $X + $POSTPEND;
  - pattern: |
      $X = $Y;
      $X = $Z;
  message: '`$X` is assigned twice; the first assignment is useless'
  languages: [javascript]
  severity: WARNING
```
</FINAL_ANSWER>
"""

COT_FEW_SHOT_EXAMPLE_RESPONSE ="""
<THOUGHTS>

</THOUGHTS>
<FINAL_ANSWER>
```yaml
rules:
- id: useless-assignment
  patterns:
  - pattern-not: |
      $X = $Y;
      $X = $X.$METHOD(...);
  - pattern-not: |
      $X = $Y;
      $X = $FUNC(..., <... $X ...>, ...);
  - pattern-not: |
      $X = $Y;
      $X = $PREPEND + $X;
  - pattern-not: |
      $X = $Y;
      $X = $X + $POSTPEND;
  - pattern: |
      $X = $Y;
      $X = $Z;
  message: '`$X` is assigned twice; the first assignment is useless'
  languages: [javascript]
  severity: WARNING
```
</FINAL_ANSWER>
"""

E="""
// ok: useless-assignment
c = j;
c = f(1, g(c));
"""



SYSTEM_PROMPT = "You are an expert who are familar with static analysis tools --Semgrep."
PROMPT_FEW_SHOT = TEMPLATE_SIMPLE + "\n" + EXAMPLE + "\n" + NAIVE_OUTPUT
PROMPT_NAIVE=TEMPLATE_SIMPLE+"\n" + NAIVE_OUTPUT
PROMPT_SIMPLE = TEMPLATE_SIMPLE + "\n" + TEMPLATE_OUTPUT
PROMPT_OVER_MATCH_LOCALIZATION = TEMPLATE_OVER_MATCH_LOCALIZATION + '\n' + TEMPLATE_OUTPUT
PROMPT_MISS_MATCH_LOCALIZATION = TEMPLATE_MISS_MATCH_LOCALIZATION + '\n' + TEMPLATE_OUTPUT
PROMPT_OVER_MATCH_FULL = TEMPLATE_OVER_MATCH_LOCALIZATION + "\n" + TEMPLATE_OVER_MATCH_TEMPLATE + '\n' + TEMPLATE_OUTPUT
PROMPT_MISS_MATCH_FULL = TEMPLATE_MISS_MATCH_LOCALIZATION + "\n" + TEMPLATE_MISS_MATCH_TEMPLATE + '\n' + TEMPLATE_OUTPUT


def few_shot_example_prompt():
    return Template(PROMPT_NAIVE).render(rule=EXAMPLE_BE_RULE, e=E, typ="FP")

def cot_few_shot_example_prompt():
    return Template(PROMPT_SIMPLE).render(rule=EXAMPLE_BE_RULE, e=E, typ="FP")

def few_shot_example_response():
    return FEW_SHOT_EXAMPLE_RESPONSE

def cot_few_shot_example_response():
    return COT_FEW_SHOT_EXAMPLE_RESPONSE

def few_shot_msg(prompt, with_cot=False):
    if with_cot:
        return [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": cot_few_shot_example_prompt()},
            {"role": "assistant", "content": cot_few_shot_example_response()},
            {"role": "user", "content": prompt}
        ]
    else:
        return [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": few_shot_example_prompt()},
            {"role": "assistant", "content": few_shot_example_response()},
            {"role": "user", "content": prompt}
        ]

def gen_template_prompt(rule, e, s, ep_xpat, sp_xpat, typ, overmatch=True, mode='full'):
    if mode == "fewshot":
        return Template(PROMPT_NAIVE).render(rule=rule, e=e, typ=typ)
    if mode == "naive":
        return Template(PROMPT_NAIVE).render(rule=rule, e=e, typ=typ)
    if mode == 'cot' or mode == "simple" or mode == 'cot-fewshot':
        return Template(PROMPT_SIMPLE).render(rule=rule, e=e, typ=typ)
    if mode == 'localization':
        if overmatch:
            return Template(PROMPT_OVER_MATCH_LOCALIZATION).render(rule=rule, e=e, s=s, ep_xpat=ep_xpat, sp_xpat=sp_xpat, typ=typ)
        else:
            return Template(PROMPT_MISS_MATCH_LOCALIZATION).render(rule=rule, e=e, s=s, ep_xpat=ep_xpat, sp_xpat=sp_xpat, typ=typ)
    if mode == 'full' or mode == "template":
        if overmatch:
            return Template(PROMPT_OVER_MATCH_FULL).render(rule=rule, e=e, s=s, ep_xpat=ep_xpat, sp_xpat=sp_xpat, typ=typ)
        else:
            return Template(PROMPT_MISS_MATCH_FULL).render(rule=rule, e=e, s=s, ep_xpat=ep_xpat, sp_xpat=sp_xpat, typ=typ)
    logging.error("gen_template_prompt: unknown mode %s", mode)



def postprocess(output):
    if "<FINAL_ANSWER>" in output and "</FINAL_ANSWER>" in output:
        if "<THOUGHTS>" in output and "</THOUGHTS>" in output:
            start = output.index("<THOUGHTS>") + len("<THOUGHTS>")
            end = output.index("</THOUGHTS>")
            explanation = output[start:end].strip()
        else:
            explanation = ""
        start = output.index("<FINAL_ANSWER>") + len("<FINAL_ANSWER>")
        end = output.index("</FINAL_ANSWER>")
        final_answer = output[start:end].strip()
        if "```yaml" in final_answer:
            final_answer = final_answer[final_answer.index("```yaml") + len("```yaml"):]
            if "```" in final_answer:
                final_answer = final_answer[:final_answer.index("```")]
                final_answer = final_answer.strip()
        return explanation, final_answer
    return None, None


def postprocess_without_cot(output):
    if "<FINAL_ANSWER>" in output and "</FINAL_ANSWER>" in output:
        start = output.index("<FINAL_ANSWER>") + len("<FINAL_ANSWER>")
        end = output.index("</FINAL_ANSWER>")
        final_answer = output[start:end].strip()
        if "```yaml" in final_answer:
            final_answer = final_answer[final_answer.index("```yaml") + len("```yaml"):]
            if "```" in final_answer:
                final_answer = final_answer[:final_answer.index("```")]
                final_answer = final_answer.strip()
        return None, final_answer
    return None, None