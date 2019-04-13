package org.janelia.horta.nodes;

import org.janelia.console.viewerapi.model.BasicNeuronSet;
import java.util.ArrayList;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;

/**
 * Convenience methods I don't want to place into lean HortaWorkspace API
 * @author Christopher Bruns
 */
public class WorkspaceUtil
{
    private final HortaMetaWorkspace workspace;

    public WorkspaceUtil(HortaMetaWorkspace workspace) {
        this.workspace = workspace;
    }
    
    public NeuronSet getOrCreateTemporaryNeuronSet() {
        NeuronSet result = getTemporaryNeuronSetOrNull();
        if (result != null)
            return result;
        final String nameOfSet = "Temporary Neurons";
        result = new BasicNeuronSet(nameOfSet, new ArrayList<NeuronModel>());
        workspace.getNeuronSets().add(result);
        workspace.setChanged();
        return result;
    }
    
    public NeuronSet getTemporaryNeuronSetOrNull() {
        final String nameOfSet = "Temporary Neurons";
        for (NeuronSet neuronSet : workspace.getNeuronSets()) {
            if (neuronSet.getName().equals(nameOfSet))
                return neuronSet;
        }
        return null;
    }
    
    // Convenience function for use after dragging a lone SWC onto the viewer.
    public void addNeuronAndNotify(NeuronModel neuron) {
        boolean bWorkspaceChanged = false;
        boolean bNeuronSetChanged = false;
        if (workspace.getNeuronSets().isEmpty()) {
            NeuronSet localNeurons = new BasicNeuronSet("Temporary Neurons", new ArrayList<NeuronModel>());
            workspace.getNeuronSets().add(localNeurons);
            workspace.setChanged();
            bWorkspaceChanged = true;
        }
        // Drop neuron into first list of neurons, when dropping on whole workspace
        NeuronSet neuronList = workspace.getNeuronSets().iterator().next();
        bNeuronSetChanged = neuronList.add(neuron);
        if (bWorkspaceChanged)
            workspace.notifyObservers();        
        if (bNeuronSetChanged)
            neuronList.getMembershipChangeObservable().notifyObservers();
    }
}
