import networkx as nx
from yaml import load, dump

def get_fact(G, node):
    return nx.get_node_attributes(G, 'fact')[node]

def get_label(G, node):
    return nx.get_node_attributes(G, 'label')[node]

def find_all_paths(G:nx.DiGraph, s, t, is_true):
    all_paths = list(nx.all_simple_paths(G, s, t))
    result = []
    for path in all_paths:
        p = []
        for node in path:
            p.append((node, is_true(G, node)))
        result.append(p)
    return result

def positive_path(path):
    for p in path:
        if not p[1]:
            return False
    return True

def negative_path(path):
    return not positive_path(path)

def diff(p1, p2):
    def find_index(p, nodeid):
        for i, x in enumerate(p):
            if x[0] == nodeid:
                return i
        return -1
    intersections = []
    intervals = []
    lx = 0
    ly = 0
    for i, x in enumerate(p1):
        j = find_index(p2, x[0])
        if j == -1:
            continue
        intersections.append((x, p2[j]))
        if lx+1 < i and ly+1 < j:
            intervals.append((p1[lx:i], p2[ly:j]))
        lx = i
        ly = j

    diff = []
    for x, y in intersections:
        if x[1] != y[1]:
            diff.append((x, y))
    for x, y in intervals:
        r1 = all(item[1] for item in x)
        r2 = all(item[1] for item in y)
        if r1 != r2:
            diff.append((x, y))
    return diff, intersections, intervals

def priority(diff, intersections, intervals):
    if not diff:
        return 0
    return (len(intersections) + len(intervals) - len(diff)) / len(diff)


