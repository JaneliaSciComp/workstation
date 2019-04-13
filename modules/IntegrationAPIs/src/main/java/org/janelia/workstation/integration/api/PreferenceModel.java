package org.janelia.workstation.integration.api;

/**
 * Access to database-stored properties that are specific to the current user.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface PreferenceModel {

    public static final String LOOKUP_PATH = "PreferenceModel/Location/Nodes";

    void setPreferenceValue(String category, String key, Object value) throws Exception;

    <T> T getPreferenceValue(String category, String key, T defaultValue) throws Exception;
}
