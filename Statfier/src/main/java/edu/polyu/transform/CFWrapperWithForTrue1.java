package edu.polyu.transform;


import org.eclipse.jdt.core.dom.ConstructorInvocation;
import edu.polyu.analysis.TypeWrapper;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.ArrayList;
import java.util.List;

public class CFWrapperWithForTrue1 extends Transform {

    private static final CFWrapperWithForTrue1 instance = new CFWrapperWithForTrue1();

    private static int varCounter;

    private CFWrapperWithForTrue1() {
        varCounter = 0;
    }

    public static CFWrapperWithForTrue1 getInstance() {
        return instance;
    }

    @Override
    public boolean run(ASTNode targetNode, TypeWrapper wrapper, ASTNode brother, ASTNode srcNode) {
        AST ast = wrapper.getAst();
        ASTRewrite astRewrite = wrapper.getAstRewrite();
        ASTNode parNode = srcNode.getParent();
        if(parNode instanceof Block) {
            if(((Block) parNode).statements().size() == 1) {
                return false;
            }
        }
        ForStatement newForStatement = ast.newForStatement();
        newForStatement.setExpression(ast.newBooleanLiteral(true));
        Block newForBodyBlock = ast.newBlock();
        Statement newStatement = (Statement) ASTNode.copySubtree(ast, srcNode);
        newForBodyBlock.statements().add(newStatement);
        newForBodyBlock.statements().add(ast.newBreakStatement());
        newForStatement.setBody(newForBodyBlock);
        astRewrite.replace(srcNode, newForStatement, null);
        return true;
    }

    // int a; a = 10; int a;
    public static boolean InitCheck(ASTNode node) {
        if(node instanceof ExpressionStatement && ((ExpressionStatement) node).getExpression() instanceof Assignment) {
            Assignment assignment = (Assignment) ((ExpressionStatement) node).getExpression();
            if(assignment.getLeftHandSide() instanceof SimpleName) {
                String varName = ((SimpleName) assignment.getLeftHandSide()).getIdentifier();
                MethodDeclaration method = TypeWrapper.getDirectMethodOfNode(node);
                if(method == null || method.getBody() == null) {
                    return false;
                }
                List<Statement> statements = TypeWrapper.getAllStatements(method.getBody().statements());
                for (Statement statement : statements) {
                    if (statement instanceof VariableDeclarationStatement) {
                        VariableDeclarationFragment vdFragment = (VariableDeclarationFragment) ((VariableDeclarationStatement) statement).fragments().get(0);
                        if (vdFragment.getName().getIdentifier().equals(varName) && vdFragment.getInitializer() == null) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<ASTNode> check(TypeWrapper wrapper, ASTNode node) {
        List<ASTNode> nodes = new ArrayList<>();
        if(TypeWrapper.isLiteral(node)) {
            return nodes;
        }
        if(!(node instanceof Statement) && !(node instanceof Block)) {
            return nodes;
        }
        ASTNode par = node.getParent();
        if (node instanceof Statement && (par instanceof IfStatement || par instanceof WhileStatement ||
                par instanceof DoStatement || par instanceof ForStatement)) {
            return nodes;
        }
        if(node instanceof VariableDeclarationStatement || node instanceof FieldDeclaration ||
            node instanceof MethodDeclaration || node instanceof ReturnStatement || node instanceof SuperConstructorInvocation) {
            return nodes;
        }
        if(node.getParent().getParent() instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) node.getParent().getParent();
            List<ASTNode> statements = method.getBody().statements();
            // To avoid transforming the first statement which includes "this"
            if(method.isConstructor() && !statements.isEmpty() && node == statements.get(0) && node instanceof ConstructorInvocation) {
                return nodes;
            }
        }
        nodes.add(node);
        return nodes;
    }

}
