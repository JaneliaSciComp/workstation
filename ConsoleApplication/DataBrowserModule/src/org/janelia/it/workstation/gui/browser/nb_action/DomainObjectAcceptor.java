package org.janelia.it.workstation.gui.browser.nb_action;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.nb_action.Compatible;


/**
 * Implement this to accept a Domain Object for processing.
 * 
 * @author fosterl
 */
public interface DomainObjectAcceptor extends Compatible<DomainObject> {
    public static final String DOMAIN_OBJECT_LOOKUP_PATH = "DomainObject/DomainObjectAcceptor/Nodes";
    String getActionLabel();
    void acceptDomainObject( DomainObject e );
    /**
     * Space these apart by at least 100, to leave room for injected separators
     * and for later-stage additions of menu items after the fact.
     * 
     * @return expected ascending order key for this menu item.
     */
    Integer getOrder();
    boolean isPrecededBySeparator();
    boolean isSucceededBySeparator();
    @Override
    boolean isCompatible( DomainObject e );
}
