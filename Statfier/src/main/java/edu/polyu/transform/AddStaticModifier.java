package edu.polyu.transform;

import edu.polyu.analysis.TypeWrapper;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.ArrayList;
import java.util.List;

public class AddStaticModifier extends Transform {

    private static final AddStaticModifier instance = new AddStaticModifier();

    private AddStaticModifier() {}

    public static Transform getInstance() {
        return instance;
    }

    @Override
    public boolean run(ASTNode targetNode, TypeWrapper wrapper, ASTNode brotherNode, ASTNode srcNode) {
        AST ast = wrapper.getAst();
        ASTRewrite astRewrite = wrapper.getAstRewrite();
        FieldDeclaration oldFieldDeclaration = (FieldDeclaration) srcNode;
        for(ASTNode modifier : (List<ASTNode>) oldFieldDeclaration.modifiers()) {
            if(modifier instanceof Modifier && ((Modifier) modifier).getKeyword().toString().equals("static")) {
                return false;
            }
        }
        List<ASTNode> nodes = new ArrayList<>();
        nodes.add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
        return true;
    }

    @Override
    public List<ASTNode> check(TypeWrapper wrapper, ASTNode node) {
        List<ASTNode> nodes = new ArrayList<>();
        if(node instanceof FieldDeclaration) {
            nodes.add(node);
        }
        return nodes;
    }

}
