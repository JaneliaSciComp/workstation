package org.janelia.it.workstation.browser.gui.support;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mix-in interface for adding preference support to a viewer. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface PreferenceSupport {

    static final Logger log = LoggerFactory.getLogger(PreferenceSupport.class);

    Long getCurrentParentId();
    
    void refreshView();
    
    /**
     * Sets the value for the given category for the current parent object.
     * Runs in a background thread and calls refreshView() when it's done.
     * @param category
     * @param value
     */
    default void setPreferenceAsync(final String category, final Object value) {

        Utils.setMainFrameCursorWaitStatus(true);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                setPreference(category, value);
            }

            @Override
            protected void hadSuccess() {
                Utils.setMainFrameCursorWaitStatus(false);
                refreshView();
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setMainFrameCursorWaitStatus(false);
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }
    
    /**
     * Sets the value for the given category for the current parent object.
     * Should be called in a background thread.
     * @param category
     * @param value
     * @throws Exception
     */
    default void setPreference(final String category, final Object value) throws Exception {
        if (getCurrentParentId()==null) return;
        FrameworkImplProvider.setRemotePreferenceValue(category, getCurrentParentId().toString(), value);
    }
    
    /**
     * Retrieves the value for the preference of the given category for the current parent object.
     * Should be called in a background thread.
     * @param category
     * @return
     */
    default String getPreference(String category) {
        if (getCurrentParentId()==null) return null;
        try {
            return FrameworkImplProvider.getRemotePreferenceValue(category, getCurrentParentId().toString(), null);
        }
        catch (Exception e) {
            log.error("Error getting preference", e);
            return null;
        }
    }
}
