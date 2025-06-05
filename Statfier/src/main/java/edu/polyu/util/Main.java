package edu.polyu.util;

//import edu.polyu.util.Utility;
import edu.polyu.util.Schedule;
//import edu.polyu.util.Schedule;

public class Main {
    public static void main(String[] args) {
        Utility.initEnv();
        Schedule schedule = Schedule.getInstance();
        if(Utility.PMD_MUTATION) {
            schedule.executePMDTransform(Utility.SEED_PATH);
            System.out.println("Done!!!");
        }

//        String path="/home/wenge/SARR_ENV/Statfier/evaluation/mutants/iter1/security_no-string-eqeq";
//        long n = Schedule.countFiles(path);
//        System.out.println("Total number of files: " + n);
    }
}
