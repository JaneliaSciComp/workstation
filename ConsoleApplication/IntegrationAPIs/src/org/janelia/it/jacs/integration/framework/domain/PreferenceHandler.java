package org.janelia.it.jacs.integration.framework.domain;

/**
 * Access to database-stored properties that are specific to the current user.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface PreferenceHandler {

    public static final String LOOKUP_PATH = "PreferenceHandler/Location/Nodes";

    void setPreferenceValue(String category, String key, Object value) throws Exception;

    <T> T getPreferenceValue(String category, String key, T defaultValue) throws Exception;
}
