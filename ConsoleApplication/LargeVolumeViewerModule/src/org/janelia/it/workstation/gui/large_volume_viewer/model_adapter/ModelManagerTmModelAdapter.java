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
    public static final int MAX_WAIT_MIN = 20;
    
    private final Map<Long,Entity> wsIdToEntity = new HashMap<>();

    private final TmProtobufExchanger exchanger = new TmProtobufExchanger();
    
    private Logger log = LoggerFactory.getLogger(ModelManagerTmModelAdapter.class);
    
    @Override
    public void loadNeurons(TmWorkspace workspace) throws Exception {
        // Obtain the serialized version of the data as raw byte buffers.
        List<byte[]> rawBytes = 
                ModelMgr.getModelMgr().getB64DecodedEntityDataValues(
                        workspace.getId(),
                        EntityConstants.ATTRIBUTE_PROTOBUF_NEURON
                );
        
        // Turn those buffers into model objects.
        ExecutorService executor = ThreadUtils.establishExecutor(
                DESERIALIZATION_THREAD_COUNT, 
                new CustomNamedThreadFactory("Client-Deserialize-Neuron")
        );
        final List<TmNeuron> neurons = Collections.synchronizedList(new ArrayList<TmNeuron>(rawBytes.size()));
        List<Future<Void>> fates = new ArrayList<>();
        for (final byte[] rawBuffer: rawBytes) {
            Callable<Void> callable = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    neurons.add(exchanger.deserializeNeuron(rawBuffer));
                    return null;
                }
            };
            fates.add( executor.submit(callable) );
        }
        
        // Await completion.
        ThreadUtils.followUpExecution(executor, fates, MAX_WAIT_MIN);
		workspace.setNeuronList(neurons);
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
        for (EntityData edata: workspaceEntity.getEntityData()) {
            log.info("Comparing neuron {} to entity data {}.", neuron.getId(), edata.getId());
            if (edata.getId().equals(neuron.getId())) {
                preExistingEntityData = edata;
                break;
            }
        }

        // Must now push a new entity data
        EntityData entityData = new EntityData();
        entityData.setOwnerKey(neuron.getOwnerKey());
        entityData.setCreationDate(neuron.getCreationDate());
        entityData.setId(neuron.getId());  // May have been seeded as null.
        entityData.setParentEntity(workspaceEntity);
        // Encoding on the client side for convenience: the save-or-update
        // method already exists.  We expect to see this carried out one
        // neuron (or two) at a time, not wholesale.
		// @todo is there real danger of this being removed?
        BASE64Encoder encoder = new BASE64Encoder();
        entityData.setValue(encoder.encode(serializableBytes));
        entityData.setEntityAttrName(EntityConstants.ATTRIBUTE_PROTOBUF_NEURON);
        EntityData savedEntityData = ModelMgr.getModelMgr().saveOrUpdateEntityData(entityData);
        if (preExistingEntityData != null) {
            workspaceEntity.getEntityData().remove(preExistingEntityData);
        }
        workspaceEntity.getEntityData().add(entityData);
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
}
