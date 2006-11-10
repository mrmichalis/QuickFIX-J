package quickfix.mina.message;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import junit.framework.TestCase;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.FieldNotFound;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.MessageUtils;
import quickfix.SessionID;
import quickfix.Message.Header;
import quickfix.field.AvgPx;
import quickfix.field.BeginString;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.LeavesQty;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.field.SenderCompID;
import quickfix.field.Side;
import quickfix.field.TargetCompID;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.MessageFactory;

public class ChecksumTest extends TestCase {

    private DataDictionary dataDictionary;
    private MessageFactory messageFactory = new MessageFactory();
    private FIXMessageEncoder encoder;
    private FIXMessageDecoder decoder;
    private ProtocolEncoderOutputForTest encoderOutput;
    private ProtocolDecoderOutputForTest decoderOutput;

    public void setUp() throws ConfigError, UnsupportedEncodingException {
        dataDictionary = new DataDictionary("FIX44.xml");
        encoder = new FIXMessageEncoder("GBK");
        decoder = new FIXMessageDecoder("GBK");
        encoderOutput = new ProtocolEncoderOutputForTest();
        decoderOutput = new ProtocolDecoderOutputForTest();
    }

    // the encoder prevents the following two tests from working - it puts a
    // ? in place of the double byte.

    public void testDoubleByteWithCodec() throws Exception {
        ExecutionReport execReport = getExecutionReport();
        char db = 38738;
        String stringWithDoubleByte = "order" + db + " id";
        execReport.setString(OrderID.FIELD, stringWithDoubleByte);
        ByteBuffer buffer = encode(execReport);
        Message message = decode(buffer);
        assertEquals(stringWithDoubleByte, message.getString(OrderID.FIELD));
    }
    
    private void dumpBytes(byte[] data) throws UnsupportedEncodingException {
        for (int i = 0; i < data.length; i++) {
            System.out.println(i + ": " + (char) data[i] + "," + (data[i] & 0xff));
        }
        System.out.println(new String(data, "GBK"));
    }

    public void testDoubleByteWithDoubleByteCharset() throws ProtocolCodecException,
            InvalidMessage, FieldNotFound, UnsupportedEncodingException {
        //decoder.setCharsetName("GBK");
        //        encoder.setCharsetName("GBK");
        ExecutionReport execReport = getExecutionReport();
        char db = 38738;
        String stringWithDoubleByte = "order" + db + " id";
        execReport.setString(OrderID.FIELD, stringWithDoubleByte);
        ByteBuffer buffer = encode(execReport);
        Message message = decode(buffer);
        assertEquals(stringWithDoubleByte, message.getString(OrderID.FIELD));
    }

    public void testNullWithCodec() throws ProtocolCodecException, InvalidMessage, FieldNotFound {
        ExecutionReport execReport = getExecutionReport();
        String stringWithNull = "order" + "\0" + " id";
        execReport.setString(OrderID.FIELD, stringWithNull);
        ByteBuffer buffer = encode(execReport);
        Message message = decode(buffer);
        assertEquals(stringWithNull, message.getString(OrderID.FIELD));
    }

    /**
     * Ensure that the parse and alterChecksum methods that these tests
     * rely on work as anticipated for "standard" characters.
     * @throws ProtocolCodecException 
     */
    public void testSanityWithCodec() throws InvalidMessage, ProtocolCodecException {
        ExecutionReport execReport = getExecutionReport();
        ByteBuffer buffer = encode(execReport);
        Message message = decode(buffer);
        assertEquals(execReport.toString(), message.toString());
        buffer = alterChecksum(buffer);
        try {
            // check not ok after we break the checksum.
            message = decode(buffer);
            fail("Checksum should be incorrect in " + message.toString() + " from "
                    + buffer.getHexDump());
        } catch (InvalidMessage e) {
        }
    }

    public void testDecodeSanity() throws ProtocolCodecException, InvalidMessage {
        ByteBuffer buffer = getExecReportByteBuffer();
        // check unaltered message is ok
        Message message = decode(buffer);

        // now change some other char to get a checksum error.
        buffer.put(25, (byte) (buffer.get(25) + 1));
        try {
            // check not ok after we break the checksum.
            message = decode(buffer);
            fail("Bad checksum should have caused invalid message for " + message.toString()
                    + " from " + buffer.getHexDump());
        } catch (InvalidMessage e) {
        }

        // make sure fixChecksum works
        decoder = new FIXMessageDecoder();
        buffer = fixChecksum(buffer);
        decode(buffer);

    }

    public void testDecodeDoubleByte() throws Exception {
        //decoder.setCharsetName("GBK");
        boolean failed = false;
        for (int y = 0; y < 256; y++) {
            if (y == 1)
                y++;
            for (int x = 0; x < 256; x++) {
                if (x == 1)
                    x++;
                System.out.println("----");
                ByteBuffer buffer = getExecReportByteBuffer();
                buffer.put(25, (byte) x);
                buffer.put(26, (byte) y);
                buffer = fixChecksum(buffer);
                try {
                    decode(buffer);
                } catch (Exception e) {
                    failed = true;
                    System.err.println((char) buffer.get(25) + " " + (char) buffer.get(26));
                    System.err.println(x + ", " + y + " " + e);
                    e.printStackTrace();
                    fail("");

                }
            }
        }
        assertTrue(!failed);
    }

    private ByteBuffer fixChecksum(ByteBuffer buffer) {
        int checksum = checksum(buffer);
        ByteBuffer newBuffer = copyOfSlice(buffer);
        byte[] newChecksum = new DecimalFormat("000").format(checksum).getBytes();
        newBuffer.position(newBuffer.limit() - 4);
        newBuffer.put(newChecksum);
        newBuffer.rewind();
        return newBuffer;
    }

    private int checksum(ByteBuffer byteBuffer) {
        byteBuffer = byteBuffer.asReadOnlyBuffer();
        int checksum = 0;
        while (byteBuffer.remaining() > 7) {
            byte b = byteBuffer.get();
            System.out.println(">> "+b+" "+(((int)b) & 0XFF));
            checksum += ((int)b) & 0XFF;
        }
        System.out.println("checksum "+(checksum%256));
        return checksum % 256;
    }

    public ByteBuffer getExecReportByteBuffer() {
        // get a message to decode without relying on the encoder to create it
        // to be on the safe side.
        return ByteBuffer.wrap(new byte[] {
        // 8=FIX.4.4
                0x38, 0x3D, 0x46, 0x49, 0x58, 0x2E, 0x34, 0x2E, 0x34, 0x01,
                // 9=72
                0x39, 0x3D, 0x37, 0x32, 0x01,
                // 35=8
                0x33, 0x35, 0x3D, 0x38, 0x01,
                // 49=sender
                0x34, 0x39, 0x3D, 0x73, 0x65, 0x6E, 0x64, 0x65, 0x72, 0x01,
                // 56=target
                0x35, 0x36, 0x3D, 0x74, 0x61, 0x72, 0x67, 0x65, 0x74, 0x01,
                // 6=5
                0x36, 0x3D, 0x35, 0x01,
                // 14=0
                0x31, 0x34, 0x3D, 0x30, 0x01,
                // 17=abc
                0x31, 0x37, 0x3D, 0x61, 0x62, 0x63, 0x01,
                // 37=xyz
                0x33, 0x37, 0x3D, 0x78, 0x79, 0x7A, 0x01,
                // 39=0
                0x33, 0x39, 0x3D, 0x30, 0x01,
                // 54=1
                0x35, 0x34, 0x3D, 0x31, 0x01,
                // 150=0
                0x31, 0x35, 0x30, 0x3D, 0x30, 0x01,
                // 151=100
                0x31, 0x35, 0x31, 0x3D, 0x31, 0x30, 0x30, 0x01,
                // 10=178
                0x31, 0x30, 0x3D, 0x31, 0x37, 0x38, 0x01 });

    }

    /**
     * @param buffer a byteBuffer containing exactly 1 fix message
     * @return a byteBuffer same as above but with broken checksum.
     */
    private ByteBuffer alterChecksum(ByteBuffer buffer) {
        ByteBuffer newBuffer = copyOfSlice(buffer);

        newBuffer.position(newBuffer.limit() - 4);
        newBuffer.mark();
        byte firstChecksumByte = newBuffer.get();
        newBuffer.reset();
        newBuffer.put((byte) (firstChecksumByte == '1' ? '2' : '1'));
        newBuffer.rewind();
        assertFalse(buffer.slice().equals(newBuffer.slice()));
        return newBuffer;
    }

    /**
     * @param sourceBuffer a byteBuffer which will not be changed by the operation
     * @return a new ByteBuffer containing bytes from sourceBuffer.position -> sourceBuffer.limit.
     */
    private ByteBuffer copyOfSlice(ByteBuffer sourceBuffer) {
        sourceBuffer = sourceBuffer.asReadOnlyBuffer();
        ByteBuffer newBuffer = ByteBuffer.allocate(sourceBuffer.remaining());
        newBuffer.put(sourceBuffer);
        newBuffer.flip();
        return newBuffer.slice();
    }

    /**
     * Conver message into a ByteBuffer
     * @param message input message
     * @return ByteBuffer with position = 0, limit = end of fix message.
     * @throws ProtocolCodecException
     */
    private ByteBuffer encode(Message message) throws ProtocolCodecException {
        encoder.encode(null, message, encoderOutput);
        return encoderOutput.getBuffer();
    }

    /**
     * Convert a single message contained in a byte buffer into a Message.
     * @param buffer input buffer.  Message must start at position() and end at limit().  Buffer will not be modified.
     * @return decoded message
     * @throws ProtocolCodecException if couldn't convert from byte buffer to string
     * @throws InvalidMessage if resulting message is invalid.
     */
    private Message decode(ByteBuffer buffer) throws ProtocolCodecException, InvalidMessage {
        decoderOutput.reset();
        MessageDecoderResult result = decoder.decode(null, buffer.slice(), decoderOutput);
        if (!MessageDecoderResult.OK.equals(result)) {
            throw new ProtocolCodecException("Result: " + result.toString());
        }
        return parse(decoderOutput.getMessage());
    }

    public void testDoubleByteWithoutDecoder() throws InvalidMessage, FieldNotFound {
        ExecutionReport execReport = getExecutionReport();
        char db = 38738;
        String stringWithDoubleByte = "order" + db + " id";
        execReport.setString(OrderID.FIELD, stringWithDoubleByte);
        String messageData = execReport.toString();
        // check everything ok.
        Message message = parse(messageData);
        assertEquals(stringWithDoubleByte, message.getString(OrderID.FIELD));
    }

    public void testNullWithoutDecoder() throws InvalidMessage, FieldNotFound {
        ExecutionReport execReport = getExecutionReport();
        String stringWithNull = "order" + "\0" + " id";
        execReport.setString(OrderID.FIELD, stringWithNull);
        String messageData = execReport.toString();
        // check everything ok.
        Message message = parse(messageData);
        assertEquals(stringWithNull, message.getString(OrderID.FIELD));
    }

    /**
     * Ensure that the parse and alterChecksum methods that these tests
     * rely on work as anticipated for "standard" characters.
     */
    public void testSanityWithoutDecoder() throws InvalidMessage {
        ExecutionReport execReport = getExecutionReport();

        String messageData = execReport.toString();
        // check everything ok.
        parse(messageData);
        messageData = alterChecksum(messageData);
        try {
            // check not ok after we break the checksum.
            parse(messageData);
            fail("Checksum should be incorrect");
        } catch (InvalidMessage e) {
        }
    }

    private ExecutionReport getExecutionReport() {
        ExecutionReport execReport = new ExecutionReport(new OrderID("xyz"), new ExecID("abc"),
                new ExecType(ExecType.NEW), new OrdStatus(OrdStatus.NEW), new Side(Side.BUY),
                new LeavesQty(100.0), new CumQty(0.0), new AvgPx(5.0));
        SessionID sessionID = new SessionID("FIX.4.4", "sender", "target");
        setHeaderFields(execReport, sessionID);
        return execReport;
    }

    public String alterChecksum(String messageData) {
        String checksum = "\00110=";
        int offset = messageData.lastIndexOf(checksum);
        String newMessage = messageData.substring(0, offset) + checksum + "1"
                + messageData.substring(offset + checksum.length() + 1);
        if (newMessage.equals(messageData)) {
            newMessage = messageData.substring(0, offset) + checksum + "2"
                    + messageData.substring(offset + checksum.length() + 1);
        }
        return newMessage;
    }

    private Message parse(String messageData) throws InvalidMessage {
        return MessageUtils.parse(messageFactory, dataDictionary, messageData);
    }

    private void setHeaderFields(Message message, SessionID sessionID) {
        Header header = message.getHeader();
        header.setString(BeginString.FIELD, sessionID.getBeginString());
        header.setString(SenderCompID.FIELD, sessionID.getSenderCompID());
        header.setString(TargetCompID.FIELD, sessionID.getTargetCompID());
    }

    private final class ProtocolEncoderOutputForTest implements ProtocolEncoderOutput {
        private List buffers = new ArrayList();
        private ByteBuffer buffer;

        public void mergeAll() {
            ByteBuffer mergedBuffer = ByteBuffer.allocate(100);
            for (Iterator iter = buffers.iterator(); iter.hasNext();) {
                ByteBuffer buffer = (ByteBuffer) iter.next();
                mergedBuffer.put(buffer);
            }
            mergedBuffer.flip();
            buffers.clear();
            buffers.add(mergedBuffer);
        }

        public void write(ByteBuffer buf) {
            buffers.add(buf);
        }

        public WriteFuture flush() {
            throw new UnsupportedOperationException();
        }

        public ByteBuffer getBuffer() {
            if (buffers.isEmpty()) {
                return null;
            }
            return (ByteBuffer) buffers.get(0);
        }
    }

    private class ProtocolDecoderOutputForTest implements ProtocolDecoderOutput {
        public List messages = new ArrayList();

        public void write(Object message) {
            messages.add(message);
        }

        public int getMessageCount() {
            return messages.size();
        }

        public String getMessage() {
            if (messages.isEmpty()) {
                return null;
            }
            return getMessage(0);
        }

        public String getMessage(int n) {
            return (String) messages.get(n);
        }

        public void reset() {
            messages.clear();
        }

        public void flush() {

        }
    }
}