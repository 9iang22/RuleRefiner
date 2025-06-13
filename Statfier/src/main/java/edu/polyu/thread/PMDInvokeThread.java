package edu.polyu.thread;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static edu.polyu.util.Utility.REPORT_FOLDER;
import static edu.polyu.util.Utility.getFilePathsFromFolder;

public class PMDInvokeThread implements Runnable {

    private int iterDepth;
    private String seedFolderPath;
    private String seedFolderName;
    private List<String> ruleList;

    public PMDInvokeThread(int iterDepth, String seedFolderPath, String seedFolderName) {
        this.iterDepth = iterDepth;
        this.seedFolderPath = seedFolderPath; //seed路径 //eg:/home/wenge/SARR_ENV/Statfier/seeds/Semgrep_Seeds
        this.seedFolderName = seedFolderName; //seed文件名称 //eg:security_insecure-trust-manager@195
        String[] tokens = seedFolderName.split("_");
        this.ruleList = new ArrayList<> () {
            {
                add("category/java/" + tokens[0] + ".xml/" + tokens[1]);
            }
        };
    }

    // seedFolderPath can be java source file or a folder contains source files
    @Override
    public void run() {
        PMDConfiguration pmdConfig = new PMDConfiguration();
        pmdConfig.setInputPathList(getFilePathsFromFolder(seedFolderPath  + File.separator + seedFolderName));
        pmdConfig.setRuleSets(this.ruleList);
        pmdConfig.setReportFormat("json");
        pmdConfig.setReportFile(Paths.get(REPORT_FOLDER.getAbsolutePath()  + File.separator + "iter" + iterDepth + "_" + seedFolderName + "_Result.json"));
        pmdConfig.setIgnoreIncrementalAnalysis(true);
//        String[] pmdConfig = {
//                "-d", seedFolderPath  + File.separator + seedFolderName,
//                "-R", this.ruleList.get(0),
//                "-f", "json",
//                "-r", REPORT_FOLDER.getAbsolutePath()  + File.separator + "iter" + iterDepth + "_" + seedFolderName + "_Result.json"
//        };
        PMD.runPmd(pmdConfig);
    }

}
