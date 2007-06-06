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

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

/**
 * Used by the session communications code. Not intended to be used by
 * applications.
 * 
 * All dynamic data is protected by the session's intrinsic lock. The
 * log and message store implementation must be thread safe.
 */
public final class SessionState {
    private final Object lock;
    private final Log log;
    private final MessageStore messageStore;
    private final boolean initiator;

    private long logonTimeoutMs = 10000L;
    private long logoutTimeoutMs = 2000L;

    private boolean logonSent;
    private boolean logonReceived;
    private boolean logoutSent;
    private boolean logoutReceived = false;
    private int testRequestCounter;
    private long lastSentTime;
    private long lastReceivedTime;
    private boolean withinHeartBeat;
    private long heartBeatMillis = Long.MAX_VALUE;
    private int heartBeatInterval;
    private HashMap messageQueue = new HashMap();
    private int[] resendRange = new int[] { 0, 0 };
    private boolean resetSent;
    private boolean resetReceived;
    private String logoutReason;

    public SessionState(Object lock, Log log, int heartBeatInterval, boolean initiator,
            MessageStore messageStore) {
        this.lock = lock;
        this.initiator = initiator;
        this.messageStore = messageStore;
        this.heartBeatInterval = heartBeatInterval;
        this.log = log == null ? new NullLog() : log;
    }

    public int getHeartBeatInterval() {
        synchronized (lock) {
            return heartBeatInterval;
        }
    }

    public void setHeartBeatInterval(int heartBeatInterval) {
        synchronized (lock) {
            this.heartBeatInterval = heartBeatInterval;
        }
        setHeartBeatMillis(heartBeatInterval * 1000L);
    }

    private void setHeartBeatMillis(long heartBeatMillis) {
        synchronized (lock) {
            this.heartBeatMillis = heartBeatMillis;
        }
    }

    private long getHeartBeatMillis() {
        synchronized (lock) {
            return heartBeatMillis;
        }
    }

    public boolean isHeartBeatNeeded() {
        long millisSinceLastSentTime = SystemTime.currentTimeMillis() - getLastSentTime();
        return millisSinceLastSentTime >= getHeartBeatMillis() && getTestRequestCounter() == 0;
    }

    public boolean isInitiator() {
        return initiator;
    }

    public long getLastReceivedTime() {
        synchronized (lock) {
            return lastReceivedTime;
        }
    }

    public void setLastReceivedTime(long lastReceivedTime) {
        synchronized (lock) {
            this.lastReceivedTime = lastReceivedTime;
        }
    }

    public long getLastSentTime() {
        synchronized (lock) {
            return lastSentTime;
        }
    }

    public void setLastSentTime(long lastSentTime) {
        synchronized (lock) {
            this.lastSentTime = lastSentTime;
        }
    }

    public Log getLog() {
        return log;
    }

    public boolean isLogonAlreadySent() {
        return isInitiator() && isLogonSent();
    }

    public boolean isLogonReceived() {
        synchronized (lock) {
            return logonReceived;
        }
    }

    public void setLogonReceived(boolean logonReceived) {
        synchronized (lock) {
            this.logonReceived = logonReceived;
        }
    }

    public boolean isLogonSendNeeded() {
        return isInitiator() && !isLogonSent();
    }

    public boolean isLogonSent() {
        synchronized (lock) {
            return logonSent;
        }
    }

    public void setLogonSent(boolean logonSent) {
        synchronized (lock) {
            this.logonSent = logonSent;
        }
    }

    public boolean isLogonTimedOut() {
        synchronized (lock) {
            return isLogonSent()
                    && SystemTime.currentTimeMillis() - getLastReceivedTime() >= getLogonTimeoutMs();
        }
    }

    public void setLogonTimeout(int logonTimeout) {
        setLogonTimeoutMs(logonTimeout * 1000L);
    }

    public int getLogonTimeout() {
        return (int) (getLogonTimeoutMs() / 1000L);
    }

    public void setLogoutTimeout(int logoutTimeout) {
        setLogoutTimeoutMs(logoutTimeout * 1000L);
    }

    public int getLogoutTimeout() {
        return (int) (getLogoutTimeoutMs() / 1000L);
    }

    private void setLogoutTimeoutMs(long logoutTimeoutMs) {
        synchronized (lock) {
            this.logoutTimeoutMs = logoutTimeoutMs;
        }
    }

    private long getLogoutTimeoutMs() {
        synchronized (lock) {
            return logoutTimeoutMs;
        }
    }

    private void setLogonTimeoutMs(long logonTimeoutMs) {
        synchronized (lock) {
            this.logonTimeoutMs = logonTimeoutMs;
        }
    }

    private long getLogonTimeoutMs() {
        synchronized (lock) {
            return logonTimeoutMs;
        }
    }

    public boolean isLogoutSent() {
        synchronized (lock) {
            return logoutSent;
        }
    }

    public void setLogoutSent(boolean logoutSent) {
        synchronized (lock) {
            this.logoutSent = logoutSent;
        }
    }

    public boolean isLogoutReceived() {
        synchronized (lock) {
            return logoutReceived;
        }
    }

    public void setLogoutReceived(boolean logoutReceived) {
        synchronized (lock) {
            this.logoutReceived = logoutReceived;
        }
    }

    public boolean isLogoutTimedOut() {
        return isLogoutSent()
                && ((SystemTime.currentTimeMillis() - getLastSentTime()) >= getLogoutTimeoutMs());
    }

    public MessageStore getMessageStore() {
        return messageStore;
    }

    public int getTestRequestCounter() {
        synchronized (lock) {
            return testRequestCounter;
        }
    }

    public void clearTestRequestCounter() {
        synchronized (lock) {
            testRequestCounter = 0;
        }
    }

    public void incrementTestRequestCounter() {
        synchronized (lock) {
            testRequestCounter++;
        }
    }

    public boolean isTestRequestNeeded() {
        long millisSinceLastReceivedTime = timeSinceLastReceivedMessage();
        return millisSinceLastReceivedTime >= (1.5 * (getTestRequestCounter() + 1))
                * getHeartBeatMillis();
    }

    private long timeSinceLastReceivedMessage() {
        return SystemTime.currentTimeMillis() - getLastReceivedTime();
    }

    public boolean isTimedOut() {
        long millisSinceLastReceivedTime = timeSinceLastReceivedMessage();
        return millisSinceLastReceivedTime >= 2.4 * getHeartBeatMillis();
    }

    public boolean isWithinHeartBeat() {
        synchronized (lock) {
            return withinHeartBeat;
        }
    }

    public boolean set(int sequence, String message) throws IOException {
        return messageStore.set(sequence, message);
    }

    public void get(int first, int last, Collection messages) throws IOException {
        messageStore.get(first, last, messages);
    }

    public void enqueue(int sequence, Message message) {
        messageQueue.put(new Integer(sequence), message);
    }

    public Message dequeue(int sequence) {
        return (Message) messageQueue.get(new Integer(sequence));
    }

    public void clearQueue() {
        messageQueue.clear();
    }

    public int getNextSenderMsgSeqNum() throws IOException {
        return messageStore.getNextSenderMsgSeqNum();
    }

    public int getNextTargetMsgSeqNum() throws IOException {
        return messageStore.getNextTargetMsgSeqNum();
    }

    public void setNextTargetMsgSeqNum(int sequence) throws IOException {
        messageStore.setNextTargetMsgSeqNum(sequence);
    }

    public void incrNextSenderMsgSeqNum() throws IOException {
        messageStore.incrNextSenderMsgSeqNum();
    }

    public void incrNextTargetMsgSeqNum() throws IOException {
        messageStore.incrNextTargetMsgSeqNum();
    }

    public Date getCreationTime() throws IOException {
        return messageStore.getCreationTime();
    }

    public void reset() {
        try {
            messageStore.reset();
        } catch (IOException e) {
            throw new RuntimeError(e);
        }
    }

    public void setResendRange(int low, int high) {
        synchronized (lock) {
            resendRange[0] = low;
            resendRange[1] = high;
        }
    }

    public boolean isResendRequested() {
        synchronized (lock) {
            return !(resendRange[0] == 0 && resendRange[1] == 0);
        }
    }

    public int[] getResendRange() {
        synchronized (lock) {
            return resendRange;
        }
    }

    public boolean isResetReceived() {
        synchronized (lock) {
            return resetReceived;
        }
    }

    public void setResetReceived(boolean resetReceived) {
        synchronized (lock) {
            this.resetReceived = resetReceived;
        }
    }

    public boolean isResetSent() {
        synchronized (lock) {
            return resetSent;
        }
    }

    public void setResetSent(boolean resetSent) {
        synchronized (lock) {
            this.resetSent = resetSent;
        }
    }

    public void setLogoutReason(String reason) {
        synchronized (lock) {
            logoutReason = reason;
        }
    }

    public String getLogoutReason() {
        synchronized (lock) {
            return logoutReason;
        }
    }

    public void clearLogoutReason() {
        synchronized (lock) {
            logoutReason = "";
        }
    }

    private final class NullLog implements Log {
        public void onOutgoing(String message) {
        }

        public void onIncoming(String message) {
        }

        public void onEvent(String text) {
        }

        public void clear() {
        }
    }
}