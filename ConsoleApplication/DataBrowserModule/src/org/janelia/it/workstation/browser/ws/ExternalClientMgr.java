package org.janelia.it.workstation.browser.ws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final int MAX_CACHE_SIZE = 100;
	
	private List<ExternalClient> externalClients = new ArrayList<>();
    private int portOffset = 0;
    private int portCounter = 1;

    // TODO: These caches should really respect the DomainModel events, like invalidation
    private Map<Long,Entity> separationCache;
    private Map<Long,Entity> sampleCache;
    private Map<Long,Entity> imageCache;
    
    private DomainToEntityTranslator translator;
    
    public ExternalClientMgr() {
        this.translator = new DomainToEntityTranslator();
        this.separationCache = new MaxSizeHashMap<>(MAX_CACHE_SIZE);
        this.sampleCache = new MaxSizeHashMap<>(MAX_CACHE_SIZE);
        this.imageCache = new MaxSizeHashMap<>(MAX_CACHE_SIZE);
    }
    
    public void setPortOffset(int portOffset) {
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
    
    @Subscribe
    public void ontologySelected(DomainObjectSelectionEvent event) {

        // We only care about single selections
        DomainObject domainObject = event.getObjectIfSingle();
        if (domainObject==null) {
            return;
        }
        
        if (domainObject instanceof Ontology) {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("rootId", domainObject.getId());
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
        separationCache.put(separation.getId(), translator.getSeparationEntity(separation));
        sampleCache.put(separation.getId(), translator.getSampleEntity(separation.getParentRun().getParent().getParent()));
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("entityId", separation.getId());
        ExternalClientMgr.getInstance().sendMessageToExternalClients("entityViewRequested", parameters);
    }

    public void sendImageRequested(PipelineResult result, FileType fileType) {
        imageCache.put(result.getId(), translator.getImageEntity(result, fileType));
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("entityId", result.getId());
        ExternalClientMgr.getInstance().sendMessageToExternalClients("entityViewRequested", parameters);
    }
    
    public Entity getCachedEntity(Long entityId) {
        Entity entity = separationCache.get(entityId);
        if (entity!=null) return entity;
        entity = imageCache.get(entityId);
        return entity;
    }
    
    public Entity getCachedSampleForSeparation(Long separationId) {
        return sampleCache.get(separationId);
    }
    
    private class MaxSizeHashMap<K, V> extends HashMap<K, V> {

        private final int max;

        public MaxSizeHashMap(int max) {
            this.max = max;
        }

        @Override
        public V put(K key, V value) {
            if (size() >= max && !containsKey(key)) {
                 return null;
            } 
            else {
                 return super.put(key, value);
            }
        }
    }
}
