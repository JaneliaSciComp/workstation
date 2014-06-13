package org.janelia.it.workstation.gui.framework.viewer;

import java.util.List;
import java.util.concurrent.Callable;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for icon panels.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class IconPanel extends Viewer {

    private static final Logger log = LoggerFactory.getLogger(IconPanel.class);

    private String currImageRole = EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE;

    protected ImagesPanel imagesPanel;

    protected IconPanel(ViewerPane viewerPane) {
        super(viewerPane);
    }

    public ImagesPanel getImagesPanel() {
        return imagesPanel;
    }

    public String getCurrImageRole() {
        return currImageRole;
    }

    public void setCurrImageRole(String currImageRole) {
        this.currImageRole = currImageRole;
    }

    public boolean areTitlesVisible() {
        return true;
    }

    public boolean areTagsVisible() {
        return true;
    }

    /**
     * Clear the view.
     */
    @Override
    public abstract void clear();

    /**
     * Clear the view and display a loading indicator.
     */
    @Override
    public abstract void showLoadingIndicator();

    /**
     * Display the given RootedEntity in the viewer.
     *
     * @param rootedEntity
     */
    @Override
    public abstract void loadEntity(RootedEntity rootedEntity);

    /**
     * Display the given RootedEntity in the viewer, and then call the callback.
     *
     * @param rootedEntity
     * @param success
     */
    @Override
    public abstract void loadEntity(RootedEntity rootedEntity, final Callable<Void> success);

    /**
     * Returns all RootedEntity objects loaded in the viewer.
     *
     * @return
     */
    @Override
    public abstract List<RootedEntity> getRootedEntities();

    /**
     * Returns all RootedEntity objected which are currently selected in the viewer.
     *
     * @return
     */
    @Override
    public abstract List<RootedEntity> getSelectedEntities();

    /**
     * Returns the RootedEntity with the given uniqueId, assuming that its currently loaded in the viewer.
     *
     * @param uniqueId
     * @return
     */
    @Override
    public abstract RootedEntity getRootedEntityById(String uniqueId);

    /**
     * Called when the viewer is about to close forever. This is an opportunity to clean up any listeners or
     * open resources.
     */
    @Override
    public abstract void close();
}
