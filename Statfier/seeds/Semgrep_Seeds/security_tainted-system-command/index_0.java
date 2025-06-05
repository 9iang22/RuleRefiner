package org.sasanlabs.service.vulnerability.commandInjection;

import java.io.IOException;
import org.springframework.web.bind.annotation.RequestParam;

public class CommandInjection {

    public static void test1(@RequestParam("ipaddress") String ipAddress) throws IOException {
        String args = "ping -c 2 " + ipAddress + "test";
        Process process;
        // ruleid: tainted-system-command
        process = new ProcessBuilder(new String[] {"sh", "-c", args}).start();
    }
}