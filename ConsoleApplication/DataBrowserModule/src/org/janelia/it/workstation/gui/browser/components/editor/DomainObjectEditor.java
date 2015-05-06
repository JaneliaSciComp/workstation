package org.janelia.it.workstation.gui.browser.components.editor;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectEditor<T extends DomainObject> {
    
    public void loadDomainObject(T domainObject);
    
    public String getName();
    
    public Object getEventBusListener();
    
}
