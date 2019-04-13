package org.janelia.workstation.integration.api;

/**
 * Implement this to carry along model 'settings' from one session to the
 * next.  These are serialized.
 *
 * @author fosterl
 */
public interface SettingsModel {
    public static final String LOOKUP_PATH = "SettingsModel/Location/Nodes";
    Object getModelProperty(String propName);
    void setModelProperty(String propName, Object value);
}
