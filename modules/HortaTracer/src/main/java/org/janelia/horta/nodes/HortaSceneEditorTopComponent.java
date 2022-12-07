package org.janelia.horta.nodes;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.action.LoadMeshAction;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.workstation.controller.model.TmModelManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
        LoadMeshAction loadMeshAction = new LoadMeshAction();
        final JPopupMenu meshToolMenu = new JPopupMenu();
        meshToolMenu.add(loadMeshAction);

        final JButton meshToolButton = new JButton();
        String gearIconFilename = "cog.png";
        ImageIcon gearIcon = Icons.getIcon(gearIconFilename);
        meshToolButton.setIcon(gearIcon);
        meshToolButton.setHideActionText(true);
        meshToolButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                meshToolMenu.show(meshToolButton,
                        0,
                        meshToolButton.getBounds().height);
            }
        });
        add(meshToolButton);

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
