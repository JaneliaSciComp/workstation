package org.janelia.it.workstation.gui.browser.ws;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;
import com.google.common.eventbus.Subscribe;

/**
 * Keeps track of external clients to the Workstation. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExternalClientMgr {

    // Singleton
    private static ExternalClientMgr instance;
    public static ExternalClientMgr getInstance() {
        if (instance==null) {
            instance = new ExternalClientMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }
    
	private final static Logger log = LoggerFactory.getLogger(ExternalClientMgr.class);

	private List<ExternalClient> externalClients = new ArrayList<>();
    private int portOffset = 0;
    private int portCounter = 1;
	

    Map<Long,NeuronSeparation> separationCache;
    public ExternalClientMgr() {
        this.separationCache = new MapMaker().weakValues().makeMap();
    }
    
    public void setPortOffset(int portOffset) {
        log.info("Setting external client port offset to: {}",portOffset);
    	this.portOffset = portOffset;
    }

    /**
     * Since there can be more than one external client of a given tool add them all as listeners and the
     * instantiation of ExternalClients will add the unique timestamp id
     * @param newClientName the external tool to add
     */
    public int addExternalClient(String newClientName) {
        ExternalClient newClient = new ExternalClient(portOffset+portCounter, newClientName);
        externalClients.add(newClient);
        this.portCounter+=1;
        return newClient.getClientPort();
    }

    public List<ExternalClient> getExternalClientsByName(String clientName){
        List<ExternalClient> returnList = new ArrayList<>();
        for (ExternalClient externalClient : externalClients) {
            if (externalClient.getName().equals(clientName)) { returnList.add(externalClient); }
        }
        return returnList;
    }

    public ExternalClient getExternalClientByPort(int targetPort) {
        for (ExternalClient externalClient : externalClients) {
            // There can be only one - client-to-port that is...
            if (externalClient.getClientPort()==targetPort) {
                return externalClient;
            }
        }
        return null;
    }

    public void removeExternalClientByPort(int targetPort) {
        ExternalClient targetClient = null;
        for (ExternalClient externalClient : externalClients) {
            if (externalClient.getClientPort() == targetPort) {
                targetClient = externalClient;
                break;
            }
        }
        if (null != targetClient) {
            externalClients.remove(targetClient);
        }
    }

    public void sendMessageToExternalClients(String operationName, Map<String, Object> parameters) {
        for (ExternalClient externalClient : externalClients) {
            try {
                externalClient.sendMessage(operationName, parameters);
            } 
            catch (Exception e) {
                log.error("Error sending message to external clients: " + operationName, e);
            }
        }
    }
    
    // TODO: move these elsewhere?
    
    @Subscribe
    public void ontologySelected(DomainObjectSelectionEvent event) {
        DomainObject obj = event.getDomainObject();
        if (obj instanceof Ontology) {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("rootId", obj.getId());
            ExternalClientMgr.getInstance().sendMessageToExternalClients("ontologySelected", parameters);   
        }
    }

    @Subscribe
    public void ontologyChanged(DomainObjectChangeEvent event) {
        DomainObject obj = event.getDomainObject();
        if (obj instanceof Ontology) {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("rootId", obj.getId());
            ExternalClientMgr.getInstance().sendMessageToExternalClients("ontologyChanged", parameters);
        }
    }
    
    public void sendNeuronSeparationRequested(NeuronSeparation separation) {
        separationCache.put(separation.getId(), separation);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("entityId", separation.getId());
        ExternalClientMgr.getInstance().sendMessageToExternalClients("entityViewRequested", parameters);
    }

    public NeuronSeparation getNeuronSeparation(Long separationId) {
        return separationCache.get(separationId);
    }
}
