/*******************************************************************************
 * * Copyright (c) 2001-2005 quickfixengine.org All rights reserved. * * This
 * file is part of the QuickFIX FIX Engine * * This file may be distributed
 * under the terms of the quickfixengine.org * license as defined by
 * quickfixengine.org and appearing in the file * LICENSE included in the
 * packaging of this file. * * This file is provided AS IS with NO WARRANTY OF
 * ANY KIND, INCLUDING THE * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE. * * See http://www.quickfixengine.org/LICENSE for
 * licensing information. * * Contact ask@quickfixengine.org if any conditions
 * of this licensing are * not clear to you. *
 ******************************************************************************/

package quickfix;

/**
 * Identifier for a session. Only supports a company ID (target, sender)
 * and a session qualifier. Sessions are also identified by FIX version so
 * that it's possible to have multiple sessions to the same counterparty
 * but using different FIX versions (and/or session qualifiers). 
 */
public class SessionID {
    private String beginString;
    private String senderCompID;
    private String targetCompID;
    private String sessionQualifier;
    private String id;

    public SessionID(String beginString, String senderCompID, String targetCompID) {
        create(beginString, senderCompID, targetCompID, "");
    }

    public SessionID(String beginString, String senderCompID, String targetCompID,
            String sessionQualifier) {
        create(beginString, senderCompID, targetCompID, sessionQualifier);
    }

    protected void finalize() {
        destroy();
    }

    public String getBeginString() {
        return beginString;
    }

    public String getSenderCompID() {
        return senderCompID;
    }

    public String getTargetCompID() {
        return targetCompID;
    }

    /**
     * Session qualifier can be used to identify different sessions
     * for the same target company ID. Session qualifiers can only me used
     * with initiated sessions. They cannot be used with accepted sessions.
     * @return the session qualifier
     */
    public String getSessionQualifier() {
        return sessionQualifier;
    }

    public boolean equals(Object object) {
        return object != null ? toString().equals(object.toString()) : false;
    }

    public String toString() {
        return id;
    }

    public int hashCode() {
        return toString().hashCode();
    }

    private void create(String beginString, String senderCompID, String targetCompID,
            String sessionQualifier) {
        this.beginString = beginString;
        this.senderCompID = senderCompID;
        this.targetCompID = targetCompID;
        this.sessionQualifier = sessionQualifier;
        id = beginString
                + ":"
                + senderCompID
                + "->"
                + targetCompID
                + (sessionQualifier != null && !sessionQualifier.equals("") ? ":"
                        + sessionQualifier : "");
    }

    private void destroy() {

    }
}