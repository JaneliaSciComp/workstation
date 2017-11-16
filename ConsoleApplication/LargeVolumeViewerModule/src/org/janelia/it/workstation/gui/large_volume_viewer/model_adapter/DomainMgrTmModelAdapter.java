package org.janelia.it.workstation.gui.large_volume_viewer.model_adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.user_data.tiled_microscope_builder.TmModelAdapter;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.options.ApplicationPanel;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.RequestProcessor;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.rabbitmq.client.Channel;
import java.util.HashMap;
import java.util.Map;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmProtobufExchanger;
import org.janelia.it.messaging.broker.sharedworkspace.HeaderConstants;
import org.janelia.it.messaging.broker.sharedworkspace.MessageType;
import org.janelia.it.messaging.client.ConnectionManager;
import org.janelia.it.messaging.client.Sender;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;

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

    private static final String MESSAGESERVER_URL = ConsoleProperties.getInstance().getProperty("domain.msgserver.url");
    private static final String MESSAGESERVER_USERACCOUNT = ConsoleProperties.getInstance().getProperty("domain.msgserver.useraccount");
    private static final String MESSAGESERVER_PASSWORD = ConsoleProperties.getInstance().getProperty("domain.msgserver.password");
    private static final String MESSAGESERVER_UPDATESEXCHANGE = ConsoleProperties.getInstance().getProperty("domain.msgserver.exchange.updates");
    private static final String MESSAGESERVER_ROUTINGKEY = ConsoleProperties.getInstance().getProperty("domain.msgserver.routingkey.updates");
    
    private RequestProcessor loadProcessor = new RequestProcessor("Tm-Save-Queue", 1, true);
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

    private enum Action {
        CREATE,
        SAVE_METADATA,
        SAVE,
        REMOVE
    }
    
    private static class NeuronAction implements Runnable {

        private TmNeuronMetadata neuron;
        private Action action;
        private SettableFuture<TmNeuronMetadata> future;

        public NeuronAction(TmNeuronMetadata neuron, Action action, SettableFuture<TmNeuronMetadata> future) {
            this.neuron = neuron;
            this.action = action;
            this.future = future;
        }

        @Override
        public void run() {
            TiledMicroscopeDomainMgr domainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
            
            ProgressHandle progress = ProgressHandleFactory.createHandle("Handling neuron " + neuron.getName() + " ...");
            try {
                progress.start();
                progress.switchToIndeterminate();
                StopWatch w = new StopWatch();

                TmNeuronMetadata result = null;
                if (action==Action.CREATE) {
                    result = domainMgr.save(neuron);
                }
                else if (action==Action.SAVE_METADATA) {
                    result = domainMgr.saveMetadata(neuron);
                }
                else if (action==Action.SAVE) {
                    result = domainMgr.save(neuron);
                }
                else if (action==Action.REMOVE) {
                    domainMgr.remove(neuron);
                }
                
                log.debug("{} for {} took {} ms", action, neuron, w.getElapsedTime());
                future.set(result);
            }
            catch (Exception ex) {
                future.setException(ex);
                FrameworkImplProvider.handleException("Error saving neuron",ex);
            }
            finally {
                progress.finish();
            }
        }
    }
    
    private Sender getSender() {
        if (messageSender==null) {
            ConnectionManager connManager = ConnectionManager.getInstance();
            connManager.configureTarget("c13u05", "admin", "mlShar@d");
            messageSender = new Sender();
            messageSender.init(connManager, "ModelUpdates", "brokerqueue");
        } 
        return messageSender;
    }
    
    private void sendMessage (TmNeuronMetadata neuron, MessageType type) throws Exception {
        List<Long> neuronIds = new ArrayList<Long>();
        neuronIds.add(neuron.getId());
        
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> updateHeaders = new HashMap<String,Object> ();
       // updateHeaders.put(HeaderConstants.TYPE, type);
       // updateHeaders.put(HeaderConstants.USER, AccessManager.getSubjectKey());
        updateHeaders.put(HeaderConstants.WORKSPACE, neuron.getWorkspaceId());
        updateHeaders.put(HeaderConstants.METADATA, mapper.writeValueAsString(neuron));
        //updateHeaders.put(HeaderConstants.NEURONIDS, neuronIds.toString());
        
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        byte[] neuronData = exchanger.serializeNeuron(neuron);
        
        getSender().sendMessage(updateHeaders, neuronData);
    }

    @Override
    public ListenableFuture<TmNeuronMetadata> asyncCreateNeuron(TmNeuronMetadata neuron) throws Exception {
        SettableFuture<TmNeuronMetadata> future = SettableFuture.create();
        loadProcessor.post(new NeuronAction(neuron, Action.CREATE, future), 0, Thread.NORM_PRIORITY);
        sendMessage (neuron, MessageType.NEURON_SAVE_NEURONDATA);
        return future;
    }

    @Override
    public ListenableFuture<TmNeuronMetadata> asyncSaveNeuron(TmNeuronMetadata neuron) throws Exception {
        SettableFuture<TmNeuronMetadata> future = SettableFuture.create();
        loadProcessor.post(new NeuronAction(neuron, Action.SAVE, future), 0, Thread.NORM_PRIORITY);
        sendMessage (neuron, MessageType.NEURON_SAVE_NEURONDATA);
 
        return future;
    }

    @Override
    public ListenableFuture<TmNeuronMetadata> asyncSaveNeuronMetadata(TmNeuronMetadata neuron) throws Exception {
        SettableFuture<TmNeuronMetadata> future = SettableFuture.create();
        loadProcessor.post(new NeuronAction(neuron, Action.SAVE_METADATA, future), 0, Thread.NORM_PRIORITY);
        sendMessage (neuron, MessageType.NEURON_SAVE_METADATA);
        
        return future;
    }

    @Override
    public ListenableFuture<TmNeuronMetadata> asyncDeleteNeuron(TmNeuronMetadata neuron) throws Exception {
        SettableFuture<TmNeuronMetadata> future = SettableFuture.create();
        loadProcessor.post(new NeuronAction(neuron, Action.REMOVE, future), 0, Thread.NORM_PRIORITY);
        return future;
    }
}
