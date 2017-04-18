package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronTagMap;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.user_data.tiled_microscope_builder.TmModelManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkspaceAnnotationModel extends AnnotationModel {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceAnnotationModel.class);

    private TmModelManipulator<TmWorkspace> neuronManager;
    
    public WorkspaceAnnotationModel(TmModelManipulator<TmWorkspace> neuronManager) {
        super(neuronManager);
        this.neuronManager = neuronManager;
    }
    
    public synchronized void loadWorkspace(final TmWorkspace workspace) throws Exception {
        if (workspace == null) {
            throw new IllegalArgumentException("Cannot load null workspace");
        }
                
        log.info("Loading workspace {}", workspace.getId());
        currentWorkspace = workspace;
        currentSample = tmDomainMgr.getSample(workspace);

        // Neurons need to be loaded en masse from raw data from server.
        log.info("Loading neurons for workspace {}", workspace.getId());
        neuronManager.loadWorkspaceNeurons(workspace);

        // Create the local tag map for cached access to tags
        log.info("Creating tag map for workspace {}", workspace.getId());
        currentTagMap = new TmNeuronTagMap();
        for(TmNeuronMetadata tmNeuronMetadata : neuronManager.getNeurons()) {
            for(String tag : tmNeuronMetadata.getTags()) {
                currentTagMap.addTag(tag, tmNeuronMetadata);
            }
        }
        
        // Clear neuron selection
        log.info("Clearing current neuron for workspace {}", workspace.getId());
        setCurrentNeuron(null);   
    }
}
