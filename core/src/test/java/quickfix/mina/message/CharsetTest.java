package quickfix.mina.message;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.mina.message.FIXMessageDecoder;
import quickfix.mina.message.FIXMessageEncoder;

public class CharsetTest extends TestCase {

    private static final String MULTIBYTE_ENCODING = "GBK";

    public void testConversion() throws UnsupportedEncodingException {
        byte[] data = new byte[]{ /* 38738 */(byte)199, (byte)224};
        String s = new String(data, MULTIBYTE_ENCODING);
        assertEquals(1, s.length());
        assertEquals(38738, (int)s.charAt(0));

        byte[] decodedData = s.getBytes(MULTIBYTE_ENCODING);
        assertEquals(199, unsignedByte(decodedData[0]));
        assertEquals(224, unsignedByte(decodedData[1]));
    }

    public void testCodec() throws UnsupportedEncodingException, ProtocolCodecException, InvalidMessage {
        byte[] data = ("8=FIX.4.2\0019=5\0011="+(char)38738+"\00110=012\001").getBytes(MULTIBYTE_ENCODING);
        assertEquals(199, unsignedByte(data[16]));
        assertEquals(224, unsignedByte(data[17]));
        FIXMessageDecoder decoder = new FIXMessageDecoder(MULTIBYTE_ENCODING);
        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.put(data);
        buffer.flip();
        ProtocolDecoderOutputForTest decoderOutput = new ProtocolDecoderOutputForTest();
        decoder.decode(null, buffer, decoderOutput);
        assertNotNull(decoderOutput.message);
        assertEquals(38738, (int)decoderOutput.message.charAt(16));
        assertEquals(26, byteLength(decoderOutput.message, MULTIBYTE_ENCODING));
        assertEquals(225, checksum(decoderOutput.message, MULTIBYTE_ENCODING));
        
        Message fixMessage = new Message(decoderOutput.message, null, false, Charset.forName("GBK"));
        String fixMessageString = fixMessage.toString();
        System.out.println(fixMessageString);
        assertEquals(26, byteLength(fixMessageString, MULTIBYTE_ENCODING));
        assertEquals(225, checksum(fixMessageString, MULTIBYTE_ENCODING));
        
        FIXMessageEncoder encoder = new FIXMessageEncoder(MULTIBYTE_ENCODING);
        ProtocolEncoderOutputForTest encoderOutput = new ProtocolEncoderOutputForTest();
        encoder.encode(null, decoderOutput.message, encoderOutput);
        
        assertNotNull(encoderOutput.buffer);
        assertEquals(199, unsignedByte(encoderOutput.buffer.get(16)));
        assertEquals(224, unsignedByte(encoderOutput.buffer.get(17)));
    }
    
    public void testChecksum() throws UnsupportedEncodingException {
        byte[] data = ("  "+(char)38738+"10=000\001").getBytes(MULTIBYTE_ENCODING);
        assertEquals(199, unsignedByte(data[2]));
        assertEquals(224, unsignedByte(data[3]));
        assertEquals(231, checksum(new String(data, MULTIBYTE_ENCODING), MULTIBYTE_ENCODING));
    }
    
    private int unsignedByte(int i) {
        return i & 0xFF;
    }
    
    private int byteLength(String s, String charsetName) throws UnsupportedEncodingException {
        byte[] data = s.getBytes(charsetName);
        return data.length;
    }

    private int checksum(String s, String charsetName) throws UnsupportedEncodingException {
        byte[] data = s.getBytes(charsetName);
        int sum = 0;
        int end = data.length - 7;
        for (int i = 0; i < end; i++) {
            System.out.println(i+" "+unsignedByte(data[i])+" "+(char)data[i]+" "+sum);
            sum += unsignedByte(data[i]);
        }
        return sum % 256;
    }
    
    private final class ProtocolEncoderOutputForTest implements ProtocolEncoderOutput {
        public ByteBuffer buffer;
        
        public void write(ByteBuffer buf) {
            buffer = buf;
        }

        public void mergeAll() {
        }

        public WriteFuture flush() {
            return null;
        }
    }

    private final class ProtocolDecoderOutputForTest implements ProtocolDecoderOutput {
        public String message;
        
        public void write(Object message) {
            this.message = (String)message;
        }

        public void flush() {
        
        }
    }
}
