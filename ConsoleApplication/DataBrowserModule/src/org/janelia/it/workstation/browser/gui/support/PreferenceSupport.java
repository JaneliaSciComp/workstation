package org.janelia.it.workstation.browser.gui.support;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.SimpleListenableFuture;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mix-in interface for adding preference support to a viewer. These preferences are kept in the remote
 * database, shared for all installations. When setting or retrieving a preference, it can be contextual 
 * to a dynamic GUID which is provided by the implementation, or in a custom category. 
 * 
 * For example, a preference may be contextual to the folder currently being viewed. In one folder, users may 
 * want to see signal MIPs, but in another they may want to see reference MIPs. In this case, the MIP type 
 * preference would be set with the contextId set to the current folderId.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface PreferenceSupport {

    static final Logger log = LoggerFactory.getLogger(PreferenceSupport.class);

    /**
     * Implement this to return the current context id to use when saving a preference. This method is called every time 
     * a preference is saved or retrieved, so the return value may change over time if the context changes. 
     * @return GUID for the current context for storing preferences
     */
    Long getCurrentContextId();
        
    /**
     * Sets the value for the given category for the current parent object.
     * Runs in a background thread.
     * @param category
     * @param value
     * @return listenable future
     */
    default SimpleListenableFuture<Void> setPreferenceAsync(final String category, final Object value) {
        if (getCurrentContextId()==null) return null;
        return setPreferenceAsync(category, getCurrentContextId().toString(), value);
    }
    
    /**
     * Sets the value for the given category for the current parent object.
     * Should be called in a background thread.
     * @param category
     * @param value
     * @throws Exception
     */
    default void setPreference(final String category, final Object value) throws Exception {
        if (getCurrentContextId()==null) return;
        setPreference(category, getCurrentContextId().toString(), value);
    }

    /**
     * Sets the value for the given category for the current parent object.
     * Runs in a background thread.
     * @param category
     * @param value
     * @return listenable future
     */
    default SimpleListenableFuture<Void> setPreferenceAsync(final String category, final String key, final Object value) {

        Utils.setMainFrameCursorWaitStatus(true);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                setPreference(category, key, value);
            }

            @Override
            protected void hadSuccess() {
                Utils.setMainFrameCursorWaitStatus(false);
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setMainFrameCursorWaitStatus(false);
                ConsoleApp.handleException(error);
            }
        };

        return worker.executeWithFuture();
    }
    
    /**
     * Sets the value for the given category for the current parent object.
     * Should be called in a background thread.
     * @param category
     * @param value
     * @throws Exception
     */
    default void setPreference(final String category, final String key, final Object value) throws Exception {
        FrameworkImplProvider.setRemotePreferenceValue(category, key, value);
    }
    
    /**
     * Retrieves the value for the preference of the given category for the current parent object.
     * Should be called in a background thread.
     * @param category
     * @return
     */
    default String getPreference(String category) {
        if (getCurrentContextId()==null) return null;
        try {
            return FrameworkImplProvider.getRemotePreferenceValue(category, getCurrentContextId().toString(), null);
        }
        catch (Exception e) {
            log.error("Error getting preference", e);
            return null;
        }
    }
    
    /**
     * Retrieves the value for the preference of the given category for the current parent object.
     * Should be called in a background thread.
     * @param category
     * @return
     */
    default <T> T getPreference(String category, String key, T defaultValue) {
        try {
            return FrameworkImplProvider.getRemotePreferenceValue(category, key, defaultValue);
        }
        catch (Exception e) {
            log.error("Error getting preference", e);
            return null;
        }
    }
}
