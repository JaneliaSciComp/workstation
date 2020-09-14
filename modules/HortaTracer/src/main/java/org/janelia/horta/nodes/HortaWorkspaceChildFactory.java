package org.janelia.horta.nodes;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import com.google.common.eventbus.Subscribe;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.horta.NeuronTracerTopComponent;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.MeshCreateEvent;
import org.janelia.workstation.controller.eventbus.MeshUpdateEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

/**
 *
 * @author Christopher Bruns
 */
class HortaWorkspaceChildFactory extends ChildFactory {

    private final TmWorkspace workspace;
    private final Collection<TmObjectMesh> meshActors = new HashSet<>();

    public HortaWorkspaceChildFactory(List<TmObjectMesh> meshList) {
        this.workspace = TmModelManager.getInstance().getCurrentWorkspace();
        this.meshActors.addAll(meshList);
        ViewerEventBus.registerForEvents(this);
    }

    @Subscribe
    public void meshCreated (MeshCreateEvent event) {
        List<TmObjectMesh> meshes = event.getMeshes();
        this.meshActors.addAll(meshes);
        refresh(false);
    }

    @Subscribe
    public void meshDeleted (MeshCreateEvent event) {
        this.meshActors.removeAll(event.getMeshes());
        refresh(false);
    }

    @Override
    protected boolean createKeys(List toPopulate) {
        for (TmObjectMesh meshActor : meshActors) {
            toPopulate.add(meshActor);
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(Object key) {
        if (key instanceof TmObjectMesh) {
            return new MeshNode((TmObjectMesh) key);
        }
        return null;
    }
}
