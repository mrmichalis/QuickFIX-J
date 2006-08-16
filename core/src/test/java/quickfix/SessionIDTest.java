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

public class SessionIDTest extends TestCase {
    public void testFromString() throws Exception {
        SessionID sessionID = new SessionID(null, null, null);
        sessionID.fromString("FIX.4.2:SENDER->TARGET:QUALIFIER");
        assertEquals("FIX.4.2", sessionID.getBeginString());
        assertEquals("SENDER", sessionID.getSenderCompID());
        assertEquals("TARGET", sessionID.getTargetCompID());
        assertEquals("QUALIFIER", sessionID.getSessionQualifier());
    }

    public void testFromStringNoQualifier() throws Exception {
        SessionID sessionID = new SessionID(null, null, null);
        sessionID.fromString("FIX.4.2:SENDER->TARGET");
        assertEquals("FIX.4.2", sessionID.getBeginString());
        assertEquals("SENDER", sessionID.getSenderCompID());
        assertEquals("TARGET", sessionID.getTargetCompID());
        assertEquals("", sessionID.getSessionQualifier());
    }

    public void testFromStringWithSubIdsAndLocations() throws Exception {
        SessionID sessionID = new SessionID("FIX.4.2:SENDER/SUB->TARGET/TSUB/LOCATION");
        assertEquals("FIX.4.2", sessionID.getBeginString());
        assertEquals("SENDER", sessionID.getSenderCompID());
        assertEquals("SUB", sessionID.getSenderSubID());
        assertEquals("", sessionID.getSenderLocationID());
        assertEquals("TARGET", sessionID.getTargetCompID());
        assertEquals("TSUB", sessionID.getTargetSubID());
        assertEquals("LOCATION", sessionID.getTargetLocationID());
    }
    
    public void testFromEqualsAndHashcode() throws Exception {
        SessionID sessionID1 = new SessionID("FIX.4.2:SENDER/SUB->TARGET/TSUB/LOCATION");
        SessionID sessionID2 = new SessionID("FIX.4.2:SENDER/SUB->TARGET/TSUB/LOCATION");
        SessionID sessionID3 = new SessionID("FIX.4.2:X/SUB/->Y//:Q");
        assertTrue(sessionID1.equals(sessionID2) && sessionID2.equals(sessionID1));
        assertTrue(sessionID1.hashCode() == sessionID2.hashCode());
        assertTrue(!sessionID1.equals(sessionID3) && !sessionID3.equals(sessionID1));
    }

    public void testFromStringError1() throws Exception {
        SessionID sessionID = new SessionID(null, null, null);
        try {
            sessionID.fromString("FIX.4.2@SENDER->TARGET");
            fail("no exception");
        } catch (RuntimeError e) {
            // expected
        }
    }

    public void testFromStringError2() throws Exception {
        SessionID sessionID = new SessionID(null, null, null);
        try {
            sessionID.fromString("FIX.4.2:SENDER=>TARGET");
            fail("no exception");
        } catch (RuntimeError e) {
            // expected
        }
    }

    public void testFromStringEmptyQualifier() throws Exception {
        SessionID sessionID = new SessionID(null, null, null);
        sessionID.fromString("FIX.4.2:SENDER->TARGET:");
        assertEquals("FIX.4.2", sessionID.getBeginString());
        assertEquals("SENDER", sessionID.getSenderCompID());
        assertEquals("TARGET", sessionID.getTargetCompID());
        assertEquals("", sessionID.getSessionQualifier());
    }
}
