package edu.polyu.util;

//import static edu.polyu.util.Invoker.compileJavaSourceFile;
//import static edu.polyu.util.Invoker.createSonarQubeProject;
//import static edu.polyu.util.Invoker.deleteSonarQubeProject;
import static edu.polyu.util.Invoker.failedCommands;
//import static edu.polyu.util.Invoker.invokeCheckStyle;
//import static edu.polyu.util.Invoker.invokeCommandsByZT;
//import static edu.polyu.util.Invoker.invokeCommandsByZTWithOutput;
//import static edu.polyu.util.Invoker.invokeInfer;
import static edu.polyu.util.Invoker.invokePMD;
//import static edu.polyu.util.Invoker.invokeSonarQube;
//import static edu.polyu.util.Invoker.invokeSpotBugs;
//import static edu.polyu.util.Invoker.writeSettingFile;
//import static edu.polyu.util.Utility.CHECKSTYLE_PATH;
import static edu.polyu.util.Utility.EVALUATION_PATH;
//import static edu.polyu.util.Utility.FINDSECBUGS_PATH;
import static edu.polyu.util.Utility.INFER_MUTATION;
//import static edu.polyu.util.Utility.INFER_PATH;
//import static edu.polyu.util.Utility.JAVAC_PATH;
//import static edu.polyu.util.Utility.Path2Last;
//import static edu.polyu.util.Utility.RESULT_FOLDER;
import static edu.polyu.util.Utility.SEARCH_DEPTH;
import static edu.polyu.util.Utility.DEBUG;
//import static edu.polyu.util.Utility.SEED_PATH;
//import static edu.polyu.util.Utility.SONARQUBE_PROJECT_NAME;
//import static edu.polyu.util.Utility.SONARSCANNER_PATH;
import static edu.polyu.util.Utility.SPOTBUGS_MUTATION;
//import static edu.polyu.util.Utility.SPOTBUGS_PATH;
//import static edu.polyu.util.Utility.SonarQubeRuleNames;
//import static edu.polyu.util.Utility.CLASS_FOLDER;
import static edu.polyu.util.Utility.compactIssues;
import static edu.polyu.util.Utility.failedReportPaths;
import static edu.polyu.util.Utility.failedT;
import static edu.polyu.util.Utility.failedToolExecution;
import static edu.polyu.util.Utility.file2row;
import static edu.polyu.util.Utility.getFilePathsFromFolder;
import static edu.polyu.util.Utility.getFilenamesFromFolder;
//import static edu.polyu.util.Utility.inferJarStr;
import static edu.polyu.util.Utility.MUTANT_FOLDER;
import static edu.polyu.util.Utility.reg_sep;
import static edu.polyu.util.Utility.REPORT_FOLDER;
import static edu.polyu.util.Utility.sep;
import static edu.polyu.util.Utility.subSeedFolderNameList;
import static edu.polyu.util.Utility.successfulT;
import static edu.polyu.util.Utility.waitTaskEnd;
import static edu.polyu.util.Utility.writeLinesToFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.polyu.analysis.TypeWrapper;
//import edu.polyu.report.CheckStyleReport;
//import edu.polyu.report.InferReport;
import edu.polyu.report.PMDReport;
//import edu.polyu.report.SonarQubeReport;
//import edu.polyu.report.SpotBugsReport;
import edu.polyu.transform.Transform;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import static edu.polyu.util.Utility.VARIABLE_LEVEL_MUTATOR;
import static edu.polyu.util.Utility.EXPRESSION_LEVEL_MUTATOR;
import static edu.polyu.util.Utility.STATEMENT_LEVEL_MUTATOR;
import static edu.polyu.util.Utility.CLASS_LEVEL_MUTATOR;
import static edu.polyu.util.Utility.MUTATED_RATE;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;


public class Schedule {

    private static final Schedule tester = new Schedule();

    private Schedule() {
    }

    private static String mutatorType;

    private static final Logger logger = Logger.getLogger(Schedule.class.getName());
    private static FileHandler fileHandler;
    private static void chooseMutatorType(){
        if(VARIABLE_LEVEL_MUTATOR){
            mutatorType="VariableLevelMutator";
        }
        if(EXPRESSION_LEVEL_MUTATOR){
            mutatorType="ExpressionLevelMutator";
        }
        if(STATEMENT_LEVEL_MUTATOR){
            mutatorType="StatementLevelMutator";
        }
        if(CLASS_LEVEL_MUTATOR){
            mutatorType="ClassLevelMutator";
        }
        //mutatorType="AllMutator";
    }

    static {
        try {
            chooseMutatorType();
            // 初始化日志文件（路径根据你的需求动态生成）
            String logPath = "/home/wenge/SARR_ENV/Statfier" + File.separator + mutatorType + "_out.log";
            fileHandler = new FileHandler(logPath, true); // true表示追加模式
            fileHandler.setFormatter(new SimpleFormatter()); // 设置简单文本格式
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "无法初始化日志文件", e);
        }
    }

    /**
     * 分析指定路径的代码文件并返回包含 ruleid 注释的下一行行号
     * @param filePath 代码文件的绝对路径
     * @return 包含 ruleid 注释的下一行行号列表
     * @throws IllegalArgumentException 如果 ruleid 和 ok 注释同时存在或同时不存在
     * @throws IOException 如果文件读取失败
     */
    public static List<Integer> findRuleIdLines(String filePath) throws IllegalArgumentException,IOException {
        List<String> codeLines = Files.readAllLines(Paths.get(filePath));
        List<Integer> ruleIdLineNumbers = new ArrayList<>();
        boolean hasRuleId = false;
        boolean hasOk = false;

        for (int i = 0; i < codeLines.size(); i++) {
            String line = codeLines.get(i).trim();

            // 检查 ruleid 注释
            if (line.contains("// ruleid:") || line.contains("/* ruleid:")||
                    line.contains("/*ruleid:")||line.contains("//ruleid:")||
                        line.contains("// deepruleid:") || line.contains("/* deepruleid:")||
                            line.contains("!-- ruleid:")){
                hasRuleId = true;
                // 下一行的行号是 i+2（因为行号从1开始）
                ruleIdLineNumbers.add(i + 2);
            }
            // 检查 ok 注释
            if (line.contains("// ok:") || line.contains("/* ok:")
                    ||line.contains("//ok:")||line.contains("/*ok:")||
                        line.contains("!-- ok:")) {
                hasOk = true;
            }
        }

        // 验证 ruleid 和 ok 注释的状态
        if ((hasRuleId && hasOk) || (!hasRuleId && !hasOk)) {
            throw new IllegalArgumentException(
                    "代码中必须包含 ruleid 或 ok 注释，但不能同时包含两者");
        }

        return ruleIdLineNumbers;
    }





    public static Schedule getInstance() {
        return tester;
    }

    /**
     * 统计指定路径下的所有文件数量（递归包含子目录）
     * @param directoryPath 目标目录路径
     * @return 文件总数，如果路径无效返回-1
     */
    public static long countFiles(String directoryPath) {
        try {
            final long[] count = {0};
            Files.walkFileTree(Paths.get(directoryPath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (Files.isRegularFile(file)) {
                        count[0]++;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    System.err.println("无法访问文件: " + file + " (" + exc.getMessage() + ")");
                    return FileVisitResult.CONTINUE;
                }
            });
            return count[0];
        } catch (InvalidPathException e) {
            System.err.println("无效路径: " + directoryPath);
            return -1;
        } catch (IOException e) {
            System.err.println("IO错误: " + e.getMessage());
            return -1;
        }
    }

    private HashMap<String, List<TypeWrapper>> bug2wrappers = new HashMap<>();

    public void executePMDTransform(String seedFolderPath) {
        //invokePMD(seedFolderPath);  //还是有用的，他会要初始化file2row和file2啥的两个filed量
        List<String> seedPaths = getFilenamesFromFolder(seedFolderPath, true);
        System.out.println("All Initial Seed Count: " + seedPaths.size());
        HashMap<String, HashSet<String>> category2bugTypes = new HashMap<>();

        String outLog = "/home/wenge/SARR_ENV/Statfier"+sep+mutatorType+"_out.log";
//        for(int index = 0; index < seedPaths.size(); index++){
//            String seedPath = seedPaths.get(index);
//            try {
//                List<Integer> ruleIdLines = findRuleIdLines(seedPath);
//                if (ruleIdLines.size() == 0) {
//                    continue;
//                }else{
//                    file2row.put(seedPath, ruleIdLines);
//                }
//            }catch (IllegalArgumentException  e){
//                System.err.println("IllegalArgumentException: " + seedPath);
//                return ;
//            }catch (IOException ioe){
//                System.err.println("IOException: " + seedPath);
//                return ;
//            }
//        }
        int initSeedWrapperSize = 0;
        for (int index = 0; index < seedPaths.size(); index++) {
            String seedPath = seedPaths.get(index);
//            if (!file2row.containsKey(seedPath)) {  //file2row中存的时有问题的行，一个list
//                //就是说如果某一个seedPath没有在file2row中出现过，那就说明这个seedPath是negative的，直接跳过不处理
//                continue;
//            }
            String[] tokens = seedPath.split(reg_sep);
            String seedFolderName = tokens[tokens.length - 2];//codestyle_ExtendsObject
            String category = seedFolderName.split("_")[0];
            String bugType = seedFolderName.split("_")[1];
            if (category2bugTypes.containsKey(category)) {
                category2bugTypes.get(category).add(bugType);
            } else {
                HashSet<String> types = new HashSet<>();
                types.add(bugType);
                category2bugTypes.put(category, types);
            }
            initSeedWrapperSize++;
            TypeWrapper seedWrapper = new TypeWrapper(seedPath, seedFolderName);
            if (bug2wrappers.containsKey(seedFolderName)) {
                bug2wrappers.get(seedFolderName).add(seedWrapper);
            } else {
                List<TypeWrapper> wrappers = new ArrayList<>();
                wrappers.add(seedWrapper);
                bug2wrappers.put(seedFolderName, wrappers);
            }
        }
        HashMap<String,Integer> neededNum = new HashMap<>();
        for(Map.Entry<String, List<TypeWrapper>> entry : bug2wrappers.entrySet()){
            String ruleCategory = entry.getKey();
            List<TypeWrapper> bugTypes = entry.getValue();
            neededNum.put(ruleCategory, bugTypes.size()*MUTATED_RATE);
        }

        //bug2wrappers中全是positive的case
        System.out.println("Initial Wrappers Size: " + initSeedWrapperSize);
        for (int depth = 1; depth <= SEARCH_DEPTH; depth++) {
            for (Map.Entry<String, HashSet<String>> entry : category2bugTypes.entrySet()) {//entry={"security":["HardCodedCryptoKey","InsercureCryptolv"]}

                String ruleCategory = entry.getKey();//ruleCategory="security"
                HashSet<String> bugTypes = entry.getValue();
                for (String bugType : bugTypes) {
                    while(true){
                        String seedFolderName = ruleCategory + "_" + bugType;
                        List<TypeWrapper> wrappers = new ArrayList<>() {
                            {
                                addAll(bug2wrappers.get(seedFolderName));
                            }
                        };//到这里wrappers得到了某一个规则下的所有testcase为positive的wrapper
                        if (wrappers.size() == 0) {
                            logger.info("No valid wrappers found for: " + seedFolderName);
                            break;
                        }
                        bug2wrappers.get(seedFolderName).clear();//从bug2wrappers中删除掉
                        if (DEBUG) {
                            System.out.println("Detection: " + seedFolderName);
                            System.out.println("Seed FolderName: " + seedFolderName + " Depth: " + depth + " Wrapper Size: " + wrappers.size());
                        }
                        String basePath="/home/wenge/SARR_ENV/Statfier/evaluation/mutants/iter1";
                        String mutatorOutputPath = basePath + sep + seedFolderName;
                        int targetPathFileNum= (int) countFiles(mutatorOutputPath);
                        int expectedNum=neededNum.get(seedFolderName);
                        //主要的逻辑放在了这里
                        List<TypeWrapper> newWrappers = Transform.singleLevelExplorer(wrappers,expectedNum,mutatorOutputPath);
                        if(newWrappers.size()==0){
                            logger.info("Empty newWrappers for: " + seedFolderName);
                            break;
                        }

                        List<TypeWrapper> validWrappers = new ArrayList<>();
                        for (int i = 0; i < newWrappers.size(); i++) {
                            TypeWrapper newWrapper = newWrappers.get(i);
                            if (!newWrapper.isBuggy()) {
                                validWrappers.add(newWrapper);
                            }
                        }
                        if (bug2wrappers.get(seedFolderName).size() > 0) {
                            System.err.println("Exception in Seed Size.");
                            System.exit(-1);
                        }
                        bug2wrappers.get(seedFolderName).addAll(validWrappers);
                    }
                }

            }
        }
    }



    public static Map<String, String> file2config = new HashMap<>();


    public static void writeEvaluationResult() {
        int rules = compactIssues.keySet().size();
        int seqCount = 0;
        int allValidVariantNumber = 0;
        for (Map.Entry<String, HashMap<String, List<TriTuple>>> entry : compactIssues.entrySet()) {
            String rule = entry.getKey();
            HashMap<String, List<TriTuple>> seq2mutants = entry.getValue();
            seqCount += seq2mutants.size();
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            root.put("Rule", rule);
            root.put("SeqSize", seq2mutants.size());
            ArrayNode bugs = mapper.createArrayNode();
            for (Map.Entry<String, List<TriTuple>> subEntry : seq2mutants.entrySet()) {
                ObjectNode bug = mapper.createObjectNode();
                bug.put("Transform_Sequence", subEntry.getKey());
                ArrayNode tuples = mapper.createArrayNode();
                for (TriTuple triTuple : subEntry.getValue()) {
                    ObjectNode tuple = mapper.createObjectNode();
                    tuple.put("Seed", triTuple.first);
                    tuple.put("Mutant", triTuple.second);
                    tuple.put("BugType", triTuple.third);
                    tuples.add(tuple);
                }
                bug.put("Bugs", tuples);
                bugs.add(bug);
                allValidVariantNumber += subEntry.getValue().size();
            }
            root.put("Results", bugs);
            File jsonFile = new File(Utility.EVALUATION_PATH + sep + "results" + sep + rule + ".json");
            try {
                if (!jsonFile.exists()) {
                    jsonFile.createNewFile();
                }
                FileWriter jsonWriter = new FileWriter(jsonFile);
                BufferedWriter jsonBufferedWriter = new BufferedWriter(jsonWriter);
                jsonBufferedWriter.write(root.toString());
                jsonBufferedWriter.close();
                jsonWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        List<String> output = new ArrayList<>();
        output.add("All Variants Size: " + Transform.cnt1);
        output.add("Reduced variants Size: " + Transform.cnt2);
        output.add("Reduction Ratio: " + Transform.cnt2.get() / (double) (Transform.cnt1.get()));
        output.add("Transformed Seeds: " + TypeWrapper.transformedSeed++);
        output.add("Successful Transform Size: " + successfulT);
        output.add("Failed Transform Size: " + failedT);
        output.add("Successful Transform Ratio: " + (successfulT) / (double) (successfulT + failedT));
        output.add("Rule Size: " + rules + "\n");
        output.add("Detected Rules: " + compactIssues.keySet());
        output.add("Unique Sequence: " + seqCount);
        output.add("Valid Mutant Size (Potential Bug): " + allValidVariantNumber);
        List<String> mutant2seed = new ArrayList<>();
        mutant2seed.add("Mutant2Seed:");
        for (Map.Entry<String, String> entry : TypeWrapper.mutant2seed.entrySet()) {
            mutant2seed.add(entry.getKey() + "->" + entry.getValue() + "#" + TypeWrapper.mutant2seq.get(entry.getKey()));
        }
        writeLinesToFile(EVALUATION_PATH + sep + "mutant2seed.log", mutant2seed);
        if (INFER_MUTATION) {
            writeLinesToFile(EVALUATION_PATH + sep + "FailedReports.log", failedReportPaths);
        }
        if (SPOTBUGS_MUTATION) {
            writeLinesToFile(EVALUATION_PATH + sep + "FailedCommands.log", failedCommands);
            writeLinesToFile(EVALUATION_PATH + sep + "FailedToolExecution.log", failedToolExecution);
        }
        long executionTime = System.currentTimeMillis() - Utility.startTimeStamp;
        output.add(String.format(
                "Overall Execution Time is: " + String.format("%d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes(executionTime),
                        TimeUnit.MILLISECONDS.toSeconds(executionTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(executionTime))) + "\n")
        );
        writeLinesToFile(EVALUATION_PATH + sep + "Output.log", output);
        if (TypeWrapper.failedParse.size() > 0) {
            writeLinesToFile(EVALUATION_PATH + sep + "FailedParse.log", TypeWrapper.failedParse);
        }
        if (PMDReport.errorReportPaths.size() > 0) {
            writeLinesToFile(EVALUATION_PATH + sep + "ErrorReportPaths.log", PMDReport.errorReportPaths);
        }
    }


}
