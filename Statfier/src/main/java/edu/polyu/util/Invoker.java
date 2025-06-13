package edu.polyu.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import edu.polyu.report.CheckStyleReport;
import edu.polyu.report.InferReport;
import edu.polyu.report.PMDReport;
import edu.polyu.report.Report;
import edu.polyu.report.SonarQubeReport;
import edu.polyu.report.SpotBugsReport;
import edu.polyu.thread.CheckStyleInvokeThread;
import edu.polyu.thread.InferInvokeThread;
import edu.polyu.thread.PMDInvokeThread;
import edu.polyu.thread.SpotBugsInvokeThread;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import org.json.JSONObject;
import org.zeroturnaround.exec.ProcessExecutor;

import static edu.polyu.util.Utility.EVALUATION_PATH;
import static edu.polyu.util.Utility.Path2Last;
import static edu.polyu.util.Utility.REPORT_FOLDER;
import static edu.polyu.util.Utility.RESULT_FOLDER;
import static edu.polyu.util.Utility.SEED_PATH;
import static edu.polyu.util.Utility.SONARQUBE_PROJECT_NAME;
import static edu.polyu.util.Utility.SONARSCANNER_PATH;
import static edu.polyu.util.Utility.SonarQubeRuleNames;
import static edu.polyu.util.Utility.getDirectFilenamesFromFolder;
import static edu.polyu.util.Utility.getFilePathsFromFolder;
import static edu.polyu.util.Utility.getFilenamesFromFolder;
import static edu.polyu.util.Utility.sep;
import static edu.polyu.util.Utility.spotBugsJarStr;
import static edu.polyu.util.Utility.waitTaskEnd;
import static edu.polyu.util.Utility.writeLinesToFile;

public class Invoker {

    public static List<String> failedCommands = new ArrayList<>();

    public static String invokeCommandsByZTWithOutput(String[] cmdArgs) {
        StringBuilder argStr = new StringBuilder();
        String output = "";
        for (String arg : cmdArgs) {
            argStr.append(arg + " ");
        }
        try {
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            output = new ProcessExecutor().command(cmdArgs).redirectError(errorStream).readOutput(true).execute().outputUTF8();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return output;
    }

    public static boolean invokeCommandsByZT(String[] cmdArgs) {
        StringBuilder argStr = new StringBuilder();
        for (String arg : cmdArgs) {
            argStr.append(arg + " ");
        }
        try {
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            int exitValue = new ProcessExecutor().command(cmdArgs).redirectError(errorStream).execute().getExitValue();
            if (Utility.CHECKSTYLE_MUTATION) {
                String errorInfo = new String(errorStream.toByteArray());
                if (argStr.toString().contains("javac")) {
                    if (exitValue != 0) {
                        failedCommands.add(argStr.toString());
                        return false;
                    }
                }
                if (argStr.toString().contains("/bin/bash")) {
                    if (exitValue == 254) {
                        if (Utility.DEBUG) {
                            System.out.println(errorInfo);
                        }
                    }
                    if (errorInfo.contains("Exception")) {
                        failedCommands.add(argStr.toString());
                        return false;
                    }
                }
            }
            if (false && Utility.PMD_MUTATION && exitValue != 4 && exitValue != 0) {
                System.out.println("Execute PMD Error!");
                System.out.println(argStr);
                return false;
            }
            if (Utility.SPOTBUGS_MUTATION && exitValue != 0) {
                failedCommands.add(argStr.toString());
                System.out.println("Invoke SpotBugs Error Value: " + exitValue);
                System.out.println(argStr);
                System.out.println("Error Msg: " + new String(errorStream.toByteArray()));
                return false;
            }
            if (Utility.INFER_MUTATION && exitValue != 0) {
                System.out.println("Invoke Infer Error Value: " + exitValue);
                System.out.println(argStr);
                System.out.println("Error Msg: " + new String(errorStream.toByteArray()));
                return false;
            }
            if (Utility.SONARQUBE_MUTATION && exitValue != 0) {
                System.err.println("Invoke SonarQube Error and Return Value: " + exitValue);
                System.err.println(argStr);
                System.err.println("Error Msg: " + new String(errorStream.toByteArray()));
                System.exit(-1);
                return false;
            }
            if (Utility.FINDSECBUGS_MUTATION && exitValue != 0) {
                failedCommands.add(argStr.toString());
                System.out.println("Invoke FindSecBugs Error Value: " + exitValue);
                System.out.println(argStr);
                System.out.println("Error Msg: " + new String(errorStream.toByteArray()));
                return false;
            }
        } catch (Exception e) {
            System.err.println(argStr);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean invokeCommands(String[] cmdArgs) {
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < cmdArgs.length; i++) {
            args.append(cmdArgs[i] + " ");
        }
        InputStream in;
        try {
            Process process = Runtime.getRuntime().exec(cmdArgs);
            in = process.getInputStream();
            BufferedReader read = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = read.readLine()) != null) {
                System.out.println(line);
                builder.append(line);
            }
            int exitValue = process.waitFor();
            if (exitValue != 4 && exitValue != 0) {
                if (Utility.SPOTBUGS_MUTATION) {
                    failedCommands.add(args.toString());
                } else {
                    System.err.println("Fail to Invoke Commands1: " + args);
                    System.err.println("Log: " + builder);
                    System.err.println("ExitValue: " + exitValue);
                    System.exit(-1);
                }
                return false;
            }
            process.getOutputStream().close();
        } catch (IOException e) {
            System.err.println("Fail to Invoke Commands2: " + args);
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            System.err.println("Fail to Invoke Commands:3 " + args);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean compileJavaSourceFile(String srcFolderPath, String fileName, String classFileFolder) {
        if (Utility.DEBUG) {
            System.out.println("Compiling: " + fileName);
        }
        if (!fileName.endsWith(".java")) {
            System.err.println("File: " + fileName + " is not ended by .java");
            System.exit(-1);
        }
        fileName = fileName.substring(0, fileName.length() - 5);
        List<String> cmd_list = new ArrayList<>();
        cmd_list.add(Utility.JAVAC_PATH);
        cmd_list.add("-d");
        cmd_list.add(classFileFolder);
        cmd_list.add("-cp");
        if (Utility.SPOTBUGS_MUTATION) {
            cmd_list.add(Utility.spotBugsJarStr.toString());
        }
        if (Utility.INFER_MUTATION) {
            cmd_list.add(Utility.inferJarStr.toString());
        }
        if (Utility.FINDSECBUGS_MUTATION) {
            cmd_list.add(Utility.findSecBugsJarStr.toString());
        }
        cmd_list.add(srcFolderPath + File.separator + fileName + ".java");
        boolean tag = invokeCommandsByZT(cmd_list.toArray(new String[cmd_list.size()]));
        return tag;
    }

    public static void invokeSpotBugs(String seedFolderPath) {
        if (seedFolderPath.endsWith(sep)) {
            seedFolderPath = seedFolderPath.substring(0, seedFolderPath.length() - 1);
        }
        if (Utility.DEBUG) {
            System.out.println("Invoke SpotBugs Path: " + seedFolderPath);
        }
        ExecutorService threadPool = Utility.initThreadPool();
        for (int i = 0; i < Utility.subSeedFolderNameList.size(); i++) {
            String subSeedFolderName = Utility.subSeedFolderNameList.get(i);
            String subSeedFolderPath = seedFolderPath + File.separator + subSeedFolderName;
            List<String> seedFileNames = getFilenamesFromFolder(subSeedFolderPath, false);
            threadPool.submit(new SpotBugsInvokeThread(subSeedFolderPath, subSeedFolderName, seedFileNames));
        }
        Utility.waitThreadPoolEnding(threadPool);
        for (String subSeedFolderName : Utility.subSeedFolderNameList) {
            List<String> reportPaths = getFilenamesFromFolder(REPORT_FOLDER.getAbsolutePath() + File.separator + subSeedFolderName, true);
            for (String reportPath : reportPaths) {
                SpotBugsReport.readSpotBugsResultFile(seedFolderPath + File.separator + subSeedFolderName, reportPath);
            }
        }
    }

    public static void writeSettingFile(String seedFolderPath, String settingFilePath, String projectName) {
        List<String> contents = new ArrayList<>();
        contents.add("sonar.projectKey=" + projectName);
        contents.add("sonar.projectName=" + projectName);
        contents.add("sonar.projectVersion=1.0");
        contents.add("sonar.login=admin");
        contents.add("sonar.password=123456");
        contents.add("sonar.sourceEncoding=UTF-8");
        contents.add("sonar.scm.disabled=true");
        contents.add("sonar.cpd.exclusions=**/*");
        contents.add("sonar.sources=" + seedFolderPath);
        contents.add("sonar.java.source=11");
        File dummyFolder = new File(seedFolderPath + sep + "dummy-binaries");
        if (!dummyFolder.exists()) {
            dummyFolder.mkdir();
        }
        File dummyFile = new File(seedFolderPath + sep + "dummy-binaries" + sep + "dummy.txt");
        if (!dummyFile.exists()) {
            try {
                dummyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        contents.add("sonar.java.binaries=" + dummyFolder.getAbsolutePath());
        contents.add("sonar.java.test.binaries=" + dummyFolder.getAbsolutePath());
        File settingFile = new File(settingFilePath);
        if (!settingFile.exists()) {
            writeLinesToFile(settingFilePath, contents);
        }
    }

    public static void deleteSonarQubeProject(String projectName) {
        String[] curlPostCommands = new String[6];
        curlPostCommands[0] = "curl";
        curlPostCommands[1] = "-u";
        curlPostCommands[2] = "admin:123456";
        curlPostCommands[3] = "-X";
        curlPostCommands[4] = "POST";
        curlPostCommands[5] = "http://localhost:9000/api/projects/delete?project=" + projectName;
        invokeCommandsByZT(curlPostCommands);
    }

    public static boolean createSonarQubeProject(String projectName) {
        String[] curlPostCommands = new String[6];
        curlPostCommands[0] = "curl";
        curlPostCommands[1] = "-u";
        curlPostCommands[2] = "admin:123456";
        curlPostCommands[3] = "-X";
        curlPostCommands[4] = "POST";
        curlPostCommands[5] = "http://localhost:9000/api/projects/create?name=" + projectName + "&project=" + projectName;
        return invokeCommandsByZT(curlPostCommands);
    }

    public static void invokeSonarQube(String seedFolderPath) {
        for (int i = 0; i < Utility.subSeedFolderNameList.size(); i++) {
            String subSeedFolderName = Utility.subSeedFolderNameList.get(i);
            String subSeedFolderPath = seedFolderPath + sep + subSeedFolderName;
            File reportFolder = new File(REPORT_FOLDER.getAbsolutePath() + sep + subSeedFolderName);
            if(!reportFolder.exists()) {
                reportFolder.mkdir();
            }
            List<String> seedPaths = getFilenamesFromFolder(subSeedFolderPath, true);
            String[] invokeCommands = new String[3];
            invokeCommands[0] = "/bin/bash";
            invokeCommands[1] = "-c";
            String[] curlCommands = new String[4];
            curlCommands[0] = "curl";
            curlCommands[1] = "-u";
            curlCommands[2] = "admin:123456";
            for(String seedPath : seedPaths) {
                if (Utility.DEBUG) {
                    System.out.println("Analyzed Seed path: " + subSeedFolderPath);
                }
                deleteSonarQubeProject(SONARQUBE_PROJECT_NAME);
                if (!createSonarQubeProject(SONARQUBE_PROJECT_NAME)) {
                    System.out.println("Project Name: " + SONARQUBE_PROJECT_NAME + " is not created!");
                    System.out.println("Seed File is not Detected: " + seedPath);
                    System.exit(-1);
                }
                invokeCommands[2] = SONARSCANNER_PATH + " -Dsonar.projectKey=" + SONARQUBE_PROJECT_NAME
                        + " -Dsonar.projectBaseDir=" + SEED_PATH
                        + " -Dsonar.sources=" + seedPath
                        + " -Dsonar.host.url=http://localhost:9000"
                        + " -Dsonar.login=admin -Dsonar.password=123456";
                boolean hasExec = invokeCommandsByZT(invokeCommands); // invoke SonarQube to detect target file
                if (hasExec) {
                    waitTaskEnd(SONARQUBE_PROJECT_NAME);
                } else {
                    return;
                }
                curlCommands[3] = "http://localhost:9000/api/issues/search?p=1&ps=500&componentKeys=" + SONARQUBE_PROJECT_NAME;
                String jsonContent = invokeCommandsByZTWithOutput(curlCommands);
                writeLinesToFile(reportFolder.getAbsolutePath() + sep + Path2Last(seedPath) + ".json", jsonContent);
                SonarQubeReport.readSonarQubeResultFile(seedPath, jsonContent);
                JSONObject root = new JSONObject(jsonContent);
                int total = root.getInt("total");
                int count = total % 500 == 0 ? total / 500 : total / 500 + 1;
                for (int p = 2; p <= count; p++) {
                    curlCommands[3] = "http://localhost:9000/api/issues/search?p=" + p + "&ps=500&componentKeys=" + SONARQUBE_PROJECT_NAME;
                    jsonContent = invokeCommandsByZTWithOutput(curlCommands);
                    SonarQubeReport.readSonarQubeResultFile(seedPath, jsonContent);
                }
            }
        }
    }

    public static void invokeInfer(String seedFolderPath) {
        ExecutorService threadPool = Utility.initThreadPool();
        for (int i = 0; i < Utility.subSeedFolderNameList.size(); i++) {
            threadPool.submit(new InferInvokeThread(0, seedFolderPath, Utility.subSeedFolderNameList.get(i)));
        }
        Utility.waitThreadPoolEnding(threadPool);
        System.out.println("Infer Result Folder: " + REPORT_FOLDER.getAbsolutePath());
        List<String> seedPaths = getFilenamesFromFolder(seedFolderPath, true);
        for (String seedPath : seedPaths) {
            String reportPath = REPORT_FOLDER.getAbsolutePath() + File.separator + "iter0_" + Utility.Path2Last(seedPath) + File.separator + "report.json";
            InferReport.readSingleInferResultFile(seedPath, reportPath);
        }
    }

    public static void invokeCheckStyle(String seedFolderPath) {
        ExecutorService threadPool = Utility.initThreadPool();
        for (int i = 0; i < Utility.subSeedFolderNameList.size(); i++) {
            threadPool.submit(new CheckStyleInvokeThread(0, seedFolderPath, Utility.subSeedFolderNameList.get(i)));
        }
        Utility.waitThreadPoolEnding(threadPool);
        List<String> reportPaths = getFilenamesFromFolder(REPORT_FOLDER.getAbsolutePath(), true);
        for (int i = 0; i < reportPaths.size(); i++) {
            CheckStyleReport.readCheckStyleResultFile(reportPaths.get(i));
        }
    }

    public static void invokePMD(String seedFolderPath) {
        if (Utility.THREAD_COUNT > 1) {
            ExecutorService threadPool = Utility.initThreadPool();//创建了一个线程池
            for (int i = 0; i < Utility.subSeedFolderNameList.size(); i++) {
                threadPool.submit(new PMDInvokeThread(0, seedFolderPath, Utility.subSeedFolderNameList.get(i)));
            }
            Utility.waitThreadPoolEnding(threadPool);
            //跑完后所有的结果会生成在REPORT_FOLDER下，然后这里就是去读取所有的结果，然后这里就是完善file2row和
            for (int i = 0; i < Utility.subSeedFolderNameList.size(); i++) {
                PMDReport.readPMDResultFile(REPORT_FOLDER.getAbsolutePath() + File.separator + "iter0_" + Utility.subSeedFolderNameList.get(i) + "_Result.json");
            }
        } else {
            for (int i = 0; i < Utility.subSeedFolderNameList.size(); i++) {
                String seedFolderName = Utility.subSeedFolderNameList.get(i);
                String[] tokens = seedFolderName.split("_");
                PMDConfiguration pmdConfig = new PMDConfiguration();
                pmdConfig.setInputPathList(Utility.getFilePathsFromFolder(seedFolderPath + File.separator + seedFolderName));
                pmdConfig.setRuleSets(new ArrayList<>() {
                    {
                        add("category/java/" + tokens[0] + ".xml/" + tokens[1]);
                    }
                });
                pmdConfig.setReportFormat("json");
                pmdConfig.setReportFile(Paths.get(REPORT_FOLDER.getAbsolutePath() + File.separator + "iter" + 0 + "_" + seedFolderName + "_Result.json"));
                pmdConfig.setIgnoreIncrementalAnalysis(true);
                PMD.runPmd(pmdConfig);
            }
            for (int i = 0; i < Utility.subSeedFolderNameList.size(); i++) {
                PMDReport.readPMDResultFile(REPORT_FOLDER.getAbsolutePath() + File.separator + "iter0_" + Utility.subSeedFolderNameList.get(i) + "_Result.json");
            }
        }
    }

}
