package quickfix.test.acceptance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;
import junit.framework.TestResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExpectMessageStep implements TestStep {
    public static long TIMEOUT_IN_MS = 10000;
    private Log log = LogFactory.getLog(getClass());
    private final String data;
    private final Map expectedFields;
    private static final Pattern headerPattern = Pattern.compile("^E(\\d+),.*");
    private static final Pattern fieldPattern = Pattern.compile("(\\d+)=([^\\001]+)\\001");
    private int clientId = 0;
    private static int heartBeatOverride = -1;

    static {
        String hbi = System.getProperty("at.heartbeat");
        if (hbi != null) {
            heartBeatOverride = Integer.parseInt(hbi);
        }
    }

    public ExpectMessageStep(String data) {
        this.data = data;
        Matcher headerMatcher = headerPattern.matcher(data);
        if (headerMatcher.matches()) {
            clientId = Integer.parseInt(headerMatcher.group(1));
        } else {
            clientId = 1;
        }
        expectedFields = simpleParse(data);
    }

    private Map simpleParse(String data) {
        HashMap fields = new HashMap();
        Matcher fieldMatcher = fieldPattern.matcher(data);
        while (fieldMatcher.find()) {
            fields.put(fieldMatcher.group(1), fieldMatcher.group(2));
        }
        return fields;
    }

    public void run(TestResult result, final TestContext context) throws InterruptedException {
        log.debug("expecting from client " + clientId + ": " + data + " " + expectedFields);
        CharSequence message = context.getNextMessage(clientId, TIMEOUT_IN_MS);
        if (message == null) {
            Assert.fail("message timeout: expected=" + expectedFields);
        }
        Map actualFields = simpleParse(message.toString());
        if (actualFields.size() == 0) {
            System.out.println("FOO!!!!!");
        }
        log.debug("actual: " + message);
        assertMessageEqual(actualFields);
    }

    private static HashSet timeFields = new HashSet();
    static {
        timeFields.add("52");
        timeFields.add("60");
        timeFields.add("122");
    }

    private void assertMessageEqual(Map actualFields) {
        Assert.assertEquals("wrong msg type", expectedFields.get("35"), actualFields.get("35"));
        Iterator fieldIterator = actualFields.entrySet().iterator();
        while (fieldIterator.hasNext()) {
            Map.Entry entry = (Map.Entry) fieldIterator.next();
            Object key = entry.getKey();
            if (timeFields.contains(key) || key.equals("10") || key.equals("9")) {
                continue;
            }
            if (expectedFields.get("108") != null && heartBeatOverride >= 0) {
                continue;
            }
            Assert.assertEquals("field " + key + " not equal: ", expectedFields.get(key), entry
                    .getValue());
        }
        Iterator expectedKey = expectedFields.keySet().iterator();
        while (expectedKey.hasNext()) {
            String key = (String) expectedKey.next();
            Assert.assertTrue("missing expected field: " + key, actualFields.containsKey(key));
        }
        Iterator timeFieldItr = timeFields.iterator();
        boolean dateLengthMismatch = false;
        while (timeFieldItr.hasNext()) {
            String key = (String) timeFieldItr.next();
            if (expectedFields.containsKey(key)) {
                if (((String) expectedFields
                        .get(key)).length() != ((String) actualFields.get(key)).length()) {
                    dateLengthMismatch = true;
                }
//                Assert.assertEquals("tag " + key + " size is wrong: ", ((String) expectedFields
//                        .get(key)).length(), ((String) actualFields.get(key)).length());
            }
        }
        if (expectedFields.get("9") != null && !dateLengthMismatch && heartBeatOverride < 0) {
            Assert.assertEquals("field 9 not equal: ", expectedFields.get("9"), actualFields.get("9"));
        }
    }

    public String toString() {
        return "expect message: " + data;
    }
}