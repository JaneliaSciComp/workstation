package org.janelia.it.workstation.gui.framework.outline;

import java.util.EventListener;

/**
 * Listener for data events.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DataAvailabilityListener extends EventListener {
    /**
     * This method gets called when data becomes available.
     * <p/>
     *
     * @param evt A DataReadyEvent object describing the event source.
     */
    void dataReady(DataReadyEvent evt);
}