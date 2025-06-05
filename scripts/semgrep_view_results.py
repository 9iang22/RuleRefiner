import sys
import json
import tqdm
import shutil
import os
from semgrep_prompt import postprocess

def passk(data):
    m = {}
    for d in data:
        if d['index'] not in m:
            m[d['index']] = []
        m[d['index']].append(d)
    fails = []
    succ = []
    for k, v in m.items():
        success = False
        for e in v:
            if e['verify_result'] == ["REFINE_SUCCESS"]:
                succ.append(e)
                success = True
                break
        if success:
            continue
        fails.append(v[0])
    return succ, fails

def diff(s1, s2):
    import difflib
    d = difflib.Differ()
    diff = d.compare(s1.splitlines(), s2.splitlines())
    return '\n'.join(diff)

def summary(d):
    try:
        results = ""
        results += "="*0x30 + "\n"
        results += d['id'] + "\n"
        results += "-"*0x30 + "\n"
        results += str(d['verify_result']) + "\n"
        results += "-"*0x30 + "\n"

        original = d['rule']
        expl, nrule = postprocess(json.loads(d['response'])['choices'][0]['message']['content'])
        results += diff(original, nrule) + "\n"
        results += "="*0x30 + "\n"

        results += d['prompt']['prompt'] + "\n"
        results += "-"*0x30 + "\n"
        results += json.loads(d['response'])['choices'][0]['message']['content'] + "\n"
        results += "-"*0x30 + "\n"
        results += d['testsuite_a'] + "\n"
        results += "="*0x30 + "\n"
        return True, results
    except:
        return False, str(d)

def dedup(data):
    m = {}
    for d in data:
        if d['index'] not in m:
            m[d['index']] = []
        m[d['index']].append(d)
    
    cnt = 0
    for k, v in m.items():
        for e in v:
            if e['verify_result'] == ["REFINE_SUCCESS"]:
                cnt += 1
                break

    for k, v in m.items():
        success = [e for e in v if e['verify_result'] == ["REFINE_SUCCESS"]]
        failed = [e for e in v if e['verify_result'] != ["REFINE_SUCCESS"]]
        # print(v[0]['id'], len(success), len(failed))

def repro(inf, outd):
    import sys
    with open(inf) as f:
        lines = f.readlines()
        data = [json.loads(line) for line in lines]
    
    if os.path.exists(outd):
        shutil.rmtree(outd)
    os.makedirs(outd)

    for i, d in tqdm.tqdm(enumerate(data)):
        cdir = os.path.join(outd, f"{i}")
        os.makedirs(cdir)
        ok, sum = summary(d)
        if not ok:
            print(sum)
            continue
        with open(os.path.join(cdir, "summary.txt"), "w") as f:
            f.write(sum)

def fault_analysis(data):
        cnt = len([d for d in data if d['verify_result'] == ["SYNTAX_ERROR"]])
        print(f"syntax error, {cnt} / {len(data)}")
        cnt = len([d for d in data if "REGRESSION_FAILED" in d['verify_result']])
        print(f"regression failed, {cnt} / {len(data)}")
        cnt = len([d for d in data if d['verify_result'] == ["REFINE_FAILED"]])
        print(f"refine failed but regression success, {cnt} / {len(data)}")
        cnt = len([d for d in data if d['verify_result'] == ["REFINE_SUCCESS"]])
        print(f"refine success, {cnt} / {len(data)}")