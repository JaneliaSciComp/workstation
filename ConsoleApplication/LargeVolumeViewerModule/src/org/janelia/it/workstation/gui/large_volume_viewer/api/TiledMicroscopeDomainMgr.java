package org.janelia.it.workstation.gui.large_volume_viewer.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.user_data.tiled_microscope_protobuf.TmProtobufExchanger;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton for managing the Tiled Microscope Domain Model and related data access.
 * 
 * Translates in Mongo-land domain objects to the existing TM classes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TiledMicroscopeDomainMgr {

    private static final Logger log = LoggerFactory.getLogger(TiledMicroscopeDomainMgr.class);

    // Singleton
    private static TiledMicroscopeDomainMgr instance;

    public static TiledMicroscopeDomainMgr getDomainMgr() {
        if (instance==null) {
            instance = new TiledMicroscopeDomainMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    private DomainModel model;


    /**
     * Returns a lazy domain model instance. 
     * @return domain model
     */
    public DomainModel getModel() {
        if (model == null) {
            model = DomainMgr.getDomainMgr().getModel();
        }
        return model;
    }

    public Collection<TmSample> getTmSamples() throws Exception {
        List<TmSample> samples = new ArrayList<>();
        for(org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample domainSample : model.getTmSamples()) {
            TmSample sample = new TmSample(domainSample.getId(), domainSample.getName(), domainSample.getCreationDate(), domainSample.getFilepath());
            samples.add(sample);
        }
        return samples;
    }

    public TmSample save(TmSample tmSample) throws Exception {

        org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample domainObject = null;
        if (tmSample.getId()!=null) {
            domainObject = model.getDomainObject(org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample.class, tmSample.getId());
        }

        return null;
    }

    public void remove(TmSample tmSample) throws Exception {
        throw new UnsupportedOperationException();
    }

    public Collection<TmWorkspace> getTmWorkspaces() throws Exception {
        throw new UnsupportedOperationException();
    }

    public TmWorkspace create(TmWorkspace tmWorkspace) throws Exception {
        throw new UnsupportedOperationException();
    }

    public TmWorkspace update(TmWorkspace tmWorkspace) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void remove(TmWorkspace tmWorkspace) throws Exception {
        throw new UnsupportedOperationException();
    }

    public List<TmNeuron> getWorkspaceNeurons(Long workspaceId) throws Exception {
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        List<TmNeuron> neurons = new ArrayList<>();
        for(Pair<TmNeuronMetadata,InputStream> pair : model.getWorkspaceNeuronPairs(workspaceId)) {
            TmNeuronMetadata neuronMetadata = pair.getLeft();
            TmNeuron tmNeuron = exchanger.deserializeNeuron(pair.getRight());
            // TODO: Should these metadata be serialized with protobuf at all? Probably not!
            // But since they are, let's verify that they're consistent.
            if (!tmNeuron.getId().equals(neuronMetadata.getId())) {
                log.error("Neuron's metadata (id) does not match serialized data: "+tmNeuron.getId());
            }
            if (!tmNeuron.getOwnerKey().equals(neuronMetadata.getOwnerKey())) {
                log.error("Neuron's metadata (ownerKey) does not match serialized data: "+tmNeuron.getId());
            }
            if (!tmNeuron.getWorkspaceId().equals(neuronMetadata.getWorkspaceRef().getTargetId())) {
                log.error("Neuron's metadata (ownerKey) does not match serialized data: "+tmNeuron.getId());
            }
            neurons.add(tmNeuron);
        }
        return neurons;
    }

    public TmNeuronMetadata create(TmNeuron tmNeuron) throws Exception {
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        TmNeuronMetadata neuronMetadata = new TmNeuronMetadata();
        Long newId = TimebasedIdentifierGenerator.generateIdList(1).get(0);
        tmNeuron.setId(newId);
        tmNeuron.setOwnerKey(AccessManager.getSubjectKey());
        neuronMetadata.setId(tmNeuron.getId());
        neuronMetadata.setOwnerKey(tmNeuron.getOwnerKey());
        neuronMetadata.setWorkspaceRef(Reference.createFor(TmWorkspace.class, tmNeuron.getWorkspaceId()));
        byte[] protobufBytes = exchanger.serializeNeuron(tmNeuron);
        model.create(neuronMetadata, new ByteArrayInputStream(protobufBytes));
        return neuronMetadata;
    }

    public TmNeuronMetadata update(TmNeuron tmNeuron) throws Exception {
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        TmNeuronMetadata neuronMetadata = new TmNeuronMetadata();
        neuronMetadata.setId(tmNeuron.getId());
        neuronMetadata.setOwnerKey(tmNeuron.getOwnerKey());
        neuronMetadata.setWorkspaceRef(Reference.createFor(TmWorkspace.class, tmNeuron.getWorkspaceId()));
        byte[] protobufBytes = exchanger.serializeNeuron(tmNeuron);
        model.update(neuronMetadata, new ByteArrayInputStream(protobufBytes));
        return neuronMetadata;
    }

    public void remove(TmNeuron tmNeuron) throws Exception {
        TmNeuronMetadata neuronMetadata = new TmNeuronMetadata();
        neuronMetadata.setId(tmNeuron.getId());
        model.remove(neuronMetadata);
    }



}
