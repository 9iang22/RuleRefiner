from semgrep_pipeline import *
import tempfile

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', stream=sys.stdout)
    # pipeline()
    data = [json.loads(line) for line in open("dataset/semgrep.jsonl").readlines()]
    d = data[0]
    example_set = prepare_data(d)
    prompts = prepare_prompts(d['rule'], example_set, "simple")
    assert prompts[0] is not None
    prompts = prepare_prompts(d['rule'], example_set, "naive")
    assert prompts[0] is not None
    prompts = prepare_prompts(d['rule'], example_set, "cot")
    assert prompts[0] is not None
    prompts = prepare_prompts(d['rule'], example_set, "fewshot")
    assert prompts[0] is not None
    prompts = prepare_prompts(d['rule'], example_set, "localization")
    assert prompts[0] is not None
    prompts = prepare_prompts(d['rule'], example_set, "template")
    assert prompts[0] is not None
    prompts = prepare_prompts(d['rule'], example_set, "full")
    assert prompts[0] is not None

    from deepseek import set_temperature
    set_temperature(0.1)
    from deepseek import TEMPERATURE
    assert abs(TEMPERATURE - 0.1) < 1e-5, f"Expected temperature to be 0.1, but got {TEMPERATURE}"
    from qwen import set_temperature
    set_temperature(0.1)
    from qwen import TEMPERATURE
    assert abs(TEMPERATURE - 0.1) < 1e-5, f"Expected temperature to be 0.1, but got {TEMPERATURE}"

    data = [d]

    tmpdir = tempfile.TemporaryDirectory()
    pipeline(data, "fewshot", f"{tmpdir.name}/prompt.jsonl", f"{tmpdir.name}/result.jsonl", f"{tmpdir.name}/verify.jsonl")
    results = []
    with open(f"{tmpdir.name}/verify.jsonl", "r") as f:
        for line in f:
            results.append(json.loads(line))
    from scripts.semgrep_view_results import passk
    success, failed = passk(results)
    assert len(success) + len(failed) == 1