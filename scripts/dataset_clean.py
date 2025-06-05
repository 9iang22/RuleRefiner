import json
import copy
import os
from semgrep2nx import yaml2ast, ast2yaml

cnt = 0

def clean(d):
    rule = d['rule']
    y = yaml2ast(rule)
    r = y['rules'][0]
    rm = []
    for k in r:
        if k in ['metadata','min-version','max-version', 'paths', 'fix', 'fix-regex']:
            rm.append(k)
    for k in rm:
        r.pop(k)
    y['rules'][0] = r
    d['rule'] = ast2yaml(y)
    return d

def split(d):
    actuals = d['actual']
    expects = d['expected']
    tests = d['splited_testsuite_b']
    failed_tests = [test for test in tests if actuals[tests.index(test)] != expects[tests.index(test)]]
    results = []
    for test in failed_tests:
        assert test in tests
        idx = tests.index(test)
        assert actuals[idx] != expects[idx]
        dd = copy.deepcopy(d)
        dd['failed_tests'] = [test]
        dd['actual'] = [actuals[i] for i in range(len(tests)) if actuals[i] == expects[i]] + [actuals[idx]]
        dd['expected'] = [expects[i] for i in range(len(tests)) if actuals[i] == expects[i]] + [expects[idx]]
        dd['splited_testsuite_b'] = [tests[i] for i in range(len(tests)) if actuals[i] == expects[i]] + [tests[idx]]
        global cnt
        cnt += 1
        dd['index'] = cnt
        results.append(dd)
    return results

with open("dataset/semgrep.jsonl") as f:
    data = [json.loads(line) for line in f.readlines()]

for d in data:
    rp = d['rule_path']
    rule = open(os.path.join("..", rp)).read()
    d['rule'] = rule
    clean(d)
results = []

for d in data:
    r = split(d)
    results.extend(r)

with open("dataset/semgrep_split.jsonl", "w+") as f:
    for d in results:
        f.write(json.dumps(d) + "\n")

            
        