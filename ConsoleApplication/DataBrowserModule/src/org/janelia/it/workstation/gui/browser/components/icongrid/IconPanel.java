package org.janelia.it.workstation.gui.browser.components.icongrid;

import javax.swing.JPanel;

import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Base class for icon panels.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class IconPanel<T,S> extends JPanel {

    private String currImageRole = EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE;

    protected IconPanel() {
    }

    public String getCurrImageRole() {
        return currImageRole;
    }

    public void setCurrImageRole(String currImageRole) {
        this.currImageRole = currImageRole;
    }

    public abstract boolean areTitlesVisible();

    public abstract boolean areTagsVisible();
    
    public abstract T getImageByUniqueId(S id);
    
    public abstract S getImageUniqueId(T imageObject);
    
    public abstract String getImageFilepath(T imageObject);
    
    public abstract String getImageFilepath(T imageObject, String role);
    
    public abstract Object getImageLabel(T imageObject);

    public abstract void registerAspectRatio(double aspectRatio);

    public abstract int getMaxImageWidth();
    
}
