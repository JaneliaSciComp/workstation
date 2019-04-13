package org.janelia.workstation.gui.large_volume_viewer.model_adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.workstation.gui.large_volume_viewer.options.ApplicationPanel;
import org.janelia.messaging.broker.sharedworkspace.HeaderConstants;
import org.janelia.messaging.broker.sharedworkspace.MessageType;
import org.janelia.messaging.client.ConnectionManager;
import org.janelia.messaging.client.Sender;
import org.janelia.model.access.tiledMicroscope.TmModelAdapter;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmProtobufExchanger;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the model adapter, which pulls/pushes data through
 * the TiledMicroscopeDomainMgr.
 *
 * When invoked to fetch exchange neurons, this implementation will do so
 * by way of the model manager.  It adapts the model manipulator for 
 * client-side use.  Not intended to be used from multiple threads, which are
 * feeding different workspaces.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainMgrTmModelAdapter implements TmModelAdapter {

    private static Logger log = LoggerFactory.getLogger(DomainMgrTmModelAdapter.class);

    private static final String MESSAGESERVER_URL = ConsoleProperties.getInstance().getProperty("domain.msgserver.url").trim();
    private static final String MESSAGESERVER_USERACCOUNT = ConsoleProperties.getInstance().getProperty("domain.msgserver.useraccount").trim();
    private static final String MESSAGESERVER_PASSWORD = ConsoleProperties.getInstance().getProperty("domain.msgserver.password").trim();
    private static final String MESSAGESERVER_UPDATESEXCHANGE = ConsoleProperties.getInstance().getProperty("domain.msgserver.exchange.updates").trim();
    private static final String MESSAGESERVER_ROUTINGKEY = ConsoleProperties.getInstance().getProperty("domain.msgserver.routingkey.updates").trim();
    
    private TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
    private Sender messageSender;

    @Override
    public List<TmNeuronMetadata> loadNeurons(TmWorkspace workspace) throws Exception {
        log.info("Loading neurons for workspace: {}", workspace);
        List<TmNeuronMetadata> neurons = new ArrayList<>();

        try {            
            StopWatch stopWatch = new StopWatch();
            List<TmNeuronMetadata> neuronList = tmDomainMgr.getWorkspaceNeurons(workspace.getId());
            log.info("Loading {} neurons took {} ms", neuronList.size(), stopWatch.getElapsedTime());
            
            if (ClientDomainUtils.hasWriteAccess(workspace)) {
                if (ApplicationPanel.isVerifyNeurons()) {
                    log.info("Checking neuron data consistency");
                        
                    // check neuron consistency and repair (some) problems
                    for (TmNeuronMetadata neuron: neuronList) {
                        log.debug("Checking neuron data for TmNeuronMetadata#{}", neuron.getId());
                        List<String> results = neuron.checkRepairNeuron();
                        if (results.size() > 0) {
                            // save results, then output to log; this is unfortunately
                            //  not visible to the user; we aren't in a place in the
                            //  code where we can pop a dialog
                            for (String s: results) {
                                log.warn(s);
                            }
                        	neuron = tmDomainMgr.save(neuron);
                        }
                        neurons.add(neuron);
                    }
                }
                else {
                    neurons.addAll(neuronList);
                }
            }
            else {
                neurons.addAll(neuronList);
            }

        }
        catch (Exception ex) {
            throw ex;
        }

        return neurons;
    }
    
    private Sender getSender() {
        if (messageSender==null) {
            ConnectionManager connManager = ConnectionManager.getInstance();
            connManager.configureTarget(MESSAGESERVER_URL,  MESSAGESERVER_USERACCOUNT, MESSAGESERVER_PASSWORD);
            messageSender = new Sender();
            messageSender.init(connManager, MESSAGESERVER_UPDATESEXCHANGE, MESSAGESERVER_ROUTINGKEY, 1);
        } 
        return messageSender;
    }
    
    private void sendMessage (TmNeuronMetadata neuron, MessageType type, Map<String,String> extraArguments) throws Exception {
        // whatever the message is, unsync the object and increment the unsynced level counter
        neuron.setSynced(false);
        neuron.incrementSyncLevel();
        
        List<Long> neuronIds = new ArrayList<Long>();
        neuronIds.add(neuron.getId());
        
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> updateHeaders = new HashMap<String,Object> ();
        updateHeaders.put(HeaderConstants.TYPE, type.toString());
        updateHeaders.put(HeaderConstants.USER, AccessManager.getSubjectKey());
        updateHeaders.put(HeaderConstants.WORKSPACE, neuron.getWorkspaceId().toString());
        updateHeaders.put(HeaderConstants.METADATA, mapper.writeValueAsString(neuron));
        updateHeaders.put(HeaderConstants.NEURONIDS, neuronIds.toString());
        if (extraArguments!=null) {
            Iterator<String> extraKeys = extraArguments.keySet().iterator();
            while (extraKeys.hasNext()) {
                String extraKey = extraKeys.next();            
                updateHeaders.put(extraKey, extraArguments.get(extraKey));
            }
        }
        
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        byte[] neuronData = exchanger.serializeNeuron(neuron);
        
        getSender().sendMessage(updateHeaders, neuronData);
    }

    @Override
    public CompletableFuture<TmNeuronMetadata> asyncCreateNeuron(TmNeuronMetadata neuron) throws Exception {
        // make sure the neuron contains the current user's ownerKey;
        neuron.setOwnerKey(AccessManager.getSubjectKey());
        sendMessage (neuron, MessageType.NEURON_CREATE, null);
        return new CompletableFuture<TmNeuronMetadata>();
    }

    @Override
    public void asyncSaveNeuron(TmNeuronMetadata neuron) throws Exception {
        sendMessage (neuron, MessageType.NEURON_SAVE_NEURONDATA, null);
    }

    @Override
    public void asyncSaveNeuronMetadata(TmNeuronMetadata neuron) throws Exception {
        sendMessage (neuron, MessageType.NEURON_SAVE_METADATA, null);
    }

    @Override
    public void asyncDeleteNeuron(TmNeuronMetadata neuron) throws Exception {
        sendMessage (neuron, MessageType.NEURON_DELETE, null);
    }
    
    @Override
    public CompletableFuture<Boolean> requestOwnership(TmNeuronMetadata neuron) throws Exception {
        sendMessage (neuron, MessageType.REQUEST_NEURON_OWNERSHIP, null);
        return new CompletableFuture<Boolean>();
    }
    
    @Override
    public CompletableFuture<Boolean> requestAssignment(TmNeuronMetadata neuron, String targetUser) throws Exception {
        Map<String,String> extraArgs = new HashMap<>();
        extraArgs.put(HeaderConstants.TARGET_USER, targetUser);        
        sendMessage (neuron, MessageType.REQUEST_NEURON_ASSIGNMENT, extraArgs);
        return new CompletableFuture<Boolean>();
    }
}
