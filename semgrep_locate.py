import semgrep2nx
from testcase import Example
from typing import List
from semgrep import semgrep_explanation_in_tempdir
import json
from utils import Counter
import graph
import math
import logging

def spfl(rule, set: List[Example], e:Example):
    assert not e.ok()
    # 1. get G for each example first
    for s in set + [e]:
        output, error = semgrep_explanation_in_tempdir(rule, s.content, s.rname, s.tname)
        outjson = json.loads(output)
        st, ed, s.graph = semgrep2nx.Semgrep2NX(outjson['explanations'][0], Counter())
    # 3. build spectrum for each example
    ef = {n:0 for n in e.graph.nodes}
    nf = {n:0 for n in e.graph.nodes}
    ep = {n:0 for n in e.graph.nodes}
    np = {n:0 for n in e.graph.nodes}
    for s in set:
        for n in s.graph.nodes:
            if semgrep2nx.is_true(s.graph, n):
                ep[n] += 1
            else:
                np[n] += 1
                
    for n in e.graph.nodes:
        if semgrep2nx.is_true(e.graph, n):
            ef[n] += 1
        else:
            nf[n] += 1
    # 4. find the most suspicious node
    def ochiai(ef, nf, ep, np): return ef / math.sqrt((ef + nf)*(ef + ep))
    def dstar(ef, nf, ep, np): return ef*ef / (nf+ep)
    def tarantula(ef, nf, ep, np): return ef / (ef + nf) / (ef / (ef + nf) + ep / (ep + np))
    def naish1(ef, nf, ep, np): return -1 if nf > 0 else np
    def jaccard(ef, nf, ep, np): return ef / (ef + nf + ep)

    suspicious = [(n, ochiai(ef[n], nf[n], ep[n], np[n])) for n in e.graph.nodes]
    suspicious = sorted(suspicious, key=lambda x: x[1], reverse=True)
    return suspicious


def empty_explanations(rule, example_set : List[Example], e : Example):
    for s in example_set + [e]:
        output, error = semgrep_explanation_in_tempdir(rule, s.content, s.rname, s.tname)
        outjson = json.loads(output)
        if "explanations" not in outjson:
            continue
        import copy
        empty_expl = copy.deepcopy(outjson['explanations'][0])
        def __empty(expl):
            expl['matches'] = []
            expl['children'] = [__empty(c) for c in expl['children']]
            return expl
        outjson['explanations'] = [__empty(empty_expl)]
        return outjson
    return None


def lcp_locate(rule, example_set : List[Example], e : Example):
    logging.debug(f"lcp_locate: {e.rname} {len(example_set)}")
    # 1. get profiling information and prediate graph for each example first
    for s in example_set + [e]:
        output, error = semgrep_explanation_in_tempdir(rule, s.content, s.rname, s.tname)
        outjson = json.loads(output)
        if "explanations" not in outjson:
            # happens when none of the patterns match the example
            outjson = empty_explanations(rule, example_set, e)
            if not outjson:
                # unlikely
                logging.error("lcp_locate: empty_explanations failed")
                raise Exception("empty_explanations failed, need check")
        ast = semgrep2nx.yaml2ast(rule)
        ast = semgrep2nx.trans(ast)[0]
        m, am, em, ast, expl = semgrep2nx.align(outjson['explanations'][0], ast)
        s.m = m
        s.am = am
        s.em = em
        s.ast = ast
        s.expl = expl
        s.st, s.ed, s.graph = semgrep2nx.Semgrep2NX(outjson['explanations'][0], Counter())
        # 2. get all pathes from each G
        pathes = graph.find_all_paths(s.graph,s.st, s.ed, semgrep2nx.is_true)
        if s.actual == True:
            pathes = [p for p in pathes if graph.positive_path(p)]
        else:
            pathes = [p for p in pathes if graph.negative_path(p)]
        s.path = pathes
    # # 3. differential analysis for each pair of (e, e')
    if logging.getLogger().isEnabledFor(logging.DEBUG):
        s = example_set[0]
        from pyvis.network import Network
        net = Network(notebook=False, directed=True)
        s.graph = semgrep2nx.color(s.graph, s.st, s.ed)
        net.from_nx(s.graph)
        net.save_graph("debug.html")
        from pprint import pprint
        pprint(s.expl)

    diff_results = []
    for s in example_set:
        for good in s.path:
            for bad in e.path:
                diff, intersections, intervals = graph.diff(bad, good)
                if len(diff) == 0:
                    logging.debug(f"[unlikely] diff result is empty : good, bad = {good}, {bad}")
                    continue
                diff_results.append((diff, intervals, intersections, s.content))
    # 4. find the most suspicious node
    from graph import priority
    diff_results = sorted(diff_results, key=lambda x: priority(x[0], x[1], x[2]), reverse=True)

    # 5. remove duplicates
    new_results = []
    for i, r in enumerate(diff_results):
        if i == 0:
            new_results.append(r)
            continue
        if r == diff_results[i-1]:
            continue
        new_results.append(r)
    diff_results = new_results
    return diff_results

def test():
    import json
    data = json.load(open("examples/localization.json"))
    rule = data['rule']
    rname = data['rule_path']
    tname = data['test_path']
    pset = [data['splited_testsuite_b'][0]]
    nset = data['splited_testsuite_b'][1:3]
    e = data['splited_testsuite_b'][3]
    nset = [Example(tname, rname, p, "semgrep", False, False) for p in nset]
    e = Example(tname, rname, e, "semgrep", False, True)
    #print(spfl(rule, nset, e))
    diff_results = lcp_locate(rule, nset, e)
    for enode, anode in e.m.items():
        print(f"match : {e.em[enode]['op']} -> {e.am[anode]['op']}")
    
    for (diff, intervals, intersections, content) in diff_results:
        for bad, good in diff:
            # interval
            if isinstance(bad[0], tuple):
                bad = bad[0]
            fact = graph.get_fact(e.graph, bad[0])
            print("locate to expl id: ", fact['id'])
            print("locate to expl op: ", fact['op'])
            print("locate to ast: ", e.am[e.m[fact['id']]])

def bug():
    import json
    data = json.load(open("examples/bug.json"))
    rule = data['rule']
    rname = data['rule_path']
    tname = data['test_path']
    pset = data['splited_testsuite_b'][1:3]
    nset = data['splited_testsuite_b'][3:5]
    e = data['splited_testsuite_b'][5]
    nset = [Example(tname, rname, p, "semgrep", False, False) for p in nset]
    e = Example(tname, rname, e, "semgrep", False, True)
    #print(spfl(rule, nset, e))
    diff_results = lcp_locate(rule, nset, e)
    for enode, anode in e.m.items():
        print(f"match : {e.em[enode]['op']} -> {e.am[anode]['op']}")
    
    for (diff, intervals, intersections, content) in diff_results:
        for bad, good in diff:
            # interval
            if isinstance(bad[0], tuple):
                bad = bad[0]
            fact = graph.get_fact(e.graph, bad[0])
            print("locate to expl id: ", fact['id'])
            print("locate to expl op: ", fact['op'])
            print("locate to ast: ", e.am[e.m[fact['id']]])

def test_empty_expl():
    import json
    data = json.load(open("examples/bug.json"))
    rule = data['rule']
    rname = data['rule_path']
    tname = data['test_path']
    pset = data['splited_testsuite_b'][1:3]
    nset = data['splited_testsuite_b'][3:5]
    e = data['splited_testsuite_b'][5]
    nset = [Example(tname, rname, p, "semgrep", False, False) for p in nset]
    e = Example(tname, rname, e, "semgrep", False, True)
    outjson = empty_explanations(rule, nset, e)
    # from pprint import pprint
    # pprint(outjson)


if __name__ == "__main__":
    test()
    bug()
    test_empty_expl()