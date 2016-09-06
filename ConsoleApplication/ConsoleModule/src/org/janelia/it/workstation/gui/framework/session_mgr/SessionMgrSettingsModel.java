/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.session_mgr;

import org.janelia.it.jacs.integration.framework.session_mgr.SettingsModel;
import org.openide.util.lookup.ServiceProvider;

/**
 * Session Manager's implementation of the settings model.  This implementation
 * lets NetBeans' platform remove direct dependencies between plugins and
 * the session manager's settings.
 *
 * @author fosterl
 */
@ServiceProvider(service = SettingsModel.class, path=SettingsModel.LOOKUP_PATH)
public class SessionMgrSettingsModel implements SettingsModel {

    @Override
    public Object getModelProperty(String propName) {
        return SessionMgr.getSessionMgr().getModelProperty(propName);
    }

    @Override
    public void setModelProperty(String propName, Object value) {
        SessionMgr.getSessionMgr().setModelProperty(propName, value);
    }
    
}
