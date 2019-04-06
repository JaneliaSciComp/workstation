package org.janelia.it.workstation.browser.ws;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.browser.events.selection.OntologySelectionEvent;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.PipelineResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private Map<Long,Entity> separationCache;
    private Map<Long,Entity> sampleCache;
    private Map<Long,Entity> imageCache;
    
    private DomainToEntityTranslator translator;
    
    public ExternalClientMgr() {
        this.translator = new DomainToEntityTranslator();
        this.separationCache = createLRUMap(MAX_CACHE_SIZE);
        this.sampleCache = createLRUMap(MAX_CACHE_SIZE);
        this.imageCache = createLRUMap(MAX_CACHE_SIZE);
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
    public void ontologySelected(OntologySelectionEvent event) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("rootId", event.getOntologyId());
        ExternalClientMgr.getInstance().sendMessageToExternalClients("ontologySelected", parameters);
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

    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {
        if (event.isTotalInvalidation()) {
            log.debug("Total invalidation detected, refreshing...");
            separationCache.clear();
            sampleCache.clear();
            imageCache.clear();
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
    
    // From https://stackoverflow.com/questions/11469045/how-to-limit-the-maximum-size-of-a-map-by-removing-oldest-entries-when-limit-rea
    public static <K, V> Map<K, V> createLRUMap(final int maxEntries) {
        return new LinkedHashMap<K, V>(maxEntries*10/7, 0.7f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }
}
