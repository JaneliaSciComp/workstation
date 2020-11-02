package org.janelia.horta.nodes;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Observable;

import com.google.common.eventbus.Subscribe;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;
import org.janelia.gltools.GL3Actor;
import org.janelia.horta.NeuronTracerTopComponent;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.workstation.controller.model.TmModelManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.OutlineView;
import org.openide.explorer.view.TreeTableView;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import static org.janelia.horta.nodes.HortaSceneEditorTopComponent.LABEL_TEXT;

/**
 *
 * @author Christopher Bruns
 */
@TopComponent.Description(
        preferredID = HortaSceneEditorTopComponent.PREFERRED_ID,
        iconBase = "org/janelia/horta/images/brain-icon2.png",
        persistenceType = TopComponent.PERSISTENCE_NEVER)
@TopComponent.Registration( 
        mode = "properties", 
        openAtStartup = false) 
@ActionID( 
        category = "Window", id = "org.janelia.horta.nodes.HortaSceneEditorTopComponent") 
@ActionReference( 
        path = "Menu/Window/Horta", position = 300) 
@TopComponent.OpenActionRegistration( 
        displayName = "#CTL_HortaSceneEditorAction",
        preferredID = HortaSceneEditorTopComponent.PREFERRED_ID
) 
@NbBundle.Messages({
    "CTL_HortaSceneEditorAction=Scene Editor",
    "CTL_HortaSceneEditorTopComponent="+HortaSceneEditorTopComponent.LABEL_TEXT,
    "HINT_HortaSceneEditorTopComponent=Horta Scene Editor"
})
public class HortaSceneEditorTopComponent extends TopComponent {
    private JScrollPane scrollPane;
    private JPanel mainPanel;
    private ObjectMeshPanel meshInfoPanel;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String PREFERRED_ID = "HortaSceneEditorTopComponent";
    public static final String LABEL_TEXT = "Scene Editor";
    /**
     * Creates new form HortaWorkspaceEditorTopComponent
     */
    public HortaSceneEditorTopComponent()
    {
        setName("Scene Editor");
        mainPanel = new JPanel();
        meshInfoPanel = new ObjectMeshPanel();
        scrollPane = new JScrollPane(mainPanel);
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        add(scrollPane);

        mainPanel.add(meshInfoPanel);
        ViewerEventBus.registerForEvents(this);

        // if this component only got opened after the workspace was loaded, initialize
        if (TmModelManager.getInstance().getCurrentWorkspace()!=null)
            meshInfoPanel.loadWorkspace(TmModelManager.getInstance().getCurrentWorkspace());
    }
    
    @Override
    public void componentOpened() {

    }
    
    @Override 
    public void componentClosed() {

    }
    
    @Subscribe
    public void workspaceLoaded(LoadProjectEvent loadEvent) {
        TmWorkspace workspace = loadEvent.getWorkspace();
        if (workspace!=null) {
            logger.info("Loading meshes into object mesh list");
            meshInfoPanel.loadWorkspace(workspace);
        }
    }

    @Subscribe
    public void meshAdded(MeshCreateEvent addEvent) {
        for (TmObjectMesh mesh: addEvent.getMeshes()) {
            meshInfoPanel.addObjectMeshToTable(mesh);
        }
    }
}
