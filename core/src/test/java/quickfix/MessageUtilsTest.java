/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved. 
 * 
 * This file is part of the QuickFIX FIX Engine 
 * 
 * This file may be distributed under the terms of the quickfixengine.org 
 * license as defined by quickfixengine.org and appearing in the file 
 * LICENSE included in the packaging of this file. 
 * 
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING 
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A 
 * PARTICULAR PURPOSE. 
 * 
 * See http://www.quickfixengine.org/LICENSE for licensing information. 
 * 
 * Contact ask@quickfixengine.org if any conditions of this licensing 
 * are not clear to you.
 ******************************************************************************/

package quickfix;

import junit.framework.TestCase;
import quickfix.field.BeginString;
import quickfix.field.MsgType;
import quickfix.field.SenderCompID;
import quickfix.field.SenderLocationID;
import quickfix.field.SenderSubID;
import quickfix.field.TargetCompID;
import quickfix.field.TargetLocationID;
import quickfix.field.TargetSubID;
import quickfix.fix40.Logon;

public class MessageUtilsTest extends TestCase {
    public void testGetStringField() throws Exception {
        String messageString = "8=FIX.4.2\0019=12\00135=X\001108=30\00110=049\001";
        assertEquals("wrong value", "FIX.4.2", MessageUtils.getStringField(messageString,
                BeginString.FIELD));
        assertEquals("wrong value", "X", MessageUtils.getStringField(messageString, MsgType.FIELD));
        assertNull(messageString, MessageUtils.getStringField(messageString, SenderCompID.FIELD));
    }

    public void testSessionIdFromMessage() throws Exception {
        Message message = setUpMessageWithSessionID();
        SessionID sessionID = MessageUtils.getSessionID(message);
        assertEquals("TW", sessionID.getSenderCompID());
        assertEquals("SSUB", sessionID.getSenderSubID());
        assertEquals("SLOC", sessionID.getSenderLocationID());
        assertEquals("ISLD", sessionID.getTargetCompID());
        assertEquals("TSUB", sessionID.getTargetSubID());
        assertEquals("TLOC", sessionID.getTargetLocationID());
    }

    public void testExtendedSessionIdFromMessage() throws Exception {
        Message message = setUpMessageWithSessionID();
        SessionID sessionID = MessageUtils.getSessionID(message);
        assertEquals(sessionID.getBeginString(), "FIX.4.0");
        assertEquals("TW", sessionID.getSenderCompID());
        assertEquals("SSUB", sessionID.getSenderSubID());
        assertEquals("SLOC", sessionID.getSenderLocationID());
        assertEquals("ISLD", sessionID.getTargetCompID());
        assertEquals("TSUB", sessionID.getTargetSubID());
        assertEquals("TLOC", sessionID.getTargetLocationID());
    }

    private Message setUpMessageWithSessionID() {
        Message message = new Logon();
        message.getHeader().setString(SenderCompID.FIELD, "TW");
        message.getHeader().setString(SenderSubID.FIELD, "SSUB");
        message.getHeader().setString(SenderLocationID.FIELD, "SLOC");
        message.getHeader().setString(TargetCompID.FIELD, "ISLD");
        message.getHeader().setString(TargetSubID.FIELD, "TSUB");
        message.getHeader().setString(TargetLocationID.FIELD, "TLOC");
        return message;
    }

    public void testReverseSessionIdFromMessage() throws Exception {
        Message message = setUpMessageWithSessionID();
        SessionID sessionID = MessageUtils.getReverseSessionID(message);
        assertEquals(sessionID.getBeginString(), "FIX.4.0");
        assertEquals("ISLD", sessionID.getSenderCompID());
        assertEquals("TSUB", sessionID.getSenderSubID());
        assertEquals("TLOC", sessionID.getSenderLocationID());
        assertEquals("TW", sessionID.getTargetCompID());
        assertEquals("SSUB", sessionID.getTargetSubID());
        assertEquals("SLOC", sessionID.getTargetLocationID());
    }
    
    public void testReverseSessionIdFromMessageWithMissingFields() throws Exception {
        Message message = new Logon();
        SessionID sessionID = MessageUtils.getReverseSessionID(message);
        assertEquals(sessionID.getBeginString(), "FIX.4.0");
        assertEquals("", sessionID.getSenderCompID());
        assertEquals("", sessionID.getTargetCompID());
    }

    public void testSessionIdFromRawMessage() throws Exception {
        String messageString = "8=FIX.4.09=5635=A34=149=TW50=TWSUB142=TWLOC52=20060118-16:34:1956=ISLD98=0108=210=223";
        SessionID sessionID = MessageUtils.getSessionID(messageString);
        assertEquals(sessionID.getBeginString(), "FIX.4.0");
        assertEquals("TW", sessionID.getSenderCompID());
        assertEquals("TWSUB", sessionID.getSenderSubID());
        assertEquals("TWLOC", sessionID.getSenderLocationID());
        assertEquals("ISLD", sessionID.getTargetCompID());
    }

    public void testReverseSessionIdFromRawMessage() throws Exception {
        String messageString = "8=FIX.4.09=5635=A34=149=TW50=TWSUB142=TWLOC52=20060118-16:34:1956=ISLD98=0108=210=223";
        SessionID sessionID = MessageUtils.getReverseSessionID(messageString);
        assertEquals(sessionID.getBeginString(), "FIX.4.0");
        assertEquals("TW", sessionID.getTargetCompID());
        assertEquals("TWSUB", sessionID.getTargetSubID());
        assertEquals("TWLOC", sessionID.getTargetLocationID());
        assertEquals("ISLD", sessionID.getSenderCompID());
    }

    public void testMessageType() throws Exception {
        String messageString = "8=FIX.4.09=5635=A34=149=TW52=20060118-16:34:1956=ISLD98=0108=210=223";
        assertEquals("A", MessageUtils.getMessageType(messageString));
    }

    public void testMessageTypeError() throws Exception {
        String messageString = "8=FIX.4.09=5634=149=TW52=20060118-16:34:1956=ISLD98=0108=210=223";
        try {
            MessageUtils.getMessageType(messageString);
            fail("expected exception");
        } catch (InvalidMessage e) {
            // expected
        }
    }

    public void testMessageTypeError2() throws Exception {
        String messageString = "8=FIX.4.09=5635=1";
        try {
            MessageUtils.getMessageType(messageString);
            fail("expected exception");
        } catch (InvalidMessage e) {
            // expected
        }
    }

    public void testGetNonexistentStringField() throws Exception {
        String messageString = "8=FIX.4.09=5634=149=TW52=20060118-16:34:1956=ISLD98=0108=210=223";
        assertNull(MessageUtils.getStringField(messageString, 35));
    }

    public void testGetStringFieldWithBadValue() throws Exception {
        String messageString = "8=FIX.4.09=5634=149=TW52=20060118-16:34:1956=ISLD98=0108=210=223";
        assertNull(MessageUtils.getStringField(messageString, 10));
    }

    public void testParse() throws Exception {
        String messageString = "8=FIX.4.09=5635=A34=149=TW52=20060118-16:34:1956=ISLD98=0108=210=223";
        MessageUtils.parse(new DefaultMessageFactory(), null, messageString);
    }
}
