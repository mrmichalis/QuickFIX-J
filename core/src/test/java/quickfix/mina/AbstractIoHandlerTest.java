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

package quickfix.mina;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Properties;

import org.apache.mina.common.IoSession;
import org.junit.Before;
import org.junit.Test;

import quickfix.DataDictionaryTest;
import quickfix.DefaultMessageFactory;
import quickfix.Log;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.field.ApplVerID;
import quickfix.field.EmailThreadID;
import quickfix.field.EmailType;
import quickfix.field.SenderCompID;
import quickfix.field.Subject;
import quickfix.field.TargetCompID;
import quickfix.fix50.Email;

public class AbstractIoHandlerTest {
    private AbstractIoHandler handler;
    private IoSession mockIoSession;
    private Message parsedMessage;
    private Session mockSession;
    private Log mockLog;
    
    @Before
    public void setUp() throws Exception {
        mockSession = mock(Session.class);
        mockLog = mock(Log.class);
        
        stub(mockSession.getLog()).toReturn(mockLog);
        stub(mockSession.getMessageFactory()).toReturn(new DefaultMessageFactory());
        stub(mockSession.getDataDictionary()).toReturn(DataDictionaryTest.getDictionary());
        
        NetworkingOptions options = new NetworkingOptions(new Properties());
        handler = new AbstractIoHandler(options) {
            @Override
            protected void processMessage(IoSession ioSession, Message message) throws Exception {
                parsedMessage = message;
            }

            /**
             * Stub method for looking QFJ session
             */
            @Override
            protected Session findQFSession(IoSession ioSession, SessionID sessionID) {
                return mockSession;
            }
        };
        
        mockIoSession = mock(IoSession.class);
    }
    
    @Test
    public void testMessageParsing() throws Exception {
        Email email = new Email(new EmailThreadID("THREAD_ID"), new EmailType(EmailType.NEW), new Subject("SUBJECT"));
        email.getHeader().setField(new ApplVerID(ApplVerID.FIX42));
        email.getHeader().setField(new SenderCompID("SENDER"));
        email.getHeader().setField(new TargetCompID("TARGET"));
        
        handler.messageReceived(mockIoSession, email.toString());
        
        assertThat(parsedMessage, is(notNullValue()));
        assertThat(parsedMessage.getClass().getPackage(), is(Package.getPackage("quickfix.fix42")));
    }
}
