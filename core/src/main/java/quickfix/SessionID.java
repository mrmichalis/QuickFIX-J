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


/**
 * Identifier for a session. Only supports a company ID (target, sender)
 * and a session qualifier. Sessions are also identified by FIX version so
 * that it's possible to have multiple sessions to the same counterparty
 * but using different FIX versions (and/or session qualifiers). 
 * 
 * A session ID format is:
 * <pre>
 *    beginString:senderComp/senderSub/senderLocation->targetComp/targetSub/targetLocation:qualifier
 * </pre>
 * 
 * The subID, location and qualifier are optional. The qualifier is deprecated but included
 * for QF JNI compatibility. It may be removed at some time in the future.
 * 
 * Examples:
 * 
 * <pre>
 *   FIX.4.2:SENDER->TARGET
 *   FIX.4.4:BROKER/JOHN/CHICAGO->EXCHANGE/NYC
 *   FIX.4.2:BROKER//CLEARING->CUSTOMER
 * </pre>
 */
public class SessionID {
    private String beginString = "";
    private String senderCompID = "";
    private String senderSubID = "";
    private String senderLocationID = "";
    private String targetCompID = "";
    private String targetSubID = "";
    private String targetLocationID = "";
    private String sessionQualifier = "";
    private String id;

    public SessionID(String sessionIDString) {
        fromString(sessionIDString);
        initializeID();
    }

    public SessionID() {
        // For JNI compatibility
    }
    
    public SessionID(String beginString, String senderCompID, String targetCompID) {
        this(beginString, senderCompID, null, null, targetCompID, null, null, null);
    }

    public SessionID(String beginString, String senderCompID, String targetCompID,
            String sessionQualifier) {
        this(beginString, senderCompID, null, null, targetCompID, null, null, sessionQualifier);
    }

    public SessionID(String beginString, String senderCompID, String senderSubID, String senderLocation,
            String targetCompID, String targetSubID, String targetLocation, String sessionQualifier) {
        this.beginString = normalize(beginString);
        this.senderCompID = normalize(senderCompID);
        this.senderSubID = normalize(senderSubID);
        this.senderLocationID = normalize(senderLocation);
        this.targetCompID = normalize(targetCompID);
        this.targetSubID = normalize(targetSubID);
        this.targetLocationID = normalize(targetLocation);
        this.sessionQualifier = normalize(sessionQualifier);
        initializeID();
    }
    
    private String normalize(String s) {
        return s != null ? s.trim() : "";
    }

    /**
     * @return the beginString or an empty string.
     */
    public String getBeginString() {
        return beginString;
    }

    /**
     * @return the senderCompID or an empty string.
     */
    public String getSenderCompID() {
        return senderCompID;
    }

    /**
     * @return the targetCompID or an empty string.
     */
    public String getTargetCompID() {
        return targetCompID;
    }
    
    /**
     * @return the senderLocationID or an empty string.
     */
    public String getSenderLocationID() {
        return senderLocationID;
    }

    /**
     * @return the targetCompID or an empty string.
     */
    public String getSenderSubID() {
        return senderSubID;
    }

    /**
     * @return the targetLocationID or an empty string.
     */
    public String getTargetLocationID() {
        return targetLocationID;
    }

    /**
     * @return the targetSubID or an empty string.
     */
    public String getTargetSubID() {
        return targetSubID;
    }

    /**
     * Session qualifier can be used to identify different sessions
     * for the same target company ID. Session qualifiers can only me used
     * with initiated sessions. They cannot be used with accepted sessions.
     * 
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

    private void initializeID() {
        id = beginString
                + ":"
                + senderCompID
                + (senderSubID.length() > 0 || senderLocationID.length() > 0 ? "/" + senderSubID : senderSubID)
                + (senderLocationID.length() > 0 ? "/" + senderLocationID : senderLocationID)
                + "->"
                + targetCompID
                + (targetSubID.length() > 0 || targetLocationID.length() > 0 ? "/" + targetSubID : targetSubID)
                + (targetLocationID.length() > 0 ? "/" + targetLocationID : targetLocationID)
                + (sessionQualifier != null && !sessionQualifier.equals("") ? ":"
                        + sessionQualifier : "");
    }

    /**
     * Populate the sessionID from a string.
     * @param sessionIDString
     * @return the sessionIDString (For compatibility with QF JNI)
     */
    public String fromString(String sessionIDString) {
        int start = 0;
        int end = sessionIDString.indexOf(':');
        if (end == -1) {
            throw new RuntimeError("Couldn't parse session ID");
        }
        beginString = sessionIDString.substring(start, end);

        start = end + 1;
        end = sessionIDString.indexOf("->", start);
        if (end == -1) {
            throw new RuntimeError("Couldn't parse session ID");
        }
        String[] senderID = sessionIDString.substring(start, end).split("/");
        senderCompID = normalize(senderID[0]);
        if (senderID.length > 1) {
            senderSubID = normalize(senderID[1]);
        }
        if (senderID.length > 2) {
            senderLocationID = normalize(senderID[2]);
        }
        
        start = end + 2;
        end = sessionIDString.indexOf(":", start);
        if (end == -1) {
            end = sessionIDString.length();
        }
        String[] targetID = sessionIDString.substring(start, end).split("/");
        targetCompID = normalize(targetID[0]);
        if (targetID.length > 1) {
            targetSubID = normalize(targetID[1]);
        }
        if (targetID.length > 2) {
            targetLocationID = normalize(targetID[2]);
        }

        if (end < sessionIDString.length()) {
            start = end + 1;
            end = sessionIDString.length();
            sessionQualifier = end != -1 ? sessionIDString.substring(start, end) : "";
        }

        return sessionIDString;
    }
}