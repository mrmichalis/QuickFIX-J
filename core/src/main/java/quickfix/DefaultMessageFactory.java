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

import static quickfix.FixVersions.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The default factory for creating FIX message instances. Create subclasses
 * of this factory for generating custom messages using nonstandard message
 * types.
 */
public class DefaultMessageFactory implements MessageFactory {
    private final Map<String,MessageFactory> messageFactories
        = new ConcurrentHashMap<String,MessageFactory>();

    public DefaultMessageFactory() {
        // To loosen the coupling between this factory and generated code, the
        // message factories are discovered at run time using reflection.
        discoverFactory(BEGINSTRING_FIX40, "quickfix.fix40.MessageFactory");
        discoverFactory(BEGINSTRING_FIX41, "quickfix.fix41.MessageFactory");
        discoverFactory(BEGINSTRING_FIX42, "quickfix.fix42.MessageFactory");
        discoverFactory(BEGINSTRING_FIX43, "quickfix.fix43.MessageFactory");
        discoverFactory(BEGINSTRING_FIX44, "quickfix.fix44.MessageFactory");
        discoverFactory(FIX50, "quickfix.fix50.MessageFactory");
    }

    private void discoverFactory(String beginString, String factoryClassName) {
        try {
            messageFactories.put(beginString, (MessageFactory) Class.forName(
                    factoryClassName).newInstance());
        } catch (InstantiationException e) {
            // ignored
        } catch (IllegalAccessException e) {
            // ignored
        } catch (ClassNotFoundException e) {
            // ignored
        }
    }
    
    // TODO FIX50 Do we need both the beginString and ApplVerID to create message?
	
    public Message create(String beginString, String msgType) {
        MessageFactory messageFactory = messageFactories.get(beginString);
        if (messageFactory != null) {
            return messageFactory.create(beginString, msgType);
        }
        return new Message();
    }

    public Group create(String beginString, String msgType, int correspondingFieldID) {
        MessageFactory messageFactory = messageFactories.get(beginString);
        if (messageFactory != null) {
            return messageFactory.create(beginString, msgType, correspondingFieldID);
        }
        throw new IllegalArgumentException("Unsupported FIX version: "+beginString);
    }
}
