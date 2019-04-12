package org.janelia.workstation.common.gui.editor;

import java.util.concurrent.Callable;

import org.janelia.model.domain.DomainObject;

/**
 * An editor for a single domain object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectEditor<T extends DomainObject> extends Editor {
    
    void loadDomainObject(T domainObject, final boolean isUserDriven, final Callable<Void> success);
    
}
