/**
 * Implementation of the model adapter, which pulls/pushes data through
 * the Model Manager.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.model_adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.user_data.tiled_microscope_builder.TmModelAdapter;
import org.janelia.it.jacs.model.user_data.tiled_microscope_protobuf.TmProtobufExchanger;
import org.janelia.it.jacs.model.util.ThreadUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.CustomNamedThreadFactory;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Encoder;

/**
 * When invoked to fetch exchange neurons, this implementation will do so
 * by way of the model manager.  It adapts the model manipulator for 
 * client-side use.  Not intended to be used from multiple threads, which are
 * feeding different workspaces.
 *
 * @author fosterl
 */
public class ModelManagerTmModelAdapter implements TmModelAdapter {
    public static final int DESERIALIZATION_THREAD_COUNT = 10;
    public static final int MAX_WAIT_MIN = 120;
    // Very large initial work unit estimate; just need to work out the
    // conversion factor between this and the true number, once that is
    // learned.
    public static final int TOTAL_WORKUNITS = 3;
    
    private final Map<Long,Entity> wsIdToEntity = new HashMap<>();

    private final TmProtobufExchanger exchanger = new TmProtobufExchanger();
    
    private Logger log = LoggerFactory.getLogger(ModelManagerTmModelAdapter.class);
    
    @Override
    public void loadNeurons(TmWorkspace workspace) throws Exception {
        final ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Loading annotations...");
        try {
            // Obtain the serialized version of the data as raw byte buffers.
            progressHandle.start(TOTAL_WORKUNITS);
            progressHandle.setInitialDelay(0);            
            progressHandle.progress(0);
            progressHandle.setDisplayName("Loading annotations...");
            //progressHandle.switchToIndeterminate();
            progressHandle.setDisplayName("Fetching raw data from database.");
            List<byte[]> rawBytes
                    = ModelMgr.getModelMgr().getB64DecodedEntityDataValues(
                            workspace.getId(),
                            EntityConstants.ATTRIBUTE_PROTOBUF_NEURON
                    );

            progressHandle.progress(1);
            //final double workUnitMultiplier = TOTAL_WORKUNITS / rawBytes.size();
            
            // Turn those buffers into model objects.
            ExecutorService executor = ThreadUtils.establishExecutor(
                    DESERIALIZATION_THREAD_COUNT,
                    new CustomNamedThreadFactory("Client-Deserialize-Neuron")
            );
            final List<TmNeuron> neurons = Collections.synchronizedList(new ArrayList<TmNeuron>(rawBytes.size()));            
            progressHandle.setDisplayName("Building neurons...");
            List<Future<Void>> fates = new ArrayList<>();
            //final Progressor progressor = new Progressor(progressHandle, workUnitMultiplier, rawBytes.size());
            log.info("Starting the neuron exchange.");
            for (final byte[] rawBuffer : rawBytes) {
                Callable<Void> callable = new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        neurons.add(exchanger.deserializeNeuron(rawBuffer));
                        return null;
                    }
                };
                fates.add(executor.submit(callable));
            }
            progressHandle.progress(2);

            // Await completion.
            ThreadUtils.followUpExecution(executor, fates, MAX_WAIT_MIN);
            log.info("Neuron exchange complete.");
            progressHandle.setDisplayName("Populating viewer...");
            workspace.setNeuronList(neurons);
            progressHandle.progress(3);
            progressHandle.finish();
        } catch (Exception ex) {
            progressHandle.finish();
            throw ex;
        }
    }

    /**
     * Pushes neuron as entity data, to database.
     * 
     * @param neuron internal-to-LVV object.
     * @throws Exception 
     */
    @Override
    public void saveNeuron(TmNeuron neuron) throws Exception {
        Entity workspaceEntity = ensureWorkspaceEntity(neuron.getWorkspaceId());

        // Need to make serializable version of the data.
        byte[] serializableBytes = exchanger.serializeNeuron(neuron);
        
        // May need to exchange this entity-data for existing one on workspace
        EntityData preExistingEntityData = null;
        if (neuron != null  &&  neuron.getId() != null) {
            for (EntityData edata : workspaceEntity.getEntityData()) {
                log.debug("Comparing neuron {} to entity data {}.", neuron.getId(), edata.getId());
                if (edata.getId() == null) {
                    log.warn("No id in entity data. {}", edata);
                }
                else if (edata.getId().equals(neuron.getId())) {
                    preExistingEntityData = edata;
                    break;
                }
            }
        }

        // Must now push a new entity data
        EntityData entityData = new EntityData();
        entityData.setOwnerKey(neuron.getOwnerKey());
        entityData.setCreationDate(neuron.getCreationDate());
        entityData.setId(neuron.getId());  // May have been seeded as null.
        // Avoid transmitting siblings over the wire.
        Entity nullParent = new Entity();
        nullParent.setEntityTypeName(workspaceEntity.getEntityTypeName());
        nullParent.setCreationDate(workspaceEntity.getCreationDate());
        nullParent.setName(workspaceEntity.getName());
        nullParent.setOwnerKey(workspaceEntity.getOwnerKey());
        nullParent.getEntityData().add(entityData);
        nullParent.setId(workspaceEntity.getId());
        entityData.setParentEntity(nullParent);
        
        // Encoding on the client side for convenience: the save-or-update
        // method already exists.  We expect to see this carried out one
        // neuron (or two) at a time, not wholesale.
		// @todo is there real danger of this being removed?
        BASE64Encoder encoder = new BASE64Encoder();
        entityData.setValue(encoder.encode(serializableBytes));
        entityData.setEntityAttrName(EntityConstants.ATTRIBUTE_PROTOBUF_NEURON);
        EntityData savedEntityData = ModelMgr.getModelMgr().saveOrUpdateEntityData(entityData);
        // Back-fill the corrected workspace entity.
        savedEntityData.setParentEntity(workspaceEntity);        if (preExistingEntityData != null) {
            workspaceEntity.getEntityData().remove(preExistingEntityData);
        }
        workspaceEntity.getEntityData().add(savedEntityData);
        neuron.setId(savedEntityData.getId());
    }

    @Override
    public TmNeuron refreshFromEntityData(TmNeuron neuron) throws Exception {
        Long workspaceId = neuron.getWorkspaceId();
        Long neuronId = neuron.getId();
        ensureWorkspaceEntity(workspaceId);
        byte[] rawBuffer = 
            ModelMgr
                .getModelMgr()
                .getB64DecodedEntityDataValue(
                        workspaceId, 
                        neuronId, 
                        EntityConstants.ATTRIBUTE_PROTOBUF_NEURON
                );
        return exchanger.deserializeNeuron(rawBuffer, neuron);
    }

    @Override
    public void deleteEntityData(TmNeuron neuron) throws Exception {
        // The lower levels of the hierarchy require an Entity Data to delete
        // but they use only its id and owner key.
        EntityData ed = new EntityData();
        ed.setId(neuron.getId());
        ed.setOwnerKey(neuron.getOwnerKey());
        ModelMgr.getModelMgr().removeEntityData(ed);
    }
    
    private Entity ensureWorkspaceEntity(Long workspaceId) throws Exception {
        Entity workspaceEntity = null;
        if (workspaceId == null) {
            throw new Exception("No workspace provided.");
        }
        else {
            workspaceEntity = wsIdToEntity.get(workspaceId);
            if (workspaceEntity == null) {
                workspaceEntity = ModelMgr.getModelMgr().getEntityById(workspaceId);
                if (workspaceEntity == null) {
                    throw new Exception("Failed to find entity for id " + workspaceId);
                } else if (!workspaceEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                    throw new Exception("Workspace ID supplied is not for a workspace: rather it is a " + workspaceEntity.getEntityTypeName());
                } else {
                    wsIdToEntity.put(workspaceId, workspaceEntity);
                }
            }
        }
        
        return workspaceEntity;
    }
    
    /*
    private class Progressor {
        private ProgressHandle progressHandle;
        private double workUnitMultiplier;
        private int failedStatusUpdateCount;
        private int max;
        private AtomicInteger counter;
        private int highestCount = 0;
        private Date latestUpdateTime = new Date();  // Seed at construction.
        
        public Progressor(ProgressHandle progressHandle, double workUnitMultiplier, int max) {
            this.progressHandle = progressHandle;
            this.workUnitMultiplier = workUnitMultiplier;
            this.counter = new AtomicInteger(0);
            this.max = max;
        }
        public synchronized void exec() {
            try {
                final int count = counter.incrementAndGet();
                // Update the progress one time per second.
                Date currentDate = new Date();
                if (currentDate.getTime() - 100 > latestUpdateTime.getTime()) {
                    Runnable r = new Runnable() {
                        public void run() {
                            progressHandle.progress(
                                    getProgressValue(count)
                            );
                        }
                    };
                    //WindowManager.getDefault().invokeWhenUIReady(r);
                    r.run();
                    log.info("Updating at count " + count);
                    //Thread.currentThread().sleep(10); // Respite for progress.
                    latestUpdateTime = currentDate;
                }
                
            } catch (Exception ex) {
                // Eat this.
                failedStatusUpdateCount++;
            }
        }
        public void report() {
            log.info("Failed to update status " + 100.0 * ((double) failedStatusUpdateCount / (double) max) + "% of time.");
        }
        
        private int getProgressValue(int count) {
            int rtnVal = (int) (workUnitMultiplier * count);
            return rtnVal;
        }
    }
    */
}
