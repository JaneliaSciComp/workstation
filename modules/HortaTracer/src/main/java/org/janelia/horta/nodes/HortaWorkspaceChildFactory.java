package org.janelia.horta.nodes;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.horta.NeuronTracerTopComponent;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

/**
 *
 * @author Christopher Bruns
 */
class HortaWorkspaceChildFactory extends ChildFactory {

    private final HortaMetaWorkspace workspace;
    private final Collection<MeshActor> meshActors = new HashSet<>();
    private final Observer refresher;

    public HortaWorkspaceChildFactory(HortaMetaWorkspace workspace, List<MeshActor> meshActorList, ObservableInterface meshObserver) {
        this.workspace = workspace;
        this.meshActors.addAll(meshActorList);
        refresher = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                refresh(false);
            }
        };
        workspace.addObserver(refresher);

        meshObserver.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                meshActors.clear();
                NeuronTracerTopComponent hortaTracer = NeuronTracerTopComponent.getInstance();
                meshActors.addAll(hortaTracer.getMeshActors());
                refresh(false);
            }
        });
    }

    @Override
    protected boolean createKeys(List toPopulate) {
        for (GL3Actor meshActor : meshActors) {
            toPopulate.add(meshActor);
        }
        for (NeuronSet neuronList : workspace.getNeuronSets()) {
            // Only show neuron lists with, you know, neurons in them.
            neuronList.getMembershipChangeObservable().deleteObserver(refresher);
            if (neuronList.size() == 0) {
                // Listen for changes to empty neuron list content
                neuronList.getMembershipChangeObservable().addObserver(refresher);
            } else {
                toPopulate.add(neuronList);
            }
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(Object key) {
        if (key instanceof NeuronSet) {
            return new NeuronSetNode((NeuronSet) key);
        } else if (key instanceof MeshActor) {
            return new MeshNode((MeshActor) key);
        }
        return null;
    }
}
