package edu.polyu.transform;

import edu.polyu.analysis.TypeWrapper;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import java.util.ArrayList;
import java.util.List;

import static edu.polyu.analysis.TypeWrapper.getChildrenNodes;
import static edu.polyu.analysis.TypeWrapper.isLiteral;

public class AddArgAssignment extends Transform {

    private static int newVarCounter = 0;

    private static AddArgAssignment addArgAssignment = new AddArgAssignment();

    private AddArgAssignment() {}

    public static Transform getInstance() {
        return addArgAssignment;
    }

    @Override
    public boolean run(ASTNode targetNode, TypeWrapper wrapper, ASTNode brotherStatement, ASTNode srcNode) {
        AST ast = wrapper.getAst();
        ASTRewrite astRewrite = wrapper.getAstRewrite();
        SimpleName newVarName = ast.newSimpleName("var" + newVarCounter++);
        VariableDeclarationFragment newVdFragment = ast.newVariableDeclarationFragment();
        newVdFragment.setName(newVarName);
        newVdFragment.setInitializer((Expression) ASTNode.copySubtree(ast, targetNode));
        VariableDeclarationStatement newVdStatement = ast.newVariableDeclarationStatement(newVdFragment);
        newVdStatement.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));
        if(targetNode instanceof StringLiteral) {
            newVdStatement.setType(ast.newSimpleType(ast.newSimpleName("String")));
        }
        if(targetNode instanceof NumberLiteral) {
            String value = ((NumberLiteral) targetNode).getToken();
            if(value.contains(".")) {
                newVdStatement.setType(ast.newPrimitiveType(PrimitiveType.DOUBLE));
            } else {
                if(value.endsWith("L") || value.endsWith("l")) {
                    newVdStatement.setType(ast.newPrimitiveType(PrimitiveType.LONG));
                } else {
                    value = value.toLowerCase();
                    long longValue;
                    if(value.contains("0x") || value.contains("0X") || value.contains("a") || value.contains("b") || value.contains("c") || value.contains("d") || value.contains("e") ||
                            value.contains("f")) {
                        if(value.startsWith("0x") || value.startsWith("0X")) {
                            value = value.substring(2);
                        }
                        longValue = Long.parseLong(value, 16);
                    } else {
                        longValue = Long.parseLong(value);
                    }
                    if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE) {
                        newVdStatement.setType(ast.newPrimitiveType(PrimitiveType.LONG));
                    } else {
                        newVdStatement.setType(ast.newPrimitiveType(PrimitiveType.INT));
                    }
                }
            }
        }
        if(targetNode instanceof BooleanLiteral) {
            newVdStatement.setType(ast.newPrimitiveType(PrimitiveType.BOOLEAN));
        }
        if(targetNode instanceof CharacterLiteral) {
            newVdStatement.setType(ast.newPrimitiveType(PrimitiveType.CHAR));
        }
        astRewrite.replace(targetNode, newVarName, null);
        ListRewrite listRewrite = astRewrite.getListRewrite(brotherStatement.getParent(), Block.STATEMENTS_PROPERTY);
        try {
            listRewrite.insertBefore(newVdStatement, brotherStatement, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<ASTNode> check(TypeWrapper wrapper, ASTNode node) {
        List<ASTNode> nodes = new ArrayList<>();
        List<ASTNode> subNodes = getChildrenNodes(node);
        if(node instanceof VariableDeclarationStatement || node instanceof ExpressionStatement) {
            for(ASTNode subNode : subNodes) {
                if(isLiteral(subNode)) {
                    nodes.add(subNode);
                }
            }
        }
        if(node instanceof ReturnStatement) {
            if(isLiteral(((ReturnStatement) node).getExpression())) {
                return nodes;
            }
            for(ASTNode subNode : subNodes) {
                if(isLiteral(subNode)) {
                    nodes.add(subNode);
                }
            }
        }
        return nodes;
    }

}
