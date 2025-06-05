package edu.polyu.transform;

import edu.polyu.analysis.TypeWrapper;
import org.eclipse.jdt.core.dom.ASTNode;
import edu.polyu.analysis.SelectionAlgorithm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.polyu.util.Utility.CHECKSTYLE_MUTATION;
import static edu.polyu.util.Utility.COMPILE;
import static edu.polyu.util.Utility.DEBUG;
import static edu.polyu.util.Utility.DIV_SELECTION;
import static edu.polyu.util.Utility.GUIDED_LOCATION;
import static edu.polyu.util.Utility.INFER_MUTATION;
import static edu.polyu.util.Utility.NO_SELECTION;
import static edu.polyu.util.Utility.PMD_MUTATION;
import static edu.polyu.util.Utility.RANDOM_LOCATION;
import static edu.polyu.util.Utility.RANDOM_SELECTION;
import static edu.polyu.util.Utility.SONARQUBE_MUTATION;
import static edu.polyu.util.Utility.VARIABLE_LEVEL_MUTATOR;
import static edu.polyu.util.Utility.EXPRESSION_LEVEL_MUTATOR;
import static edu.polyu.util.Utility.STATEMENT_LEVEL_MUTATOR;
import static edu.polyu.util.Utility.CLASS_LEVEL_MUTATOR;
import edu.polyu.util.Schedule;

public abstract class Transform {

    private static List<Transform> transforms;
    public final static HashMap<String, Transform> name2transform;

    public abstract List<ASTNode> check(TypeWrapper wrapper, ASTNode node);

    public abstract boolean run(ASTNode targetNode, TypeWrapper wrapper, ASTNode broNode, ASTNode srcNode);

    public String getIndex() {
        return this.getClass().getSimpleName();
    }

//    static {
//        transforms = new ArrayList<>();
//        name2transform = new HashMap<>();
//        if (DEBUG) {
//            if(VARIABLE_LEVEL_MUTATOR){
//                transforms.add(AddArgAssignment.getInstance());//为变量改成先声明后赋值
//                transforms.add(AddGlobalAssignment.getInstance()); //为一个静态变量先声明后赋值
//                transforms.add(AddLocalAssignment.getInstance());  //对于一个变量，先声明再赋值
//                transforms.add(AddStaticAssignment.getInstance()); //对于一个static修饰变量先声明后赋值
//                transforms.add(AddStaticModifier.getInstance()); //对于一个static修饰变量先声明后赋值
//            }
//            if(EXPRESSION_LEVEL_MUTATOR){
//                transforms.add(AddBrackets.getInstance());  //加括号
//                transforms.add(AddRedundantLiteral.getInstance());  //冗余计算
//                transforms.add(CompoundExpression1.getInstance()); //把逻辑变量转化为等价逻辑表达式
//                transforms.add(CompoundExpression2.getInstance());
//                transforms.add(CompoundExpression3.getInstance());
//                transforms.add(CompoundExpression4.getInstance());
//                transforms.add(CompoundExpression5.getInstance());
//            }
//            if(STATEMENT_LEVEL_MUTATOR){
//                transforms.add(AddControlBranch.getInstance());  //加括号
//                transforms.add(CFWrapperWithDoWhile.getInstance());
//                transforms.add(CFWrapperWithForTrue1.getInstance());
//                transforms.add(CFWrapperWithForTrue2.getInstance());
//                transforms.add(CFWrapperWithIfTrue.getInstance());
//                transforms.add(CFWrapperWithWhileTrue.getInstance());
//                transforms.add(CFWrapperWithIfFalse.getInstance());//添加永远不会执行的流
//                transforms.add(LoopConversion1.getInstance());//添加永远不会执行的流
//                transforms.add(LoopConversion2.getInstance());//添加永远不会执行的流
//            }
//            if(CLASS_LEVEL_MUTATOR){
//                transforms.add(AnonymousClassWrapper.getInstance()); //对于一个static修饰变量先声明后赋值
//                transforms.add(EnumClassWrapper.getInstance()); //对于一个static修饰变量先声明后赋值
//                transforms.add(NestedClassWrapper.getInstance());//添加永远不会执行的流
//                transforms.add(TransferLocalVarToGlobal.getInstance()); //把局部变量转化为类的成员变量（private）
//                transforms.add(TransferLocalVarToStaticGlobal.getInstance()); //把局部变量转化为静态成员变量
//                transforms.add(AddMethodCallToLiteral.getInstance());//把常量写道另一个函数中
//            }
//        } else {
//            if(VARIABLE_LEVEL_MUTATOR){
//                transforms.add(AddArgAssignment.getInstance());//为变量改成先声明后赋值
//                transforms.add(AddGlobalAssignment.getInstance()); //为一个静态变量先声明后赋值
//                transforms.add(AddLocalAssignment.getInstance());  //对于一个变量，先声明再赋值
//                transforms.add(AddStaticAssignment.getInstance()); //对于一个static修饰变量先声明后赋值
//                transforms.add(AddStaticModifier.getInstance()); //对于一个static修饰变量先声明后赋值
//            }
//            if(EXPRESSION_LEVEL_MUTATOR){
//                transforms.add(AddBrackets.getInstance());  //加括号
//                transforms.add(AddRedundantLiteral.getInstance());  //冗余计算
//                transforms.add(CompoundExpression1.getInstance()); //把逻辑变量转化为等价逻辑表达式
//                transforms.add(CompoundExpression2.getInstance());
//                transforms.add(CompoundExpression3.getInstance());
//                transforms.add(CompoundExpression4.getInstance());
//                transforms.add(CompoundExpression5.getInstance());
//            }
//            if(STATEMENT_LEVEL_MUTATOR){
//                transforms.add(AddControlBranch.getInstance());  //加括号
//                transforms.add(CFWrapperWithDoWhile.getInstance());
//                transforms.add(CFWrapperWithForTrue1.getInstance());
//                transforms.add(CFWrapperWithForTrue2.getInstance());
//                transforms.add(CFWrapperWithIfTrue.getInstance());
//                transforms.add(CFWrapperWithWhileTrue.getInstance());
//                transforms.add(CFWrapperWithIfFalse.getInstance());//添加永远不会执行的流
//                transforms.add(LoopConversion1.getInstance());//添加永远不会执行的流
//                transforms.add(LoopConversion2.getInstance());//添加永远不会执行的流
//            }
//            if(CLASS_LEVEL_MUTATOR) {
//                transforms.add(AnonymousClassWrapper.getInstance()); //对于一个static修饰变量先声明后赋值
//                transforms.add(EnumClassWrapper.getInstance()); //对于一个static修饰变量先声明后赋值
//                transforms.add(NestedClassWrapper.getInstance());//添加永远不会执行的流
//                transforms.add(TransferLocalVarToGlobal.getInstance()); //把局部变量转化为类的成员变量（private）
//                transforms.add(TransferLocalVarToStaticGlobal.getInstance()); //把局部变量转化为静态成员变量
//                transforms.add(AddMethodCallToLiteral.getInstance());//把常量写道另一个函数中
//            }
//        }
//        for (Transform transform : transforms) {
//            name2transform.put(transform.getIndex(), transform);
//        }
//    }

    static {
        transforms = new ArrayList<>();
        name2transform = new HashMap<>();
        if (DEBUG) {
            if(VARIABLE_LEVEL_MUTATOR){
                transforms.add(AddArgAssignment.getInstance());//为变量改成先声明后赋值
                transforms.add(AddGlobalAssignment.getInstance()); //为一个静态变量先声明后赋值
                transforms.add(AddLocalAssignment.getInstance());  //对于一个变量，先声明再赋值
                transforms.add(AddStaticAssignment.getInstance()); //对于一个static修饰变量先声明后赋值
                transforms.add(AddStaticModifier.getInstance()); //对于一个static修饰变量先声明后赋值
            }
            if(EXPRESSION_LEVEL_MUTATOR){
                transforms.add(AddBrackets.getInstance());  //加括号
                transforms.add(AddRedundantLiteral.getInstance());  //冗余计算
                transforms.add(CompoundExpression1.getInstance()); //把逻辑变量转化为等价逻辑表达式
                transforms.add(CompoundExpression2.getInstance());
                transforms.add(CompoundExpression3.getInstance());
                transforms.add(CompoundExpression4.getInstance());
                transforms.add(CompoundExpression5.getInstance());
            }
            if(STATEMENT_LEVEL_MUTATOR){
                transforms.add(AddControlBranch.getInstance());  //加括号
                transforms.add(CFWrapperWithDoWhile.getInstance());
                transforms.add(CFWrapperWithForTrue1.getInstance());
                transforms.add(CFWrapperWithForTrue2.getInstance());
                transforms.add(CFWrapperWithIfTrue.getInstance());
                transforms.add(CFWrapperWithWhileTrue.getInstance());
                transforms.add(CFWrapperWithIfFalse.getInstance());//添加永远不会执行的流
                transforms.add(LoopConversion1.getInstance());//添加永远不会执行的流
                transforms.add(LoopConversion2.getInstance());//添加永远不会执行的流
            }
            if(CLASS_LEVEL_MUTATOR){
                transforms.add(AnonymousClassWrapper.getInstance()); //对于一个static修饰变量先声明后赋值
                transforms.add(EnumClassWrapper.getInstance()); //对于一个static修饰变量先声明后赋值
                transforms.add(NestedClassWrapper.getInstance());//添加永远不会执行的流
                transforms.add(TransferLocalVarToGlobal.getInstance()); //把局部变量转化为类的成员变量（private）
                transforms.add(TransferLocalVarToStaticGlobal.getInstance()); //把局部变量转化为静态成员变量
                transforms.add(AddMethodCallToLiteral.getInstance());//把常量写道另一个函数中
            }
        } else {
            if(VARIABLE_LEVEL_MUTATOR){
                transforms.add(AddArgAssignment.getInstance());//为变量改成先声明后赋值
                transforms.add(AddGlobalAssignment.getInstance()); //为一个静态变量先声明后赋值
                transforms.add(AddLocalAssignment.getInstance());  //对于一个变量，先声明再赋值
                transforms.add(AddStaticAssignment.getInstance()); //对于一个static修饰变量先声明后赋值
                transforms.add(AddStaticModifier.getInstance()); //对于一个static修饰变量先声明后赋值
            }
            if(EXPRESSION_LEVEL_MUTATOR){
                transforms.add(AddBrackets.getInstance());  //加括号
                transforms.add(AddRedundantLiteral.getInstance());  //冗余计算
                transforms.add(CompoundExpression1.getInstance()); //把逻辑变量转化为等价逻辑表达式
                transforms.add(CompoundExpression2.getInstance());
                transforms.add(CompoundExpression3.getInstance());
                transforms.add(CompoundExpression4.getInstance());
                transforms.add(CompoundExpression5.getInstance());
            }
            if(STATEMENT_LEVEL_MUTATOR){
                transforms.add(AddControlBranch.getInstance());  //加括号
                transforms.add(CFWrapperWithDoWhile.getInstance());
                transforms.add(CFWrapperWithForTrue1.getInstance());
                transforms.add(CFWrapperWithForTrue2.getInstance());
                transforms.add(CFWrapperWithIfTrue.getInstance());
                transforms.add(CFWrapperWithWhileTrue.getInstance());
                transforms.add(CFWrapperWithIfFalse.getInstance());//添加永远不会执行的流
                transforms.add(LoopConversion1.getInstance());//添加永远不会执行的流
                transforms.add(LoopConversion2.getInstance());//添加永远不会执行的流
            }
            if(CLASS_LEVEL_MUTATOR) {
                transforms.add(AnonymousClassWrapper.getInstance()); //对于一个static修饰变量先声明后赋值
                transforms.add(EnumClassWrapper.getInstance()); //对于一个static修饰变量先声明后赋值
                transforms.add(NestedClassWrapper.getInstance());//添加永远不会执行的流
                transforms.add(TransferLocalVarToGlobal.getInstance()); //把局部变量转化为类的成员变量（private）
                transforms.add(TransferLocalVarToStaticGlobal.getInstance()); //把局部变量转化为静态成员变量
                transforms.add(AddMethodCallToLiteral.getInstance());//把常量写道另一个函数中
            }
        }
        for (Transform transform : transforms) {
            name2transform.put(transform.getIndex(), transform);
        }
    }

    public static List<Transform> getTransforms() {
        return transforms;
    }

    public static AtomicInteger cnt1 = new AtomicInteger(0);
    public static AtomicInteger cnt2 = new AtomicInteger(0);

    public static void singleLevelExplorer(List<TypeWrapper> wrappers, int currentDepth) {  // Current depth means the depth of variants in wrappers, not the iteration level
        while (!wrappers.isEmpty()) {
            TypeWrapper wrapper = wrappers.get(0); // remove TypeWrapper in currentDepth level
            wrappers.remove(0);
            if (wrapper.depth == currentDepth) {
                if (!wrapper.isBuggy()) {
                    List<TypeWrapper> mutants = new ArrayList<>();
                    if (GUIDED_LOCATION) {
                        mutants = wrapper.TransformByGuidedLocation();
                    } else if (RANDOM_LOCATION) {
                        mutants = wrapper.TransformByRandomLocation();
                    }
                    if (DEBUG) {
                        System.out.println("Src Path: " + wrapper.getFilePath());
                        System.out.println("Mutant Size: " + mutants.size());
                    }
                    cnt1.addAndGet(mutants.size());
                    List<TypeWrapper> reducedMutants = null;
                    if (NO_SELECTION) {
                        reducedMutants = mutants;
                    }
                    if (RANDOM_SELECTION) {
                        reducedMutants = SelectionAlgorithm.Random_Selection(mutants);
                    }
                    if (DIV_SELECTION) {
                        reducedMutants = SelectionAlgorithm.Div_Selection(mutants);
                    }
                    if(DEBUG) {
                        System.out.println("Reduced Mutant Size: " + reducedMutants.size());
                    }
                    cnt2.addAndGet(reducedMutants.size());
                    for (int j = 0; j < reducedMutants.size(); j++) {
                        TypeWrapper newMutant = reducedMutants.get(j);
                        if (COMPILE) {
                            newMutant.rewriteJavaCode();  // 1. Rewrite transformation, don't remove this line, we need rewrite Java code twice
                            newMutant.resetClassName();  // 2. Rewrite class name and pkg definition
                            newMutant.removePackageDefinition();
                        }
                        newMutant.rewriteJavaCode();
                        if (newMutant.writeToJavaFile()) {
                            TypeWrapper.mutant2seed.put(newMutant.getFilePath(), newMutant.getInitSeedPath());
                            TypeWrapper.mutant2seq.put(newMutant.getFilePath(), newMutant.getTransSeq().toString());
                        }
                    }
                    wrappers.addAll(reducedMutants);
                }
            } else {
                wrappers.add(0, wrapper);
                break;
            }
        }
    }

    // Return value:
    //对某一个类别下面的所有positive的例子进行变异
    public static List<TypeWrapper> singleLevelExplorer(List<TypeWrapper> wrappers,int Limit, String outputPath) {  // Current depth means the depth of variants in wrappers, not the iteration level
        List<TypeWrapper> newWrappers = new ArrayList<>();
        while (!wrappers.isEmpty()) {
            if(Schedule.countFiles(outputPath)>Limit){
                return new ArrayList<>();
            }
            //做pop操作
            TypeWrapper wrapper = wrappers.get(0); // remove TypeWrapper in currentDepth level
            wrappers.remove(0);
            List<TypeWrapper> mutants = new ArrayList<>();
            if (GUIDED_LOCATION) {
                mutants = wrapper.TransformByGuidedLocation();//只是定位还没有写入
            } else if (RANDOM_LOCATION) {
                mutants = wrapper.TransformByRandomLocation();
            }
            if (DEBUG) {
                System.out.println("Src Path: " + wrapper.getFilePath());
                System.out.println("Mutant Size: " + mutants.size());
            }
            cnt1.addAndGet(mutants.size());
            List<TypeWrapper> reducedMutants = null;
            if (NO_SELECTION) {
                reducedMutants = mutants;
            }
            if (RANDOM_SELECTION) {
                reducedMutants = SelectionAlgorithm.Random_Selection(mutants);
            }
            if (DIV_SELECTION) {
                reducedMutants = SelectionAlgorithm.Div_Selection(mutants);//这个会在大类中选用几个变异来实施，还是算了
            }
            if(DEBUG) {
                System.out.println("Reduced Mutant Size: " + reducedMutants.size());
            }
            cnt2.addAndGet(reducedMutants.size());
            for (int j = 0; j < reducedMutants.size(); j++) {
                TypeWrapper newMutant = reducedMutants.get(j);
                if (COMPILE) {
                    newMutant.rewriteJavaCode();  // 1. Rewrite transformation, don't remove this line, we need rewrite Java code twice
                    newMutant.resetClassName();  // 2. Rewrite class name and pkg definition
                    newMutant.removePackageDefinition();
                }
                newMutant.rewriteJavaCode();
                if (newMutant.writeToJavaFile()) {
                    TypeWrapper.mutant2seed.put(newMutant.getFilePath(), newMutant.getInitSeedPath());
                    TypeWrapper.mutant2seq.put(newMutant.getFilePath(), newMutant.getTransSeq().toString());
                }
            }
            newWrappers.addAll(reducedMutants);
        }
        return newWrappers;
    }

    public static void singleLevelExplorer(ArrayDeque<TypeWrapper> wrappers, int currentDepth) {  // Current depth means the depth of variants in wrappers, not the iteration level
        while (!wrappers.isEmpty()) {
            TypeWrapper wrapper = wrappers.pollFirst(); // remove TypeWrapper in currentDepth level
            if (wrapper.depth < currentDepth) {
                if (!wrapper.isBuggy()) {
                    List<TypeWrapper> mutants = new ArrayList<>();
                    if (GUIDED_LOCATION) {
                        mutants = wrapper.TransformByGuidedLocation();
                    } else if (RANDOM_LOCATION) {
                        mutants = wrapper.TransformByRandomLocation();
                    }
                    if (DEBUG) {
                        System.out.println("Src Path: " + wrapper.getFilePath());
                        System.out.println("Mutant Size: " + mutants.size());
                    }
                    cnt1.addAndGet(mutants.size());
                    List<TypeWrapper> reducedMutants = null;
                    if (NO_SELECTION) {
                        reducedMutants = mutants;
                    }
                    if (RANDOM_SELECTION) {
                        reducedMutants = SelectionAlgorithm.Random_Selection(mutants);
                    }
                    if (DIV_SELECTION) {
                        reducedMutants = SelectionAlgorithm.Div_Selection(mutants);
                    }
                    if(DEBUG) {
                        System.out.println("Reduced Mutant Size: " + reducedMutants.size());
                    }
                    cnt2.addAndGet(reducedMutants.size());
                    for (int j = 0; j < reducedMutants.size(); j++) {
                        TypeWrapper newMutant = reducedMutants.get(j);
                        if (COMPILE) {
                            newMutant.rewriteJavaCode();  // 1. Rewrite transformation, don't remove this line, we need rewrite Java code twice
                            newMutant.resetClassName();  // 2. Rewrite class name and pkg definition
                            newMutant.removePackageDefinition();
                        }
                        newMutant.rewriteJavaCode();
                        if (newMutant.writeToJavaFile()) {
                            TypeWrapper.mutant2seed.put(newMutant.getFilePath(), newMutant.getInitSeedPath());
                            TypeWrapper.mutant2seq.put(newMutant.getFilePath(), newMutant.getTransSeq().toString());
                        }
                    }
                    wrappers.addAll(reducedMutants);
                }
            } else {
                wrappers.addFirst(wrapper);
                break;
            }
        }
    }

}
