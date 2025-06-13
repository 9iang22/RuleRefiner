package edu.polyu.thread;

import edu.polyu.analysis.TypeWrapper;
import edu.polyu.transform.Transform;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PMD;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static edu.polyu.report.PMDReport.readPMDResultFile;
import static edu.polyu.util.Utility.DEBUG;
import static edu.polyu.util.Utility.SEARCH_DEPTH;
import static edu.polyu.util.Utility.MUTANT_FOLDER;
import static edu.polyu.util.Utility.REPORT_FOLDER;
import static edu.polyu.util.Utility.getFilePathsFromFolder;

public class PMDTransformThread implements Runnable {

    private int currentDepth = 0;
    private String seedFolderName;
    private String ruleCategory;
    private String ruleType;
    private ArrayDeque<TypeWrapper> wrappers;

    public PMDTransformThread(List<TypeWrapper> initWrappers, String seedFolderName) {
        this.seedFolderName = seedFolderName;
        String[] tokens = seedFolderName.split("_");
        this.ruleCategory = tokens[0];
        this.ruleType = tokens[1];
        this.wrappers = new ArrayDeque<>() {
            {
                addAll(initWrappers);
            }
        };
    }

    // iter 1 -> SEARCH_DEPTH: 1. transformation to generate mutant; 2. invoke PMD to detect bugs
    @Override
    public void run() {
        for (int depth = 1; depth <= SEARCH_DEPTH; depth++) {
            if(DEBUG) {
                System.out.println("Seed FolderName: " + this.seedFolderName + " Depth: " + depth + " Wrapper Size: " + wrappers.size());
            }
            Transform.singleLevelExplorer(this.wrappers, this.currentDepth++);
            String resultFilePath = REPORT_FOLDER.getAbsolutePath() + File.separator + "iter" + depth + "_" + seedFolderName + "_Result.json";
            String mutantFolderPath = MUTANT_FOLDER + File.separator + "iter" + depth + File.separator + seedFolderName;
//            String[] pmdConfig = {
//                    "-d", mutantFolderPath,
//                    "-R", "category/java/" + ruleCategory + ".xml/" + ruleType,
//                    "-f", "json",
//                    "-r", resultFilePath,
////                    "-t", "4"
//            };
            PMDConfiguration pmdConfig = new PMDConfiguration();
            pmdConfig.setInputPathList(getFilePathsFromFolder(mutantFolderPath));
            pmdConfig.setRuleSets(new ArrayList<>() {
                {
                    add("category/java/" + ruleCategory + ".xml/" + ruleType);
                }
            });
            pmdConfig.setReportFormat("json");
            pmdConfig.setReportFile(Paths.get(resultFilePath));
            PMD.runPmd(pmdConfig); // detect mutants of level i
            readPMDResultFile(resultFilePath);
            List<TypeWrapper> validWrappers = new ArrayList<>();
            while (!wrappers.isEmpty()) {
                TypeWrapper head = wrappers.pollFirst();
                if (!head.isBuggy()) { // if this mutant is buggy, then we should switch to next mutant
                    validWrappers.add(head);
                }
            }
            wrappers.addAll(validWrappers);
        }
    }

}
