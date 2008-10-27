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

package quickfix.mina.acceptor;

import org.apache.mina.common.IoSession;

import quickfix.*;
import quickfix.field.*;
import quickfix.mina.AbstractIoHandler;
import quickfix.mina.EventHandlingStrategy;
import quickfix.mina.IoSessionResponder;
import quickfix.mina.NetworkingOptions;
import quickfix.mina.SessionConnector;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.io.ByteArrayInputStream;

class AcceptorIoHandler extends AbstractIoHandler {
    private final EventHandlingStrategy eventHandlingStrategy;

    private final AcceptorSessionProvider sessionProvider;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);

    public AcceptorIoHandler(AcceptorSessionProvider sessionProvider, NetworkingOptions networkingOptions,
            EventHandlingStrategy eventHandingStrategy) {
        super(networkingOptions);
        this.sessionProvider = sessionProvider;
        this.eventHandlingStrategy = eventHandingStrategy;
    }

    public void sessionCreated(IoSession session) throws Exception {
        super.sessionCreated(session);
        log.info("MINA session created: " + session.getRemoteAddress());
    }

    protected void processMessage(final IoSession protocolSession, Message message) throws Exception {
        Session qfSession = (Session) protocolSession.getAttribute(SessionConnector.QF_SESSION);
        if (qfSession == null) {
            if (message.getHeader().getString(MsgType.FIELD).equals(MsgType.LOGON)) {
                SessionID sessionID = MessageUtils.getReverseSessionID(message);
                qfSession = sessionProvider.getSession(sessionID);
                if (qfSession != null) {
                    Log sessionLog = qfSession.getLog();
                    if (qfSession.hasResponder()) {
                        // Session is already bound to another connection
                        String rejectMsg = "Multiple logons/connections for this session are not allowed";
                        sessionLog.onEvent(rejectMsg);
                        sendRejectOnMultipleLogonsAndDisconnect(protocolSession, message, qfSession, sessionID, sessionLog, rejectMsg);
                        return;
                    }
                    sessionLog.onEvent("Accepting session " + qfSession.getSessionID() + " from "
                            + protocolSession.getRemoteAddress());
                    int heartbeatInterval = message.getInt(HeartBtInt.FIELD);
                    qfSession.setHeartBeatInterval(heartbeatInterval);
                    sessionLog.onEvent("Acceptor heartbeat set to " + heartbeatInterval
                            + " seconds");
                    protocolSession.setAttribute(SessionConnector.QF_SESSION, qfSession);
                    NetworkingOptions networkingOptions = getNetworkingOptions();
                    qfSession.setResponder(new IoSessionResponder(protocolSession,
                            networkingOptions.getSynchronousWrites(),
                            networkingOptions.getSynchronousWriteTimeout()));
                } else {
                    log.error("Unknown session ID during logon: " + sessionID);
                    return;
                }
            } else {
                log.warn("Ignoring non-logon message before session establishment: " + message);
                return;
            }
        }
    
        if (qfSession == null) {
            // [QFJ-117] this can happen if a late test request arrives after we 
            // gave up waiting and closed the session.
            log.error("Attempt to process message for non existant or closed session (only "
                    + "legal action for logon messages). MsgType="
                    + message.getHeader().getString(MsgType.FIELD));
        } else {
            eventHandlingStrategy.onMessage(qfSession, message);
        }
    }

    /** When multiple logons are detected, we want to not only disconnect the session immediately, but also
     * to send a reject message
     * We can't send the reject messge on the known session since it'll go to a valid existing connection.
     * Instead, create a temp IoSessionResponder with a vanilla session info that has the incoming session id,
     * increment the msgSeqNum and send a reject back on that, and close the responder immediately.
     */
    private void sendRejectOnMultipleLogonsAndDisconnect(final IoSession protocolSession, Message message,
                                                         Session qfSession, SessionID sessionID, final Log sessionLog, String rejectMsg)
            throws ConfigError, FieldNotFound {
        // approach a: create a temp session to reply to the incoming multiple-logged on request
        SessionFactory sessFactory = new DefaultSessionFactory(new ApplicationAdapter(), new MemoryStoreFactory(), null);
        String settingsString = "";
        settingsString += "[default]\n";
        settingsString += "BeginString="+qfSession.getSessionID().getBeginString()+"\n";
        settingsString += "ConnectionType=acceptor\n";
        settingsString += "StartTime=00:00:00\n";
        settingsString += "EndTime=00:00:00\n";
        final Session tempSession = sessFactory.create(qfSession.getSessionID(),
                new SessionSettings(new ByteArrayInputStream(settingsString.getBytes())));

        // create a reject with a reason for disconnect
        Message reject = qfSession.getMessageFactory().create(sessionID.getBeginString(), MsgType.LOGOUT);
        reject.setField(new Text(rejectMsg));
        reject.getHeader().setField(new MsgSeqNum(message.getHeader().getInt(MsgSeqNum.FIELD)+1));
        reject.getHeader().setField(new SenderCompID(sessionID.getSenderCompID()));
        reject.getHeader().setField(new TargetCompID(sessionID.getTargetCompID()));
        reject.getHeader().setField(new SendingTime(new Date()));
        sessionLog.onEvent("sending reject for multiple logons to "+tempSession.getSessionID() + ": "+reject);

        // Create a temp responder to reply with reject directly on the established channel
        final IoSessionResponder responder = new IoSessionResponder(protocolSession, true, 100);
        responder.send(reject.toString());
        // disconnect the responder in the future to give it some time to send a message out
        executor.schedule((new Runnable() {
            public void run() {
                responder.disconnect();
                protocolSession.close();
                sessionLog.onEvent("Closed protocol/responder for duplicate session: "+tempSession.getSessionID());
            }
        }), 3, TimeUnit.SECONDS);
    }

    protected Session findQFSession(IoSession protocolSession, SessionID sessionID) {
        Session s = super.findQFSession(protocolSession, sessionID);
        if (s == null) {
            s = sessionProvider.getSession(sessionID);
        }
        return s;
    }
    
    
}
