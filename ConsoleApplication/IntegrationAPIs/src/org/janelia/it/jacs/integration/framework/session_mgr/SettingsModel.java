/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.integration.framework.session_mgr;

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
