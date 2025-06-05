import json
import yaml
import semgrep2nx

negitive_tags = {"pattern-not", "pattern-not-inside", "pattern-not-regex"}

def either_template(anode):
    op = anode['op']
    if isinstance(op, list):
        op = ["<PATTERN>", "<GENERATE A PATTERN HERE>"]
    else:
        op = "<PATTERN>"
    template_node = {
        "op": op,
        "children": [],
        "id": 0xdeaddead
    }
    return {"op": "pattern-either", "children": [[anode], [template_node]], "id": 0xdeadbeef}

def and_template(anode):
    op = anode['op']
    if isinstance(op, list):
        op = ["<PATTERN>", "<GENERATE A PATTERN HERE>"]
    else:
        op = "<PATTERN>"
    template_node = {
        "op": op,
        "children": [],
        "id": 0xdeaddead
    }
    return {"op": "patterns", "children": [[anode], [template_node]], "id": 0xdeadbeef}


# anode -> target
def replace(root, anode, target):
    import copy
    if isinstance(root, str):
        return copy.deepcopy(root)
    if isinstance(root, list):
        return [replace(c, anode, target) for c in root]
    if root == anode:
        return target
    else:
        copy_root = copy.deepcopy(root)
        copy_root["children"] = [replace(c, anode, target) for c in root["children"]]
        return copy_root

def compare(root1, root2):
    if isinstance(root1, str):
        return root1 == root2
    if isinstance(root1, list):
        return all([compare(c1, c2) for c1, c2 in zip(root1, root2)])
    if root1['op'] != root2['op']:
        return False
    return all([compare(c1, c2) for c1, c2 in zip(root1['children'], root2['children'])])

def compress(root):
    if isinstance(root, str):
        return root
    if isinstance(root, list):
        return [compress(c) for c in root]
    new_children = []
    if root['op'] == "patterns":
        for c in root['children']:
            if isinstance(c, list):
                cc = c[0]
                if cc['op'] == "patterns":
                    new_children.extend(cc['children'])
                else:
                    new_children.append(c)
            else:
                new_children.append(c)
        root['children'] = new_children
    elif root['op'] == "pattern-either" or root['op'] == "sinks" or root['op'] == "sources" or root['op'] == "sanitizers":
        for c in root['children']:
            if isinstance(c, list):
                cc = c[0]
                if cc['op'] == "pattern-either":
                    new_children.extend(cc['children'])
                else:
                    new_children.append(c)
            else:
                new_children.append(c)
        root['children'] = new_children
    root['children'] = [compress(c) for c in root['children']]
    # redo this node
    return root

def gen_template(e, loc):
    import graph
    d1, d2 = graph.diff(loc[2], loc[3])
    fact = graph.get_fact(e.graph, d1[0])
    # print("locate to expl id: ", fact['id'])
    # print("locate to expl op: ", fact['op'], fact['matches'])
    # print("locate to ast: ", e.am[e.m[fact['id']]])

    # bug: this fails when there is only one pattern in the rule
    anode = e.am[e.m[fact['id']]]

    # if d1[1] is True, we should "and" a template to make it shrink, versus "or" a template to make it expand.
    if d1[1]:
        node = and_template(anode)
    else:
        node = either_template(anode)
    replaced = replace(e.ast, anode, node)
    # compress the ast 5 times at most
    for i in range(5):
        replaced = compress(replaced)
    ast = semgrep2nx.trans_back([replaced])
    r = semgrep2nx.ast2yaml(ast)
    return r

def analysis_loc(e, localization):
    import graph
    def get_xpat(gid):
        try:
            fact = graph.get_fact(e.graph, gid)
            anode = e.am[e.m[fact['id']]]
            ast = semgrep2nx.trans_back([anode])
            xpat = semgrep2nx.ast2yaml(ast)
        except:
            return ""
        return xpat

    # loc : [diff, intersections, intervals, s.content]
    diff, its, itv, similar_case = localization
    if len(diff) > 0:
        diff_its_itv = diff[0]
    bad, good = diff_its_itv[0], diff_its_itv[1]
    if isinstance(bad, list):
        overmatch = all([node[1]] for node in bad)
    else:
        overmatch = bad[1]

    bad_xpat = []
    if not isinstance(bad, list):
        bad = [bad]
    for node in bad:
        bad_xpat.append(get_xpat(node[0]))

    good_xpat = []
    if not isinstance(good, list):
        good = [good]
    for node in good:
        good_xpat.append(get_xpat(node[0]))

    # print(xpat)
    return bad_xpat, good_xpat, similar_case, overmatch


rule = """
rules:
  - id: command-injection-formatted-runtime-call
    languages:
      - java
    message: A formatted or concatenated string was detected in a java.lang.Runtime
      call. This poses a risk if user-controlled variables are involved,
      potentially leading to command injection. Ensure variables are not
      user-controlled or properly sanitized.
    patterns:
      - pattern-either:
          - pattern: $RUNTIME.exec($X + $Y);
          - pattern-either:
              - pattern: $RUNTIME.loadLibrary(String.format(...));
              - pattern: $RUNTIME.exec($CMD, $ENVP, $ARG);
          - pattern: $RUNTIME.exec(String.format(...));
          - pattern: $RUNTIME.loadLibrary($X + $Y);
"""
def test_compress():
    from semgrep2nx import yaml2ast, ast2yaml,trans, trans_back
    ast = yaml2ast(rule)
    ast = trans(ast)[0]
    for i in range(5):
        ast = compress(ast)
    ast = compress(ast)
    ast = trans_back([ast])
    print(ast2yaml(ast))
    

if __name__ == "__main__":
    test_compress()