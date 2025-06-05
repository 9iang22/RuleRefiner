import tree_sitter_ql as tsql
from tree_sitter import Language, Parser
from pprint import pprint
from utils import Counter
import networkx as nx
from pyvis.network import Network


LAN = Language(tsql.language())
parser = Parser(LAN)

def get_source(node, source):
    start_byte = node.start_byte
    end_byte = node.end_byte
    node_source = source[start_byte:end_byte]
    return node_source

def parse(codeql):
    tree =  parser.parse(bytes(codeql,"utf8"))
    return tree

def extract_from_select(root):
    select = root.children[0].children[0]
    if select.type == "select":
        for i, s in enumerate(select.children):
            if s.type == "where" and len(select.children) > i+1:
                return select.children[i+1]
    return None

def extract_from_predicate(root):
    predicate = root.children[0].children[0]
    if predicate.type == "classlessPredicate":
        for c in predicate.children:
            if c.type == "body":
                return c
    return None


def simplfiy(G, start, end):
    labels = nx.get_node_attributes(G, 'label')

    def is_empty(node):
        return (labels.get(node) == "Start" and node != start) or (labels.get(node) == "End" and node != end)
    
    work = 1
    while work == 1:
        rm = []
        add = []
        work = 0
        for node in list(G.nodes):
            for succ in list(G.successors(node)):
                if is_empty(succ):
                    if G.in_degree(succ) == 1:
                        for succ_2 in list(G.successors(succ)):
                            G.add_edge(node, succ_2)
                        G.remove_node(succ)
                        work = 1; break
            if work : break
            for pred in list(G.predecessors(node)):
                if is_empty(pred):
                    if G.out_degree(pred) == 1:
                        for pred_2 in list(G.predecessors(pred)):
                            G.add_edge(pred_2, node)
                        G.remove_node(pred)
                        work = 1; break
            if work : break
    for node in list(G.nodes):
        if G.in_degree(node) == 0 and G.out_degree(node) == 0:
            G.remove_node(node)
    return G

def labelize(src, node):
    source = get_source(node, src)
    # if len(source) < 20:
    #     return source
    # return f"{source[:20]}..."
    return source

def Codeql2NX(root, C, source):
    if not root.is_named:
        return None, None, None
    G = nx.DiGraph()
    st = next(C)
    G.add_node(st, label="Start", fact=None, color='grey')
    ed = next(C)
    G.add_node(ed, label="End", fact=None, color='grey')
    G.add_edge(st, ed)
    if root.type == "qualified_expr" or root.type == "instance_of" or root.type == "quantified":
        c = next(C)
        G.add_node(c, label=labelize(source, root), fact=None, color="#FF6347")
        G.add_edge(c, ed)
        G.add_edge(st, c)
        G.remove_edge(st, ed)
    elif root.type == "conjunction":
        result = []
        next_connect = st
        for c in root.children:
            n_pred, n_succ, g = Codeql2NX(c, C, source)
            if not g: continue
            G.add_edge(next_connect, n_pred)
            G.add_edge(n_succ, ed)
            G.remove_edge(next_connect, ed)
            G = nx.compose(G, g)
            next_connect = n_succ
        G.add_edge(next_connect, ed)
    elif root.type == "disjunction":
        for child in root.children:
            n_pred, n_succ, g = Codeql2NX(child, C, source)
            if not g: continue
            G = nx.compose(G, g)
            G.add_edge(st, n_pred)
            G.add_edge(n_succ, ed)
            if G.has_edge(st, ed):
                G.remove_edge(st, ed)
    elif root.type == "negation":
        c = next(C)
        G.add_node(c, label=labelize(source, root), fact=None, color="#FF6347")
        G.add_edge(c, ed)
        G.add_edge(st, c)
        G.remove_edge(st, ed)
    elif root.type == "par_expr" or root.type == "body":
        # ( expr ... )
        for c in root.named_children:
            pred, succ, g = Codeql2NX(root.children[1], C, source)
            if g:
                G = nx.compose(G, g)
                G.add_edge(st, pred)
                G.add_edge(succ, ed)
                if G.has_edge(st, ed):
                    G.remove_edge(st, ed)

    comp = nx.weakly_connected_components(G)
    if not len(list(comp)) == 1:
        net = Network(notebook=True, directed=True)
        net.from_nx(G)
        net.save_graph("debug.html")
        assert False
    return st, ed, G

def test():
    test_ql = """
predicate unbracedTrailingBody(Stmt ctrlStructure, Stmt trailingBody) {
  not trailingBody instanceof BlockStmt and
  (
    exists(IfStmt c | c = ctrlStructure |
      trailingBody = c.getElse() and not trailingBody instanceof IfStmt
      or
      trailingBody = c.getThen() and not exists(c.getElse())
    )
    or
    exists(LoopStmt c | c = ctrlStructure | not c instanceof DoStmt and trailingBody = c.getBody())
  )
}
"""
    tree = parse(test_ql)
    root = tree.root_node
    body = extract_from_select(root)
    if not body:
        body = extract_from_predicate(root)
    print(body)
    st, ed, G = Codeql2NX(body, Counter(), test_ql)
    print(G)
    G = simplfiy(G, st, ed)
    print(G)
    net = Network(notebook=True, directed=True)
    net.from_nx(G)
    net.save_graph("codeql2nx.html")

if __name__ == "__main__":
    test()