package org.janelia.it.workstation.gui.browser.gui.editor;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * An editor for a single domain object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectEditor<T extends DomainObject> {
    
    public void loadDomainObject(T domainObject);
    
    public String getName();
    
    public Object getEventBusListener();
    
}
