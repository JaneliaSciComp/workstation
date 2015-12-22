/**
 * Implementation of the model adapter, which pulls/pushes data through
 * the Model Manager.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.model_adapter;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.user_data.tiled_microscope_builder.TmModelAdapter;

/**
 *
 * @author fosterl
 */
public class ModelManagerTmModelAdapter implements TmModelAdapter {
    public void loadNeurons(TmWorkspace workspace) throws Exception {
        
    }

    public void saveNeuron(TmNeuron neuron) throws Exception {
        
    }

    public TmNeuron refreshFromEntityData(TmNeuron neuron) throws Exception {
        return null;
    }

    public void deleteEntityData(Long entityId) throws Exception {
        
    }
}
