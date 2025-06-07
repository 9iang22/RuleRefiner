# Case Study: check-regex-dos Refinement

## Root Cause Introducation

### **Misclassification Explanation**

The Semgrep rule `check-regex-dos` was failing to detect a potential Regular Expression Denial of Service (ReDoS) vulnerability, resulting in a **False Negative**. The rule is designed as a taint-tracking rule, identifying when user-controllable input (`source`) flows into a regular expression constructor (`sink`).

The original rule correctly identified taint sources like Rails `params`, function arguments, and direct model attribute access (e.g., `model.name`). However, it did not recognize data retrieved via the ActiveRecord method `read_attribute(...)` as a potential taint source. In the problematic code, the variable `foo` is assigned the result of `Record.read_attribute("some_attribute")`. Since this pattern was not in the rule's list of sources, the taint engine did not flag `foo` as user-controllable input, and no vulnerability was reported when it was later used in `Regexp.new(foo)`.

### **Case Comparison**

-   **Defect-revealing Case (False Negative):**
    ```ruby
    # The source is a call to `read_attribute`, which was not tracked.
    foo = Record.read_attribute("some_attribute")
    # The sink receives the untracked variable `foo`.
    Regexp.new(foo).match("some_string")
    ```

-   **Most Similiar Regression Case (True Positive):**
    ```ruby
    # The source is `params`, which was correctly tracked.
    foo = params[:some_regex]
    # The sink receives the tracked variable `foo`, triggering the rule.
    Regexp.new(foo).match("some_string")
    ```
The key difference is the origin of the data. The original rule was not comprehensive enough to cover all common methods of accessing potentially user-controlled data from a model.

## Profiling and Differential Analysis
-   **Differential Pattern Fragment (Original `pattern-sources`):**
    ```yaml
    ...
        - patterns:
          - pattern: $X
          // this pattern is not satisified by 
          // defect-revealing case but is satisfied by regression case.
          - pattern-inside: |  
              params[...]
    ...
    ```

## 2. Generated Template
```
+       - pattern-either:
            - pattern-inside: |
                params[...]
+           - <ADD_TAG_HERE> :|
                <ADD_FIX_PATTERN_HERE>
```

## 3. Fix the Rule

The refined rule includes an additional pattern in `pattern-sources` to correctly identify data originating from the `read_attribute` method as tainted.

```yaml
rules:
- id: check-regex-dos
  mode: taint
  pattern-sources:
  - patterns:
    - pattern-either:
      - patterns:
        - pattern: $X
        - pattern-inside: |
            def $F(...,$X,...)
              ...
            end
      - patterns:
        - pattern: $X
+       - pattern-either:
            - pattern-inside: |
                params[...]
+           - pattern-inside: |
+               $Record.read_attribute(...)
      - patterns:
        - pattern: $Y
        - pattern-inside: |
            $RECORD.$Y
  pattern-sinks:
  - patterns:
    - pattern-either:
      - patterns:
        - pattern: $Y
        - pattern-inside: |
            /...#{...}.../
      - patterns:
        - pattern: $Y
        - pattern-inside: |
            Regexp.new(...)
// ...
```