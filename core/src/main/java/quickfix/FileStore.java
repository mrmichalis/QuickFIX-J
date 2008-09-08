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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.quickfixj.CharsetSupport;

import quickfix.field.converter.UtcTimestampConverter;

/**
 * File store implementation. THIS CLASS IS PUBLIC ONLY TO MAINTAIN
 * COMPATIBILITY WITH THE QUICKFIX JNI. IT SHOULD ONLY BE CREATED USING A
 * FACTORY.
 * 
 * @see quickfix.FileStoreFactory
 */
public class FileStore implements MessageStore {
    private static final String READ_OPTION = "r";
    private static final String WRITE_OPTION = "w";
    private static final String SYNC_OPTION = "d";
    private static final String NOSYNC_OPTION = "";

    private final TreeMap<Long, long[]> messageIndex;
    private final MemoryStore cache = new MemoryStore();

    private final String msgFileName;
    private final String headerFileName;
    private final String seqNumFileName;
    private final String sessionFileName;
    private final boolean syncWrites;
    private final int maxCachedMsgs;
    private final String charsetEncoding = CharsetSupport.getCharset();

    private RandomAccessFile messageFile;
    private DataOutputStream headerDataOutputStream;
    private FileOutputStream headerFileOutputStream;
    private RandomAccessFile sequenceNumberFile;

    FileStore(String path, SessionID sessionID, boolean syncWrites, int maxCachedMsgs) throws IOException {
        this.syncWrites = syncWrites;
        this.maxCachedMsgs = maxCachedMsgs;
        
        if (maxCachedMsgs > 0) {
            messageIndex = new TreeMap<Long, long[]>();
        } else {
            messageIndex = null;
        }

        if (path == null) {
            path = ".";
        }
		path = new File(path).getAbsolutePath();
		
        String sessionName = FileUtil.sessionIdFileName(sessionID);
        String prefix = FileUtil.fileAppendPath(path, sessionName + ".");

        msgFileName = prefix + "body";
        headerFileName = prefix + "header";
        seqNumFileName = prefix + "seqnums";
        sessionFileName = prefix + "session";

        File directory = new File(msgFileName).getParentFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }

        initialize(false);
    }

    void initialize(boolean deleteFiles) throws IOException {
        closeFiles();

        if (deleteFiles) {
            deleteFiles();
        }
        
        messageFile = new RandomAccessFile(msgFileName, getRandomAccessFileOptions());
        sequenceNumberFile = new RandomAccessFile(seqNumFileName, getRandomAccessFileOptions());

        initializeCache();
    }

    private void initializeCache() throws IOException {
        cache.reset();
        initializeMessageIndex();
        initializeSequenceNumbers();
        initializeSessionCreateTime();
        messageFile.seek(messageFile.length());
    }

    private void initializeSessionCreateTime() throws IOException {
        File sessionTimeFile = new File(sessionFileName);
        if (sessionTimeFile.exists()) {
            DataInputStream sessionTimeInput = new DataInputStream(new BufferedInputStream(
                    new FileInputStream(sessionTimeFile)));
            try {
                Calendar c = SystemTime.getUtcCalendar(UtcTimestampConverter.convert(sessionTimeInput.readUTF()));
                cache.setCreationTime(c);
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            } finally {
                sessionTimeInput.close();
            }
        } else {
            storeSessionTimeStamp();
        }
    }

    private void storeSessionTimeStamp() throws IOException {
        DataOutputStream sessionTimeOutput = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(sessionFileName, false)));
        try {
            Date date = SystemTime.getDate();
            cache.setCreationTime(SystemTime.getUtcCalendar(date));
            sessionTimeOutput.writeUTF(UtcTimestampConverter.convert(date, true));
        } finally {
            sessionTimeOutput.close();
        }
    }

    /* (non-Javadoc)
     * @see quickfix.MessageStore#getCreationTime()
     */
    public Date getCreationTime() throws IOException {
        return cache.getCreationTime();
    }

    private void initializeSequenceNumbers() throws IOException {
        sequenceNumberFile.seek(0);
        if (sequenceNumberFile.length() > 0) {
            String s = sequenceNumberFile.readUTF();
            int offset = s.indexOf(':');
            cache.setNextSenderMsgSeqNum(Integer.parseInt(s.substring(0, offset)));
            cache.setNextTargetMsgSeqNum(Integer.parseInt(s.substring(offset + 1)));
        }
    }

    private void initializeMessageIndex() throws IOException {
        // this part is unnecessary if no offsets are being stored in memory
        if (messageIndex != null) {
            messageIndex.clear();
            File headerFile = new File(headerFileName);
            if (headerFile.exists()) {
                DataInputStream headerDataInputStream = new DataInputStream(new BufferedInputStream(
                        new FileInputStream(headerFile)));
                try {
                    while (headerDataInputStream.available() > 0) {
                        int sequenceNumber = headerDataInputStream.readInt();
                        long offset = headerDataInputStream.readLong();
                        int size = headerDataInputStream.readInt();
                        updateMessageIndex((long)sequenceNumber, new long[] { offset, size });
                    }
                } finally {
                    headerDataInputStream.close();
                }
            }
        }
        headerFileOutputStream = new FileOutputStream(headerFileName, true);
        headerDataOutputStream = new DataOutputStream(new BufferedOutputStream(
                headerFileOutputStream));
    }
    
    private void updateMessageIndex(Long sequenceNum, long[] offsetAndSize) {
        // Remove the lowest indexed sequence number if this addition
        // would result the index growing to larger than maxCachedMsgs.
        if (messageIndex.size() >= maxCachedMsgs && messageIndex.get(sequenceNum) == null) {
            messageIndex.pollFirstEntry();
        }
        
        messageIndex.put(sequenceNum, offsetAndSize);
    }

    private String getRandomAccessFileOptions() {
        return READ_OPTION + WRITE_OPTION + (syncWrites ? SYNC_OPTION : NOSYNC_OPTION);
    }

    /**
     * Close the store's files.
     * @throws IOException
     */
    public void closeFiles() throws IOException {
        closeOutputStream(headerDataOutputStream);
        closeFile(messageFile);
        closeFile(sequenceNumberFile);
    }

    private void closeFile(RandomAccessFile file) throws IOException {
        if (file != null) {
            file.close();
        }
    }

    private void closeOutputStream(OutputStream stream) throws IOException {
        if (stream != null) {
            stream.close();
        }
    }

    public void deleteFiles() throws IOException {
        closeFiles();
        deleteFile(headerFileName);
        deleteFile(msgFileName);
        deleteFile(seqNumFileName);
        deleteFile(sessionFileName);
    }

    private void deleteFile(String fileName) throws IOException {
        File file = new File(fileName);
        if (file.exists() && !file.delete()) {
            System.err.println("File delete failed: " + fileName);
        }
    }

    /* (non-Javadoc)
     * @see quickfix.MessageStore#getNextSenderMsgSeqNum()
     */
    public int getNextSenderMsgSeqNum() throws IOException {
        return cache.getNextSenderMsgSeqNum();
    }

    /* (non-Javadoc)
     * @see quickfix.MessageStore#getNextTargetMsgSeqNum()
     */
    public int getNextTargetMsgSeqNum() throws IOException {
        return cache.getNextTargetMsgSeqNum();
    }

    /* (non-Javadoc)
     * @see quickfix.MessageStore#setNextSenderMsgSeqNum(int)
     */
    public void setNextSenderMsgSeqNum(int next) throws IOException {
        cache.setNextSenderMsgSeqNum(next);
        storeSequenceNumbers();
    }

    /* (non-Javadoc)
     * @see quickfix.MessageStore#setNextTargetMsgSeqNum(int)
     */
    public void setNextTargetMsgSeqNum(int next) throws IOException {
        cache.setNextTargetMsgSeqNum(next);
        storeSequenceNumbers();
    }

    /* (non-Javadoc)
     * @see quickfix.MessageStore#incrNextSenderMsgSeqNum()
     */
    public void incrNextSenderMsgSeqNum() throws IOException {
        cache.incrNextSenderMsgSeqNum();
        storeSequenceNumbers();
    }

    /* (non-Javadoc)
     * @see quickfix.MessageStore#incrNextTargetMsgSeqNum()
     */
    public void incrNextTargetMsgSeqNum() throws IOException {
        cache.incrNextTargetMsgSeqNum();
        storeSequenceNumbers();
    }

    /* (non-Javadoc)
     * @see quickfix.MessageStore#get(int, int, java.util.Collection)
     */
    public void get(int startSequence, int endSequence, Collection<String> messages) throws IOException {
        Set<Integer> uncachedOffsetMsgIds = new HashSet<Integer>();
        // Use a treemap to make sure the messages are sorted by sequence num
        TreeMap<Integer, String> messagesFound = new TreeMap<Integer, String>();
        for (int i = startSequence; i <= endSequence; i++) {
            String message = getMessage(i);
            if (message != null) {
                messagesFound.put(i, message);
            } else {
                uncachedOffsetMsgIds.add(i);
            }
        }
        
        if (!uncachedOffsetMsgIds.isEmpty()) {
            // parse the header file to find missing messages
            File headerFile = new File(headerFileName);
            DataInputStream headerDataInputStream = new DataInputStream(new BufferedInputStream(
                    new FileInputStream(headerFile)));
            try {
                while (headerDataInputStream.available() > 0) {
                    int sequenceNumber = headerDataInputStream.readInt();
                    long offset = headerDataInputStream.readLong();
                    int size = headerDataInputStream.readInt();
                    if (uncachedOffsetMsgIds.remove(sequenceNumber)) {
                        String message = getMessage(new long[] {offset, size});
                        if (message != null) {
                            messagesFound.put(sequenceNumber, message);
                        }
                    }
                    if (uncachedOffsetMsgIds.isEmpty()) {
                        break;
                    }
                }
            } finally {
                headerDataInputStream.close();
            }
        }
        
        messages.addAll(messagesFound.values());
    }

    /**
     * This method is here for JNI API consistency but it's not 
     * implemented. Use get(int, int, Collection) with the same 
     * start and end sequence.
     * 
     */
    public boolean get(int sequence, String message) throws IOException {
        throw new UnsupportedOperationException("not supported");
    }

    private String getMessage(int i) throws IOException {
        String message = null;
        if (messageIndex != null) {
            long[] offsetAndSize = messageIndex.get((long)i);
            if (offsetAndSize != null) {
                message = getMessage(offsetAndSize);
            }
        }
        return message;
    }

    private String getMessage(long[] offsetAndSize) throws IOException {
        messageFile.seek(offsetAndSize[0]);
        int size = (int) offsetAndSize[1];
        byte[] data = new byte[size];
        if (messageFile.read(data) != size) {
            throw new IOException("Truncated input while reading message");
        }
        String message = new String(data, charsetEncoding);
        messageFile.seek(messageFile.length());
        return message;
    }

    /* (non-Javadoc)
     * @see quickfix.MessageStore#set(int, java.lang.String)
     */
    public boolean set(int sequence, String message) throws IOException {
        long offset = messageFile.getFilePointer();
        int size = message.length();
        if (messageIndex != null) {
            updateMessageIndex((long)sequence, new long[] { offset, size });
        }
        headerDataOutputStream.writeInt(sequence);
        headerDataOutputStream.writeLong(offset);
        headerDataOutputStream.writeInt(size);
        headerDataOutputStream.flush();
        if (syncWrites) {
            headerFileOutputStream.getFD().sync();
        }
        messageFile.write(message.getBytes(CharsetSupport.getCharset()));
        return true;
    }

    private void storeSequenceNumbers() throws IOException {
        sequenceNumberFile.seek(0);
        // I changed this from explicitly using a StringBuffer because of
        // recommendations from Sun. The performance also appears higher
        // with this implementation. -- smb.
        // http://bugs.sun.com/bugdatabase/view_bug.do;:WuuT?bug_id=4259569
        sequenceNumberFile.writeUTF(""+cache.getNextSenderMsgSeqNum()+':'+cache.getNextTargetMsgSeqNum());
    }

    String getHeaderFileName() {
        return headerFileName;
    }

    String getMsgFileName() {
        return msgFileName;
    }

    String getSeqNumFileName() {
        return seqNumFileName;
    }

    /*
     * (non-Javadoc)
     * @see quickfix.RefreshableMessageStore#refresh()
     */
    public void refresh() throws IOException {
        initialize(false);
    }

    /* (non-Javadoc)
     * @see quickfix.MessageStore#reset()
     */
    public void reset() throws IOException {
        initialize(true);
    }
}