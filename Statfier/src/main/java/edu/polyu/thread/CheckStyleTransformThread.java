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

import static edu.polyu.report.CheckStyleReport.readCheckStyleResultFile;
import static edu.polyu.util.Utility.CHECKSTYLE_CONFIG_PATH;
import static edu.polyu.util.Utility.CHECKSTYLE_PATH;
import static edu.polyu.util.Utility.DEBUG;
import static edu.polyu.util.Utility.Path2Last;
import static edu.polyu.util.Utility.REPORT_FOLDER;

public class CheckStyleTransformThread implements Runnable {

    private int currentDepth;
    private String seedFolderName;
    private ArrayDeque<TypeWrapper> wrappers;
    private String configPath;

    public CheckStyleTransformThread(TypeWrapper initWrapper, String seedFolderName, String configPath) {
        this.currentDepth = 0;
        this.seedFolderName = seedFolderName;
        this.wrappers = new ArrayDeque<>() {
            {
                add(initWrapper);
            }
        };
        this.configPath = configPath;
    }

    @Override
    public void run() {
        for (int depth = 1; depth <= Utility.SEARCH_DEPTH; depth++) {
            if (DEBUG) {
                System.out.println("TransformThread Depth: " + depth + " Folder: " + this.seedFolderName);
            }
            Transform.singleLevelExplorer(this.wrappers, this.currentDepth++);
            String MUTANT_FOLDERPath = Utility.MUTANT_FOLDER + File.separator + "iter" + depth + File.separator + seedFolderName;
            List<String> mutantFilePaths = Utility.getFilenamesFromFolder(MUTANT_FOLDERPath, true);
            File configFile = new File(configPath);
            if (configFile.exists()) {
                configPath = CHECKSTYLE_CONFIG_PATH + File.separator + seedFolderName + 0 + ".xml";
            }
            for (int i = 0; i < mutantFilePaths.size(); i++) {
                String mutantFilePath = mutantFilePaths.get(i);
                String mutantFileName = Path2Last(mutantFilePath);
                String reportFilePath = REPORT_FOLDER + File.separator + "iter" + depth + "_" + mutantFileName + ".txt";
                String[] invokeCommands = new String[3];
                if (OSUtil.isWindows()) {
                    invokeCommands[0] = "cmd.exe";
                    invokeCommands[1] = "/c";
                } else {
                    invokeCommands[0] = "/bin/bash";
                    invokeCommands[1] = "-c";
                }
                invokeCommands[2] = "java -jar " + CHECKSTYLE_PATH + " -f" + " plain" + " -o " + reportFilePath + " -c "
                        + configPath + " " + mutantFilePath;
                if (DEBUG) {
                    System.out.println(invokeCommands[2]);
                }
                Invoker.invokeCommandsByZT(invokeCommands);
                readCheckStyleResultFile(reportFilePath);
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
