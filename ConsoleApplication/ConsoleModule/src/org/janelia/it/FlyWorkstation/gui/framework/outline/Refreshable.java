package org.janelia.it.FlyWorkstation.gui.framework.outline;

/**
 * Something that can be refreshed.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface Refreshable {

    /**
     * Refresh the display.
     */
    public void refresh();

    /**
     * Refresh the display with the latest data from the database.
     */
    public void totalRefresh();
}
