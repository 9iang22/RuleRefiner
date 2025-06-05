import json
import networkx as nx
import xml.etree.ElementTree as ET
import json
import ts_cfg
import logging

def parse_jacoco_xml(xml_path):
    tree = ET.parse(xml_path)
    root = tree.getroot()

    coverage_data = {}

    for package in root.findall('package'):
        for sourcefile in package.findall('sourcefile'):
            filename = sourcefile.get('name')
            lines = {}

            for line in sourcefile.findall('line'):
                line_data = {
                    'mi': int(line.get('mi')),
                    'ci': int(line.get('ci')),
                    'mb': int(line.get('mb')),
                    'cb': int(line.get('cb'))
                }
                lines[int(line.get('nr'))] = line_data

            if filename not in coverage_data:
                coverage_data[filename] = {}
            
            coverage_data[filename].update(lines)
    return coverage_data

pmd_report_func = [
    "addViolation",
    "addViolationWithPosition",
    "addViolationWithMessage",
]

def is_positive(node):
    if not node:
        return False
    if node.type == "method_invocation":
        if node.child_by_field_name('name').text.decode() in pmd_report_func:
            return True
    for child in node.children:
        if is_positive(child):
            return True
    return False


def pmd2nx(cfg):
    G = nx.DiGraph()
    vst = {}
    am = {}
    def _dfs(node):
        if node in vst:
            return
        vst[node] = True
        id = node.id
        fact = node.json()
        fact['NegEnd'] = False
        fact['PosEnd'] = is_positive(node.ast)
        G.add_node(id, label=str(node), fact=fact)
        am.update({id:node.ast})
        for succ, cond in node.succs:
            G.add_edge(id, succ.id, label=cond, fact=cond)
            _dfs(succ)

    for method_name, entry_block in cfg.items():
        _dfs(entry_block)
    return G, am

def view(G):
    from pyvis.network import Network
    net = Network(notebook=False, directed=True)
    net.from_nx(G)
    net.save_graph("pmd2nx.html")


def color(G : nx.DiGraph):
    facts = nx.get_node_attributes(G, 'fact')
    for node in list(G.nodes):
        if node in facts:
            fact = facts[node]
            if fact['PosEnd']:
                G.nodes[node]['color'] = 'red'
            elif fact['covered']:
                G.nodes[node]['color'] = 'green'
            else:
                G.nodes[node]['color'] = 'grey'
    for a, b in list(G.edges):
        if a in facts and b in facts:
            if facts[a]['covered'] and facts[b]['covered']:
                G.edges[a, b]['color'] = 'green'
            else:
                G.edges[a, b]['color'] = 'grey'
    return G

def is_covered(line_cov):
    mi = line_cov['mi']
    ci = line_cov['ci']
    mb = line_cov['mb']
    cb = line_cov['cb']
    if mb + cb == 0:
        return ci > 0
    else:
        return cb > 0

def attach_coverage(G : nx.DiGraph, coverage_data):
    facts = nx.get_node_attributes(G, 'fact')
    for node in list(G.nodes):
        if node in facts:
            fact = facts[node]
            fact["covered"] = False
            if fact["position"]:
                st_lno = fact["position"]["start"][0] + 1
                if st_lno not in coverage_data:
                    fact['covered'] = True
                    continue
                line_cov = coverage_data[st_lno]
                fact['covered'] = is_covered(line_cov)
            else:
                fact['covered'] = True
        else:
            logging.warning(f"Node {node} not found in facts")
    nx.set_node_attributes(G, facts, 'fact')
    return G

if __name__ == "__main__":
    source = open("examples/pmd_localization.java").read()
    cfg = ts_cfg.build_cfg_from_source(source)
    g, am = pmd2nx(cfg.methods)
    coverage_data = parse_jacoco_xml("examples/pmd_localization.xml")["RedundantFieldInitializerRule.java"]
    g = attach_coverage(g, coverage_data)
    g = color(g)
    view(g)
        

