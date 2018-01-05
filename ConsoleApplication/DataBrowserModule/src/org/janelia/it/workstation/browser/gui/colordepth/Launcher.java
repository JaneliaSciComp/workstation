package org.janelia.it.workstation.browser.gui.colordepth;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectAcceptor;
import org.janelia.it.workstation.browser.components.ViewerUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches the color depth search interface.
 */
@ServiceProvider(service = DomainObjectAcceptor.class, path=DomainObjectAcceptor.DOMAIN_OBJECT_LOOKUP_PATH)
public class Launcher implements DomainObjectAcceptor  {
    
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);
    
    private static final int MENU_ORDER = 250;
    
    public Launcher() {
    }

    @Override
    public void acceptDomainObject(DomainObject domainObject) {
        ColorDepthSearchTopComponent targetViewer = ViewerUtils.provisionViewer(ColorDepthSearchViewerManager.getInstance(), "editor");
        targetViewer.loadDomainObject(domainObject, true);
    }

    @Override
    public String getActionLabel() {
        return "  Open In Search Viewer";
    }

    @Override
    public boolean isCompatible(DomainObject dObj) {
        log.trace(dObj.getType() + " called " + dObj.getName() + " class: " + dObj.getClass().getSimpleName());
        return dObj instanceof ColorDepthSearch;
    }
    
    @Override
    public boolean isEnabled(DomainObject dObj) {
        return true;
    }
    
    @Override
    public Integer getOrder() {
        return MENU_ORDER;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return false;
    }

    @Override
    public boolean isSucceededBySeparator() {
        return false;
    }
}
