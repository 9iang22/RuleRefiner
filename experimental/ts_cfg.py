from tree_sitter import Language, Parser, Tree, Node
from collections import defaultdict
import tree_sitter_java
import networkx as nx
from pyvis.network import Network

parser = Parser()
parser.language = Language(tree_sitter_java.language())

class TSHelper:
    @staticmethod
    def find_methods(node):
        methods = {}
        if node.type == 'method_declaration':
            methods[node.child_by_field_name('name').text.decode()] = node
        for child in node.children:
            methods.update(TSHelper.find_methods(child))
        return methods
    
    @staticmethod
    def gen_ast(source):
        tree = parser.parse(bytes(source, "utf8"))
        return tree.root_node
    
    @staticmethod
    def visit(node, callback):
        if not callback(node):
            return False
        for child in node.children:
            if not TSHelper.visit(child, callback):
                return False
        return True
    

class CFGNode:
    def __init__(self, id):
        self.id = id
        self.statements = []
        self.ast : Node = None
        self.entry = False
        self.exit = False
        self.condition = None
        self.succs = []
    
    def add_statement(self, statement):
        self.statements.append(statement)
    
    def __repr__(self):
        statement = "dummy"
        if self.entry: return "ENTRY"
        if self.exit: return "EXIT"
        if self.ast: 
            statement = self.ast.text.decode()
            lno = self.ast.start_point[0] + 1
        else: 
            lno = -1
        if len(statement) > 20:
            statement = statement[:20] + "..."
        if lno == -1:
            return f"B{self.id}: {statement}"
        return f"L{lno} B{self.id}: {statement}"
    
    def __hash__(self):
        return self.id

    def json(self):
        return {
            "id": self.id,
            "statements": self.statements,
            "entry": self.entry,
            "exit": self.exit,
            "condition": self.condition,
            "succs": [{"id": succ.id, "cond" : label} for succ, label in self.succs],
            "position" : {"start": self.ast.start_point, "end" : self.ast.end_point} if self.ast else None
        }


class CFGBuilder:
    def __init__(self):
        global parser
        self.parser = parser
        self.source = None
        self.tree = None
        self.cfg = defaultdict(list)
        self.block_counter = 0
        self.block_stack = []
        self.loop_scope = []

    def new_block(self, ast=None):
        self.block_counter += 1
        return CFGNode(self.block_counter)

    def new_entry(self):
        new_block = self.new_block()
        new_block.entry = True
        new_block.add_statement("ENTRY")
        return new_block
    
    def new_exit(self):
        new_block = self.new_block()
        new_block.exit = True
        new_block.add_statement("EXIT")
        return new_block
    
    def connect(self, source, target, label=None):
        if source and target:
            self.cfg[source].append((target, label))
            source.succs.append((target, label))
    
    def build_from_code(self, code):
        tree = self.parser.parse(bytes(code, "utf8"))
        root = tree.root_node
        methods = self.find_methods(root)
        self.methods = {}
        self.source = code
        self.tree = tree
        for method_name, method in methods.items():
            body = method.child_by_field_name('body')
            exit_block = self.new_exit()
            self.exit_block = exit_block
            st, ed = self.traverse(body)
            self.connect(ed, exit_block)
            entry = self.new_entry()
            self.connect(entry, st)
            self.methods[method_name] = entry
        return self
    
    def find_methods(self, node):
        methods = {}
        if node.type == 'method_declaration':
            methods[node.child_by_field_name('name').text.decode()] = node
        for child in node.children:
            methods.update(self.find_methods(child))
        return methods
    
    def dispatch(self, node: Node):
        if node.type == 'if_statement':
            st, ed = self.handle_if_statement(node)
        elif node.type in ['for_statement', 'while_statement', "enhanced_for_statement"]:
            st, ed = self.handle_loop(node)
        elif node.type == "return_statement":
            st, ed = self.handle_return(node)
        elif node.type == "break_statement":
            st, ed = self.handle_break(node)
        else:
            st, ed = self.handle_normal(node)
        return st, ed

    def traverse(self, node : Node):
        entry = None
        exit = None
        if node.type == 'block':
            for child in node.children:
                if not child.is_named: continue
                if child.type == "line_comment": continue
                st, ed = self.dispatch(child)
                if not entry: entry = st
                else: self.connect(exit, st)
                if not ed: break
                exit = ed
            return entry, exit
        else:
            return self.dispatch(node)

    def handle_normal(self, node):
        block = self.new_block()
        block.ast = node
        block.add_statement(node.text.decode())
        return block, block

    
    def handle_if_statement(self, node):
        condition = node.child_by_field_name('condition')
        condition_block = self.new_block()
        after_block = self.new_block()
        condition_block.condition = condition.text.decode()
        condition_block.ast = condition

        consequence = node.child_by_field_name('consequence')
        st, ed = self.traverse(consequence)
        self.connect(condition_block, st, 'True')
        self.connect(ed, after_block, None)

        alternative = node.child_by_field_name('alternative')
        if alternative: 
            st, ed = self.traverse(alternative)
            self.connect(condition_block, st, 'False')
            self.connect(ed, after_block, None)
        else:
            self.connect(condition_block, after_block, 'False')
        return condition_block, after_block
    
    def handle_loop(self, node):
        loop_condition = node.child_by_field_name('condition')
        condition_block = self.new_block()
        after_block = self.new_block()
        if loop_condition: 
            condition_block.condition = loop_condition.text.decode()
            condition_block.ast = loop_condition
            loop_body = node.child_by_field_name('body')
            self.loop_scope.append(after_block)
            st, ed = self.traverse(loop_body)
            self.loop_scope.pop()

            self.connect(condition_block, st, 'True')
            self.connect(condition_block, after_block, 'False')
            self.connect(ed, after_block, None)  # break loop
            return condition_block, after_block
        else: # enhanced_for_statement
            st, ed = self.traverse(node.child_by_field_name('body'))
            return st, ed

    def handle_break(self, node):
        break_block = self.new_block()
        break_block.add_statement(node.text.decode())
        break_block.ast = node
        if len(self.loop_scope) == 0:
            # raise Exception("Break statement outside loop")
            return break_block, None
        self.connect(break_block, self.loop_scope[-1], None)
        return break_block, None

    def handle_continue(self, node):
        continue_block = self.new_block()
        continue_block.add_statement(node.text.decode())
        continue_block.ast = node
        return continue_block, None

    def handle_return(self, node):
        return_block = self.new_block()
        return_block.add_statement(node.text.decode())
        return_block.ast = node
        self.connect(return_block, self.exit_block, None)
        return return_block, None

def visualize_cfg(cfg, output_file="cfg.html"):
    G = nx.DiGraph()
    node_details = {}
    for source, edges in cfg.items():
        if source.id not in node_details:
            G.add_node(source.id, label=str(source))
        for target, label in edges:
            if target.id not in node_details:
                G.add_node(target.id, label=str(target))
            edge_label = f" {label}" if label else ""
            G.add_edge(source.id, target.id, label=edge_label, title=edge_label)
    
    # 使用PyVis生成可视化
    net = Network(notebook=True, directed=True, height="750px", width="100%")
    net.from_nx(G)
    
    # 配置节点显示
    for node in net.nodes:
        node['shape'] = 'box'
        node['color'] = '#e6f3ff'
    
    # 保存HTML文件
    net.show(output_file)
    print(f"Visualization saved to {output_file}")

def build_cfg_from_source(code):
    cfg_builder = CFGBuilder()
    return cfg_builder.build_from_code(code)

if __name__ == "__main__":
    # 示例用法
    code = open("/home/jiangzz/SARR/graph/examples/493e0014b0/after/UnnecessaryFullyQualifiedNameRule.java").read()

    code = """
/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package custom.lang.java.rule.optimizations;

import java.math.BigInteger;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.ASTBooleanLiteral;
import net.sourceforge.pmd.lang.java.ast.ASTCastExpression;
import net.sourceforge.pmd.lang.java.ast.ASTExpression;
import net.sourceforge.pmd.lang.java.ast.ASTFieldDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTLiteral;
import net.sourceforge.pmd.lang.java.ast.ASTNullLiteral;
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryExpression;
import net.sourceforge.pmd.lang.java.ast.ASTReferenceType;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclarator;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

/**
 * Detects redundant field initializers, i.e. the field initializer expressions the JVM would assign by default.
 *
 * @author lucian.ciufudean@gmail.com
 * @since Apr 10, 2009
 */
public class RedundantFieldInitializerRule extends AbstractJavaRule {

    public RedundantFieldInitializerRule() {
	addRuleChainVisit(ASTFieldDeclaration.class);
    }

    public Object visit(ASTFieldDeclaration fieldDeclaration, Object data) {
	// Finals can only be initialized once.
	if (fieldDeclaration.isFinal()) {
	    return data;
	}

	// Look for a match to the following XPath:
	//    VariableDeclarator/VariableInitializer/Expression/PrimaryExpression/PrimaryPrefix/Literal
	for (ASTVariableDeclarator variableDeclarator : fieldDeclaration
		.findChildrenOfType(ASTVariableDeclarator.class)) {
	    if (variableDeclarator.jjtGetNumChildren() > 1) {
		final Node variableInitializer = variableDeclarator.jjtGetChild(1);
		if (variableInitializer.jjtGetChild(0) instanceof ASTExpression) {
		    final Node expression = variableInitializer.jjtGetChild(0);
		    final Node primaryExpression;
		    if (expression.jjtGetNumChildren() == 1) {
			if (expression.jjtGetChild(0) instanceof ASTPrimaryExpression) {
			    primaryExpression = expression.jjtGetChild(0);
			} else if (expression.jjtGetChild(0) instanceof ASTCastExpression
				&& expression.jjtGetChild(0).jjtGetChild(1) instanceof ASTPrimaryExpression) {
			    primaryExpression = expression.jjtGetChild(0).jjtGetChild(1);
			} else {
			    continue;
			}
		    } else {
			continue;
		    }
		    final Node primaryPrefix = primaryExpression.jjtGetChild(0);
		    if (primaryPrefix.jjtGetNumChildren() == 1 && primaryPrefix.jjtGetChild(0) instanceof ASTLiteral) {
			final ASTLiteral literal = (ASTLiteral) primaryPrefix.jjtGetChild(0);
			if (isRef(fieldDeclaration, variableDeclarator)) {
			    // Reference type
			    if (literal.jjtGetNumChildren() == 1 && literal.jjtGetChild(0) instanceof ASTNullLiteral) {
				addViolation(data, variableDeclarator);
			    }
			} else {
			    // Primitive type
			    if (literal.jjtGetNumChildren() == 1 && literal.jjtGetChild(0) instanceof ASTBooleanLiteral) {
				// boolean type
				ASTBooleanLiteral booleanLiteral = (ASTBooleanLiteral) literal.jjtGetChild(0);
				if (!booleanLiteral.isTrue()) {
				    addViolation(data, variableDeclarator);
				}
			    } else if (literal.jjtGetNumChildren() == 0) {
				// numeric type
				// Note: Not catching NumberFormatException, as it shouldn't be happening on valid source code.
				Number value = -1;
				if (literal.isIntLiteral()) {
				    value = parseInteger(literal.getImage());
				} else if (literal.isLongLiteral()) {
				    String s = literal.getImage();
				    // remove the ending "l" or "L" for long values
					s = s.substring(0, s.length() - 1);
					value = parseInteger(s);
				} else if (literal.isFloatLiteral()) {
                    String s = literal.getImage();
                    // remove the ending "f" or "F" for float values
                    s = s.substring(0, s.length() - 1);
                    value = Float.valueOf(s);
				} else if (literal.isDoubleLiteral()) {
				    value = Double.valueOf(literal.getImage());
				} else if (literal.isCharLiteral()) {
				    value = (int) literal.getImage().charAt(1);
				}

				if (value.intValue() == 0) {
				    addViolation(data, variableDeclarator);
				}
			    }
			}
		    }
		}
	    }
	}

	return data;
    }

    /**
     * Checks if a FieldDeclaration is a reference type (includes arrays). The reference information is in the
     * FieldDeclaration for this example: <pre>int[] ia1</pre> and in the VariableDeclarator for this example:
     * <pre>int ia2[];</pre>.
     *
     * @param fieldDeclaration the field to check.
     * @param variableDeclarator the variable declarator to check.
     * @return <code>true</code> if the field is a reference. <code>false</code> otherwise.
     */
    private boolean isRef(ASTFieldDeclaration fieldDeclaration, ASTVariableDeclarator variableDeclarator) {
	Node type = fieldDeclaration.jjtGetChild(0).jjtGetChild(0);
	if (type instanceof ASTReferenceType) {
	    // Reference type, array or otherwise
	    return true;
	} else {
	    // Primitive array?
	    return ((ASTVariableDeclaratorId) variableDeclarator.jjtGetChild(0)).isArray();
	}
    }

    private void addViolation(Object data, ASTVariableDeclarator variableDeclarator) {
	super.addViolation(data, variableDeclarator, variableDeclarator.jjtGetChild(0).getImage());
    }

    private Number parseInteger(String s) {
        boolean negative = false;
        String number = s;
        if (number.charAt(0) == '-') {
            negative = true;
            number = number.substring(1);
        }
        BigInteger result;
        if (number.startsWith("0x") || number.startsWith("0X")) {
            result = new BigInteger(number.substring(2), 16);
        } else if (s.startsWith("0") && s.length() > 1) {
            result = new BigInteger(number.substring(1), 8);
        } else {
            result = new BigInteger(number);
        }
        if (negative) {
            result = result.negate();
        }
        return result;
    }
}
    """

    cfg_builder = CFGBuilder()
    cfg = cfg_builder.build_from_code(code)
    visualize_cfg(cfg.cfg, "example_cfg.html")