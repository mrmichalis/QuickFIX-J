package org.quickfixj.jmx.mbean.connector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import org.quickfixj.jmx.mbean.JmxSupport;
import org.quickfixj.jmx.openmbean.TabularDataAdapter;

import quickfix.mina.initiator.AbstractSocketInitiator;

class SocketInitiatorAdmin extends ConnectorAdmin implements SocketInitiatorAdminMBean {
    private final TabularDataAdapter tabularDataAdapter = new TabularDataAdapter();
    private final AbstractSocketInitiator initiator;

    protected SocketInitiatorAdmin(AbstractSocketInitiator connector, Map sessionNames) {
        super(connector, sessionNames);
        initiator = (AbstractSocketInitiator) connector;
    }
    
    public TabularData getEndpoints() throws IOException {
        try {
            return tabularDataAdapter.fromBeanList("Endpoints", "Endpoint", "sessionID", new ArrayList(
                    initiator.getInitiators()));
        } catch (OpenDataException e) {
            throw JmxSupport.toIOException(e);
        }
    }
}
