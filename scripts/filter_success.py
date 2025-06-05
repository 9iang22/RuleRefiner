import json

succ = []

files = [
    "dataset/semgrep_verify.jsonl.1.1",
    "dataset/semgrep_verify.jsonl.1.2",
    "dataset/semgrep_verify.jsonl.1.3",
    "dataset/semgrep_verify.jsonl.1.4",
    "dataset/semgrep_verify.jsonl.1.5",
    "dataset/semgrep_verify.jsonl.2.1",
    "dataset/semgrep_verify.jsonl.2.2",
    "dataset/semgrep_verify.jsonl.2.3",
    "dataset/semgrep_verify.jsonl.2.4",
    "dataset/semgrep_verify.jsonl.2.5",
]


for f in files:
    data = [json.loads(line) for line in open(f).readlines()]
    succ += [d for d in data if d["verify_result"] == ["REFINE_SUCCESS"]]

m = {}
for d in succ:
    if d['index'] not in m:
        m[d['index']] = d
    else:
        continue

with open("dataset/semgrep_success.jsonl", "w") as f:
    for k, v in m.items():
        f.write(json.dumps(v) + "\n")