package quickfix.test.acceptance;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;
import junit.framework.TestResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpectDisconnectStep implements TestStep {
    private Logger log = LoggerFactory.getLogger(getClass());
    private static final Pattern DISCONNECT_PATTERN = Pattern.compile("e(\\d+)*,?DISCONNECT");
    private int clientId = 0;
    private String command;

    public ExpectDisconnectStep(String data) {
        this.command = data;
    }
    
    public void run(TestResult result, TestConnection connection) throws Exception {
        Matcher matcher = DISCONNECT_PATTERN.matcher(command);
        if (matcher.lookingAt()) {
            if (matcher.group(1) != null) {
                clientId = Integer.parseInt(matcher.group(1));
            } else {
                clientId = 1;
            }
        } else {
            Assert.fail("incorrect disconnect command: "+command);
        }
        log.debug("expecting disconnect from client "+clientId);
        connection.waitForClientDisconnect(clientId);
    }
    
    public String toString() {
        return "disconnect from server: "+command;
    }

}
