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
import quickfix.field.ApplVerID;
import quickfix.field.BeginString;
import quickfix.field.SenderCompID;
import quickfix.field.TargetCompID;
import quickfix.fix50.Email;

public class MessageCrackerTest extends TestCase {
    // This is mostly just checks basic functionality of
    // the message cracker. It doesn't attempt full or even good
    // test coverage of the cracker.

    private boolean messageCracked;
    
    public void testFix50MessageCracking() throws Exception {
        quickfix.fix50.Email message = new quickfix.fix50.Email();
        message.getHeader().setString(BeginString.FIELD, FixVersions.BEGINSTRING_FIXT11);
        message.getHeader().setString(SenderCompID.FIELD, "SENDER");
        message.getHeader().setString(TargetCompID.FIELD, "TARGET");
        message.getHeader().setString(ApplVerID.FIELD, ApplVerID.FIX50);

        MessageCracker cracker = new MessageCracker() {
            public void onMessage(Email email, SessionID sessionID) {
                messageCracked = true;
            }
        };

        cracker.crack(message, new SessionID(FixVersions.BEGINSTRING_FIXT11, "SENDER", "TARGER"));
        
        assertTrue(messageCracked);
    }

    // TODO FIX50 This message cracker test should pass
    
//    public void testFix50MessageCrackingWithNonFix50ApplVerID() throws Exception {
//        quickfix.fix44.Email message = new quickfix.fix44.Email();
//        message.getHeader().setString(BeginString.FIELD, FixVersions.BEGINSTRING_FIXT11);
//        message.getHeader().setString(SenderCompID.FIELD, "SENDER");
//        message.getHeader().setString(TargetCompID.FIELD, "TARGET");
//        message.getHeader().setString(ApplVerID.FIELD, ApplVerID.FIX44);
//
//        MessageCracker cracker = new MessageCracker() {
//            public void onMessage(quickfix.fix44.Email email, SessionID sessionID) {
//                messageCracked = true;
//            }
//        };
//
//        cracker.crack(message, new SessionID(FixVersions.BEGINSTRING_FIXT11, "SENDER", "TARGER"));
//        
//        assertTrue(messageCracked);
//    }
}
