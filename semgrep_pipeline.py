from semgrep_locate import lcp_locate
from semgrep2nx import Semgrep2NX
from testcase import Example
from typing import List
from semgrep import semgrep_explanation_in_tempdir
import json
import logging
import copy
import sys
from semgrep_template import analysis_loc
from semgrep_prompt import postprocess, gen_template_prompt
import tqdm
from para import map_reduce

from semgrep import semgrep_test_in_tempdir, OK, SYNTAXERROR, COMMANDERROR
from semgrep_verify import syntax_check, regression, verify_fix

chat_raw = None
chat2 = None
set_temperature = None

def prepare_data(d)->List[Example]:
    example_set = []
    for test, expect, actual in zip(d['splited_testsuite_b'], d['expected'], d['actual']):
        example_set.append(Example(d['test_path'], d['rule_path'], test, "semgrep", expect, actual))
    return example_set

def prepare_prompts(old_rule, example_set, mode):
    correct_set = [e for e in example_set if e.ok()]
    positive_set = [e for e in correct_set if e.actual]
    negative_set = [e for e in correct_set if not e.actual]

    incorrects = [e for e in example_set if not e.ok()]
    assert len(incorrects) == 1
    results = []

    if mode == "simple" or mode == "naive" or mode == "cot" or mode == "fewshot" or mode == "cot-fewshot":
        for incorrect in incorrects:
            typ = "false positive" if incorrect.is_fp() else "false negative"
            prompt = gen_template_prompt(old_rule, incorrect.content, None, None, None, typ, None, mode)
            results.append({"prompt":prompt, "template":old_rule, "incorrect":incorrect.content, "type": "fp" if incorrect.is_fp() else "fn"})
        return results
    
    logging.debug(f"find {len(incorrects)} incorrect cases")

    localization_failed = 0

    for incorrect in incorrects:
        logging.debug(f"find incorrect case: \n{incorrect.content}")
        if incorrect.is_fn() and len(positive_set) == 0 or incorrect.is_fp() and len(negative_set) == 0:
            # skip, unlikely in our dataset
            continue
        if incorrect.is_fn():
            localization = lcp_locate(old_rule, positive_set, incorrect)
        else:
            localization = lcp_locate(old_rule, negative_set, incorrect)
        if localization == []:
            localization_failed += 1
            continue
        if len(localization) >= 5:
            localization = localization[:5]
        logging.debug(f"localize at {localization}")
        for loc in localization:
            ep_xpat, sp_xpat, similar_case, overmatch = analysis_loc(incorrect, loc)
            ep_xpat = "\n".join([str(x) for x in ep_xpat])
            sp_xpat = "\n".join([str(x) for x in sp_xpat])
            typ = "false positive" if incorrect.is_fp() else "false negative"
            prompt = gen_template_prompt(old_rule, incorrect.content, similar_case, ep_xpat, sp_xpat, typ, overmatch, mode)
            results.append({"prompt":prompt, "prompt_type":"template", "incorrect":incorrect.content, "type": "fp" if incorrect.is_fp() else "fn"})
    return results

def query_all(data, mode='full'):
    from para import map_reduce
    def mapf(d):
        prompt = d['prompt']['prompt']
        if mode == "fewshot" or mode == "cot-fewshot":
            from semgrep_prompt import few_shot_msg
            if mode == "cot-fewshot":
                msg = few_shot_msg(prompt, True)
            else:
                msg = few_shot_msg(prompt, False)
            r = chat2(msg)
        else:
            r = chat_raw(prompt)
        d['response'] = r
        return d
    reducef = lambda x: x
    return map_reduce(data, mapf, reducef, max_workers=100)

def query_all_fewshot(data):
    from para import map_reduce
    def mapf(d):
        prompt = d['prompt']['prompt']
        from semgrep_prompt import few_shot_msg
        msg = few_shot_msg(prompt)
        r = chat2(msg)
        d['response'] = r
        return d
    reducef = lambda x: x
    return map_reduce(data, mapf, reducef, max_workers=100)

def check_one(d):
    example_set = prepare_data(d)
    correct_set = [e for e in example_set if e.ok()]
    positive_set = [e for e in correct_set if e.actual]
    negative_set = [e for e in correct_set if not e.actual]

    incorrect = d['prompt']['incorrect']
    find = None
    for e in example_set:
        if e.content == incorrect:
            find = e
            break
    assert find
    incorrect = find

    old_rule = d['rule']
    response = json.loads(d['response'])
    expl, new_rule = postprocess(response['choices'][0]['message']['content'])
    verify_results = []
    if not syntax_check(new_rule, incorrect):
        verify_results.append("SYNTAX_ERROR")
    else:
        if not regression(new_rule, old_rule, example_set):
            verify_results.append("REGRESSION_FAILED")
        if not verify_fix(new_rule, incorrect):
            verify_results.append("REFINE_FAILED")
        else:
            verify_results.append("REFINE_SUCCESS")
    d['verify_result'] = verify_results
    return d

def check_all(data):
    return map_reduce(data, check_one, lambda x: x, max_workers=len(data))

def gen_all_prompts(data, mode):    
    results = []

    def mapf(d):
        example_set = prepare_data(d)
        results = prepare_prompts(d['rule'], example_set, mode)
        res = []
        if len(results) == 0:
            logging.debug(f"no localization found for {d['index']}")
            return []
        for r in results:
            d = copy.deepcopy(d)
            d['prompt'] = r
            res.append(d)
        return res
    
    def reducef(x):
        res = []
        for xx in x:
            res.extend(xx)
        return res
    
    results = map_reduce(data, mapf, reducef, max_workers=len(data))
    return results

def test():
    logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', stream=sys.stdout)
    data = json.load(open("examples/localization.json"))
    rule = data['rule']
    rname = data['rule_path']
    tname = data['test_path']
    tests = data['splited_testsuite_b']
    example_set = [Example(tname, rname, tests[0], "semgrep", True, True),
                   Example(tname, rname, tests[1], "semgrep", False, False),
                   Example(tname, rname, tests[2], "semgrep", False, False),
                   Example(tname, rname, tests[3], "semgrep", False, True)]
    results = pipeline(rule, example_set)
    print(results)

def main(data, mode, prompt_file='results/semgrep_prompts.jsonl', 
         result_file='results/semgrep_result.jsonl', 
         verify_file='results/semgrep_verify.jsonl'):
    data = gen_all_prompts(data, mode)
    with open(prompt_file, "w+") as o:
        for d in data:
            o.write(json.dumps(d) + "\n")

    data = [json.loads(line) for line in open(prompt_file).readlines()]
    results = query_all(data, mode)
    with open(result_file, "w+") as o:
        for r in results:
            o.write(json.dumps(r) + "\n")

    data = [json.loads(line) for line in open(result_file).readlines()]
    results = check_all(data)
    with open(verify_file, "w+") as o:
        for r in results:
            o.write(json.dumps(r) + "\n")


def fix_one(d):
    response = json.loads(d['response'])
    expl, rule = postprocess(response['choices'][0]['message']['content'])
    d['old_response'] = d['response']
    if rule:
        from semgrep_syntax import do_fix
        nrule, prompt, resp = do_fix(rule, d['rule_path'])
        if nrule:
            response['choices'][0]['message']['content'] = response['choices'][0]['message']['content'].replace(rule, nrule)
            d['response'] = json.dumps(response)
            d['old_verify_result'] = d['verify_result']
            d['verify_result'] = check_one(d)['verify_result']
            d['syntax_fix_prompt'] = prompt
            d['syntax_fix_response'] = resp
            d['syntax_fix_rule'] = nrule
    return d


def syntax_fix_stage(data):
    return map_reduce(data, fix_one, lambda x: x, max_workers=len(data))

def pipeline(data, mode, prompt_file, result_file, verify_file):
    data = gen_all_prompts(data, mode)
    with open(prompt_file, "w+") as o:
        for d in data:
            o.write(json.dumps(d) + "\n")

    data = [json.loads(line) for line in open(prompt_file).readlines()]
    results = query_all(data, mode)
    with open(result_file, "w+") as o:
        for r in results:
            o.write(json.dumps(r) + "\n")

    data = [json.loads(line) for line in open(result_file).readlines()]
    results = check_all(data)
    with open(verify_file, "w+") as o:
        for r in results:
            o.write(json.dumps(r) + "\n")

 
if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Run the Semgrep pipeline.")
    parser.add_argument('--mode', type=str, default='full', choices=['naive', 'cot', 'fewshot','cot-fewshot', 'localization', 'template', 'full'], help='Mode of the pipeline.')
    parser.add_argument('--prompt_file', type=str, default='results/semgrep_prompts.jsonl', help='File to save prompts.')
    parser.add_argument('--result_file', type=str, default='results/semgrep_result.jsonl', help='File to save results.')
    parser.add_argument('--verify_file', type=str, default='results/semgrep_verify.jsonl', help='File to save verification results.')
    parser.add_argument('--temperature', type=float, default=0.0, help='Temperature for the model.')
    parser.add_argument('--model', type=str, default='deepseek-v3', choices=['deepseek-v3', 'qwen-plus','gpt-4o-mini'], help='Model to use for the pipeline.')
    parser.add_argument('--k', type=int, default=1, help='pass@k.')
    args = parser.parse_args()

    if args.model == 'deepseek-v3':
        from deepseek import chat_raw, chat2, set_temperature
        set_temperature(args.temperature)
    elif args.model == 'qwen-plus':
        from qwen import chat_raw, chat2, set_temperature
        set_temperature(args.temperature)
    elif args.model == 'gpt-4o-mini':
        from gpt import chat_raw, chat2, set_temperature
        set_temperature(args.temperature)
    else:
        raise ValueError(f"Unsupported model: {args.model}")

    logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', stream=sys.stdout)

    data = [json.loads(line) for line in open("dataset/semgrep.jsonl").readlines()]
    for i in range(1, args.k+1):
        pipeline(
            data = data,
            mode=args.mode,
            prompt_file=f"{args.prompt_file}.{i}",
            result_file=f"{args.result_file}.{i}",
            verify_file=f"{args.verify_file}.{i}",
        )
    results = []
    for i in range(1, args.k+1):
        results += [json.loads(line) for line in open(f"{args.verify_file}.{i}").readlines()]
    from scripts.semgrep_view_results import passk
    success, failed = passk(results)
    print(f"Pass@{args.k}: {len(success)} / {len(data)}")

else:
    from deepseek import chat_raw, chat2, set_temperature
            

        

