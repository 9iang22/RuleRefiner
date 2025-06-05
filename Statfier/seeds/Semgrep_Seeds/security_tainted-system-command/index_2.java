package org.sasanlabs.service.vulnerability.commandInjection;

import java.io.IOException;
import org.springframework.web.bind.annotation.RequestParam;

public class CommandInjection {

    public static void test5(@RequestParam String input) throws IOException {
        String test1 = "test";
        String comb = String.format("%s%s", test1, input);
        Runtime rt = Runtime.getRuntime();
        // ruleid: tainted-system-command
        Process exec = rt.exec(comb);
    }
}