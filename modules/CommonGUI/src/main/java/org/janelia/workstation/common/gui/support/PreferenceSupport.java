package org.janelia.workstation.common.gui.support;

import org.janelia.workstation.integration.FrameworkImplProvider;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.workers.SimpleListenableFuture;
import org.janelia.workstation.core.workers.SimpleWorker;
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

    static final String DEFAULT_KEY = "Default";
    
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
        String key = getCurrentContextId()==null?"DEFAULT_KEY":getCurrentContextId().toString();
        return setPreferenceAsync(category, key, value);
    }
    
    /**
     * Sets the value for the given category for the current parent object.
     * Should be called in a background thread.
     * @param category
     * @param value
     * @throws Exception
     */
    default void setPreference(final String category, final Object value) throws Exception {
        String key = getCurrentContextId()==null?"DEFAULT_KEY":getCurrentContextId().toString();
        setPreference(category, key, value);
    }

    /**
     * Sets the value for the given category for the current parent object.
     * Runs in a background thread.
     * @param category
     * @param value
     * @return listenable future
     */
    default SimpleListenableFuture<Void> setPreferenceAsync(final String category, final String key, final Object value) {

        UIUtils.setMainFrameCursorWaitStatus(true);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                setPreference(category, key, value);
            }

            @Override
            protected void hadSuccess() {
                UIUtils.setMainFrameCursorWaitStatus(false);
            }

            @Override
            protected void hadError(Throwable error) {
                UIUtils.setMainFrameCursorWaitStatus(false);
                FrameworkImplProvider.handleException(error);
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
        String key = getCurrentContextId()==null?"DEFAULT_KEY":getCurrentContextId().toString();
        try {
            return FrameworkImplProvider.getRemotePreferenceValue(category, key, null);
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
