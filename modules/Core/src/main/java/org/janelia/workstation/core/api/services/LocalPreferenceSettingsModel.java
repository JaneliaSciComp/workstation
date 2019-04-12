package org.janelia.workstation.core.api.services;

import org.janelia.it.jacs.integration.framework.system.SettingsModel;
import org.janelia.workstation.core.api.LocalPreferenceMgr;
import org.openide.util.lookup.ServiceProvider;

/**
 * Session Manager's implementation of the settings model.  This implementation
 * lets NetBeans' platform remove direct dependencies between plugins and
 * the session manager's settings.
 *
 * @author fosterl
 */
@ServiceProvider(service = SettingsModel.class, path=SettingsModel.LOOKUP_PATH)
public class LocalPreferenceSettingsModel implements SettingsModel {

    @Override
    public Object getModelProperty(String propName) {
        return LocalPreferenceMgr.getInstance().getModelProperty(propName);
    }

    @Override
    public void setModelProperty(String propName, Object value) {
        LocalPreferenceMgr.getInstance().setModelProperty(propName, value);
    }
    
}
