package edu.polyu.thread;

import edu.polyu.analysis.TypeWrapper;
import edu.polyu.transform.Transform;
import edu.polyu.util.OSUtil;
import edu.polyu.util.Invoker;
import edu.polyu.util.Utility;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static edu.polyu.report.SpotBugsReport.readSpotBugsResultFile;
import static edu.polyu.util.Utility.SPOTBUGS_PATH;
import static edu.polyu.util.Utility.REPORT_FOLDER;

public class SpotBugsTransformThread implements Runnable {

    private int currentDepth;
    private ArrayDeque<TypeWrapper> wrappers;

    public SpotBugsTransformThread(List<TypeWrapper> initWrappers) {
        this.currentDepth = 0;
        this.wrappers = new ArrayDeque<>() {
            {
                addAll(initWrappers);
            }
        };
    }

    @Override
    public void run() {
        // initWrapper: -> iter1 mutants -> transform -> compile -> detect -> iter2 mutants...
        for (int depth = 1; depth <= Utility.SEARCH_DEPTH; depth++) {
            Transform.singleLevelExplorer(this.wrappers, this.currentDepth++);
            for (TypeWrapper wrapper : wrappers) {
                String seedFilePath = wrapper.getFilePath();
                String seedFolderPath = wrapper.getFolderPath();
                String[] tokens = seedFilePath.split(Utility.sep);
                String seedFileNameWithSuffix = tokens[tokens.length - 1];
                String subSeedFolderName = tokens[tokens.length - 2];
                String seedFileName = seedFileNameWithSuffix.substring(0, seedFileNameWithSuffix.length() - 5);
                File CLASS_FOLDER = new File(Utility.CLASS_FOLDER.getAbsolutePath() + File.separator + seedFileName);
                if (!CLASS_FOLDER.exists()) {
                    CLASS_FOLDER.mkdirs();
                }
                Invoker.compileJavaSourceFile(seedFolderPath, seedFileNameWithSuffix, CLASS_FOLDER.getAbsolutePath());
                String reportPath = REPORT_FOLDER.getAbsolutePath() + File.separator + subSeedFolderName + File.separator + seedFileName + "_Result.xml";
                String[] invokeCmds = new String[3];
                if (OSUtil.isWindows()) {
                    invokeCmds[0] = "cmd.exe";
                    invokeCmds[1] = "/c";
                } else {
                    invokeCmds[0] = "/bin/bash";
                    invokeCmds[1] = "-c";
                }
                invokeCmds[2] = SPOTBUGS_PATH + " -textui"
                        + " -xml:withMessages" + " -output " + reportPath + " "
                        + CLASS_FOLDER.getAbsolutePath();
                boolean hasExec = Invoker.invokeCommandsByZT(invokeCmds);
                if (hasExec) {
                    String report_path = REPORT_FOLDER.getAbsolutePath() + File.separator + subSeedFolderName + File.separator + seedFileName + "_Result.xml";
                    readSpotBugsResultFile(wrapper.getFolderPath(), report_path);
                }
            }
            List<TypeWrapper> validWrappers = new ArrayList<>();
            while (!wrappers.isEmpty()) {
                TypeWrapper head = wrappers.pollFirst();
                if (!head.isBuggy()) {
                    validWrappers.add(head);
                }
            }
            wrappers.addAll(validWrappers);
        }
    }
}
