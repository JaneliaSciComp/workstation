package org.janelia.it.workstation.gui.browser.api;

import org.janelia.it.jacs.integration.framework.domain.PreferenceHandler;
import org.openide.util.lookup.ServiceProvider;

/**
 * Preference handler which uses the DomainMgr.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = PreferenceHandler.class, path=PreferenceHandler.LOOKUP_PATH)
public class DomainMgrPreferenceHandler implements PreferenceHandler {

    @Override
    public void setPreferenceValue(String category, String key, Object value) throws Exception {
        DomainMgr.getDomainMgr().setPreference(category, key, value);
    }

    @Override
    public Object getPreferenceValue(String category, String key, Object defaultValue) throws Exception {
        return DomainMgr.getDomainMgr().getPreferenceValue(category, key, defaultValue);
    }
    
}
