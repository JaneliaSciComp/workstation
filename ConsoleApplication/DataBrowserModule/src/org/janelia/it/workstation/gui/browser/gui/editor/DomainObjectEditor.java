package org.janelia.it.workstation.gui.browser.gui.editor;

import java.util.concurrent.Callable;
import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * An editor for a single domain object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectEditor<T extends DomainObject> {
    
    public void loadDomainObject(T domainObject, final boolean isUserDriven, final Callable<Void> success);
    
    public String getName();
    
    public Object getEventBusListener();
    
}
