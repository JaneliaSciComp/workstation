package org.janelia.workstation.core.api;

import org.janelia.workstation.integration.api.PreferenceModel;
import org.openide.util.lookup.ServiceProvider;

/**
 * Preference handler which uses the DomainMgr.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = PreferenceModel.class, path= PreferenceModel.LOOKUP_PATH)
public class DomainMgrPreferenceModel implements PreferenceModel {

    @Override
    public void setPreferenceValue(String category, String key, Object value) throws Exception {
        DomainMgr.getDomainMgr().setPreference(category, key, value);
    }

    @Override
    public <T> T getPreferenceValue(String category, String key, T defaultValue) throws Exception {
        return DomainMgr.getDomainMgr().getPreferenceValue(category, key, defaultValue);
    }
}
