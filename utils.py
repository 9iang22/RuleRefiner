import json

class Counter():
    def __init__(self):
        self.i = 0

    def __next__(self):
        self.i += 1
        return self.i

    def __iter__(self):
        return self

def read_jsonl(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        return [json.loads(line.strip(line)) for line in f.readlines()]

def write_jsonl(data, file_path):
    with open(file_path, 'w', encoding='utf-8') as f:
        for item in data:
            f.write(json.dumps(item) + '\n')