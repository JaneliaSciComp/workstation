package org.janelia.it.workstation.gui.browser.icongrid;

import javax.swing.JPanel;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for icon panels.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class IconPanel<T> extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(IconPanel.class);

    private String currImageRole = EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE;

    protected ImagesPanel<T> imagesPanel;

    protected IconPanel() {
    }

    public ImagesPanel<T> getImagesPanel() {
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

    public String getSelectionCategory() {
        return EntitySelectionModel.CATEGORY_MAIN_VIEW;
    }
    
    public abstract T getContextObject();
    
    public abstract void setContextObject(T contextObject);
    
    public abstract Object getImageUniqueId(T imageObject);
    
    public abstract String getImageFilepath(T imageObject);
    
    public abstract String getImageFilepath(T imageObject, String role);
    
    public abstract Object getImageLabel(T imageObject);
    
}
