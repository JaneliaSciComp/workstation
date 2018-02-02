package org.janelia.it.workstation.ab2;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectAcceptor;
import org.janelia.it.workstation.ab2.model.AB2DomainObject;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.Sample;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceProvider(service = DomainObjectAcceptor.class, path=DomainObjectAcceptor.DOMAIN_OBJECT_LOOKUP_PATH)
public class AB2DomainObjectAcceptor implements DomainObjectAcceptor  {

    private static final Logger logger = LoggerFactory.getLogger(AB2DomainObjectAcceptor.class);

    private static final int MENU_ORDER = 200;

    @Override
    public String getActionLabel() {
        return "  Open In AB2";
    }

    @Override
    public boolean isCompatible(DomainObject dObj) {
        // For now, restrict access to admins only
        if (!AccessManager.getAccessManager().isAdmin()) {
            return false;
        }
        logger.trace(dObj.getType() + " called " + dObj.getName() + " class: " + dObj.getClass().getSimpleName());
        if (dObj instanceof Sample) {
            return true;
        }
        else if (dObj instanceof AB2DomainObject) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isEnabled(DomainObject dObj) {
        return true;
    }

    @Override
    public void acceptDomainObject(DomainObject dObj) {
        logger.info("acceptDomainObject() dObj type=" + dObj.getClass().getName());
        AB2TopComponent ab2TopComponent = AB2TopComponent.findComp();
        if (ab2TopComponent != null) {
            if (!ab2TopComponent.isOpened()) {
                ab2TopComponent.open();
            }
            if (ab2TopComponent.isOpened()) {
                ab2TopComponent.requestActive();
            }
            ab2TopComponent.loadDomainObject(dObj, true);
        }
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