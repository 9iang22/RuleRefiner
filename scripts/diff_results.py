import json
import sys
import os
import shutil
from semgrep_prompt import postprocess

def diff(s1, s2):
    import difflib
    d = difflib.Differ()
    diff = d.compare(s1.splitlines(), s2.splitlines())
    return '\n'.join(diff)

def summary(pair):
    d = pair[0]
    results = "#"*0x30 + "\n"
    results += d['id'] + "\n"
    results += "-"*0x30 + "\n"
    results += str(d['verify_result']) + "\n"
    results += "-"*0x30 + "\n"
    results += d['prompt']['prompt'] + "\n"
    results += "-"*0x30 + "\n"
    results += json.loads(d['response'])['choices'][0]['message']['content'] + "\n"
    results += "-"*0x30 + "\n"

    original = d['rule']
    expl, nrule = postprocess(json.loads(d['response'])['choices'][0]['message']['content'])
    results += diff(original, nrule) + "\n"
    results += "="*0x30 + "\n"

    d = pair[1]
    results += d['id'] + "\n"
    results += "-"*0x30 + "\n"
    results += str(d['verify_result']) + "\n"
    results += "-"*0x30 + "\n"
    results += d['prompt']['prompt'] + "\n"
    results += "-"*0x30 + "\n"
    results += json.loads(d['response'])['choices'][0]['message']['content'] + "\n"
    return results

f1 = sys.argv[1]
f2 = sys.argv[2]

with open(f1, 'r') as file:
    data1 = [json.loads(line) for line in file.readlines()]
with open(f2, 'r') as file:
    data2 = [json.loads(line) for line in file.readlines()]

pair = []
m = {}

if os.path.exists('diff'):
    shutil.rmtree('diff')
os.mkdir('diff')

for d in data2:
    if d['verify_result'] == ['REFINE_SUCCESS']:
        idx = d['index']
        if idx not in m:
            m[idx] = 1
        else:
            continue
        for dd in data1:
            if dd['index'] == idx and dd['verify_result'] != ['REFINE_SUCCESS']:
                with open(os.path.join('diff', f"{idx}.txt"), 'w') as f:
                    f.write(summary((dd, d)))
                