package org.janelia.workstation.core.util;

/**
 * Interface for refreshable things. It could be anything, but it's likely to be a view.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface Refreshable {

    /** Refresh the thing */
    void refresh();
}
