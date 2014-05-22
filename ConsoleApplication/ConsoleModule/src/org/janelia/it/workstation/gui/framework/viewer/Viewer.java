package org.janelia.it.workstation.gui.framework.viewer;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JPanel;
import org.janelia.it.workstation.gui.framework.outline.EntityViewerState;

/**
 * A viewer panel that is refreshable and can be placed inside a ViewerPane.
 *
 * A viewer must also be able to lookup and return the Entities and RootedEntities that it is currently displaying.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class Viewer extends JPanel implements org.janelia.it.workstation.gui.framework.outline.Refreshable {

    private final ViewerPane viewerPane;

    public Viewer(final ViewerPane viewerPane) {
        this.viewerPane = viewerPane;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setAsActive();
            }
        });
    }

    public String getSelectionCategory() {
        return viewerPane.getSelectionCategory();
    }

    public void setAsActive() {
        if (!viewerPane.isActive()) {
            org.janelia.it.workstation.model.entity.RootedEntity contextRootedEntity = getContextRootedEntity();
            if (contextRootedEntity != null) {
                org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser().getEntityOutline().highlightEntityByUniqueId(contextRootedEntity.getId());
            }
        }
        viewerPane.setAsActive();
    }

    public ViewerPane getViewerPane() {
        return viewerPane;
    }

    public abstract org.janelia.it.workstation.model.entity.RootedEntity getContextRootedEntity();

    /**
     * Clear the view.
     */
    public abstract void clear();

    /**
     * Clear the view and display a loading indicator.
     */
    public abstract void showLoadingIndicator();

    /**
     * Display the given RootedEntity in the viewer.
     */
    public abstract void loadEntity(org.janelia.it.workstation.model.entity.RootedEntity rootedEntity);

    /**
     * Display the given RootedEntity in the viewer, and then call the callback.
     */
    public abstract void loadEntity(org.janelia.it.workstation.model.entity.RootedEntity rootedEntity, final Callable<Void> success);

    /**
     * Returns all RootedEntity objects loaded in the viewer.
     *
     * @return
     */
    public abstract List<org.janelia.it.workstation.model.entity.RootedEntity> getRootedEntities();

    /**
     * Returns all RootedEntity objected which are currently selected in the viewer.
     *
     * @return
     */
    public abstract List<org.janelia.it.workstation.model.entity.RootedEntity> getSelectedEntities();

    /**
     * Returns the RootedEntity with the given uniqueId, assuming that its currently loaded in the viewer.
     */
    public abstract org.janelia.it.workstation.model.entity.RootedEntity getRootedEntityById(String uniqueId);

    /**
     * Called when the viewer is about to close forever. This is an opportunity to clean up any listeners or
     * open resources.
     */
    public abstract void close();
    
    /**
     * Called when the viewer pane is about to navigate to a new entity.
     */
    public abstract EntityViewerState saveViewerState();
    
    /**
     * Called when the state is being restored by a navigation action.
     */
    public abstract void restoreViewerState(EntityViewerState state);

}
