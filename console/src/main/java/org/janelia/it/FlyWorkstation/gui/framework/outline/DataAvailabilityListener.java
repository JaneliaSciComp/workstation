package org.janelia.it.FlyWorkstation.gui.framework.outline;

/**
 * Listener for data events.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DataAvailabilityListener extends java.util.EventListener {
    /**
     * This method gets called when data becomes available.
     * <p/>
     *
     * @param evt A DataReadyEvent object describing the event source.
     */
    void dataReady(DataReadyEvent evt);
}