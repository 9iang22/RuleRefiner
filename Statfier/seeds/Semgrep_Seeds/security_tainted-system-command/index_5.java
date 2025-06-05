package org.sasanlabs.service.vulnerability.commandInjection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.sasanlabs.internal.utility.LevelConstants;
import org.sasanlabs.internal.utility.Variant;
import org.sasanlabs.internal.utility.annotations.AttackVector;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRequestMapping;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRestController;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.bean.GenericVulnerabilityResponseBean;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
@VulnerableAppRestController(descriptionLabel = "COMMAND_INJECTION_VULNERABILITY", value = "CommandInjection")
public class CommandInjection {
    private static final String IP_ADDRESS = "ipaddress";
    private static final Pattern SEMICOLON_SPACE_LOGICAL_AND_PATTERN = Pattern.compile("[;& ]");
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\.|$)){4}\b");
    StringBuilder getResponseFromPingCommand(String ipAddress, boolean isValid) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        StringBuilder stringBuilder = new StringBuilder();
        if (isValid) {
            Process process;
            if (!isWindows) {
                // deepruleid: tainted-system-command
                process = new ProcessBuilder(new String[] {"sh", "-c", "ping -c 2 " + ipAddress}).redirectErrorStream(true).start();
            }
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                bufferedReader.lines().forEach(val -> stringBuilder.append(val).append("
"));
            }
        }
        return stringBuilder;
    }
    @AttackVector(vulnerabilityExposed = VulnerabilityType.COMMAND_INJECTION, description = "COMMAND_INJECTION_URL_PARAM_DIRECTLY_EXECUTED_IF_SEMICOLON_SPACE_LOGICAL_AND_NOT_PRESENT")
    @VulnerableAppRequestMapping(value = LevelConstants.LEVEL_2, htmlTemplate = "LEVEL_1/CI_Level1")
    public ResponseEntity<GenericVulnerabilityResponseBean<String>> getVulnerablePayloadLevel2(@RequestParam(IP_ADDRESS) String ipAddress, RequestEntity<String> requestEntity) throws ServiceApplicationException, IOException {
        Supplier<Boolean> validator = () -> StringUtils.isNotBlank(ipAddress) && !SEMICOLON_SPACE_LOGICAL_AND_PATTERN.matcher(requestEntity.getUrl().toString()).find();
        return new ResponseEntity<GenericVulnerabilityResponseBean<String>>(new GenericVulnerabilityResponseBean<String>(this.getResponseFromPingCommand(ipAddress, validator.get()).toString(), true), HttpStatus.OK);
    }
}