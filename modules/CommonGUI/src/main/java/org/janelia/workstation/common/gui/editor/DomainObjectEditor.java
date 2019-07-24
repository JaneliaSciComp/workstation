package org.janelia.workstation.common.gui.editor;

import java.util.concurrent.Callable;

import org.janelia.model.domain.DomainObject;

/**
 * An editor for a single domain object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectEditor<P extends DomainObject> extends Editor, ViewerContextProvider {
    
    void loadDomainObject(P domainObject, final boolean isUserDriven, final Callable<Void> success);

}
