package org.janelia.workstation.integration.framework.system;

/**
 * Service for showing detailed progress information as part of the main GUI.
 * This is separate from NetBean's ProgressHandleFactory.createHandle facility.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ProgressHandler {

    public static final String LOOKUP_PATH = "Handlers/ProgressHandler";

    void showProgressPanel();
}
