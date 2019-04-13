package org.janelia.console.viewerapi.model;

/**
 *
 * @author Christopher Bruns
 */
public interface Hideable
{
    boolean isVisible();
    void setVisible(boolean visible);
}
