package org.janelia.workstation.integration.framework.system;

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
