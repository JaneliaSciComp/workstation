package org.janelia.it.workstation.gui.browser.gui.editor;

import java.util.concurrent.Callable;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.navigation.DomainObjectEditorState;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;

/**
 * An editor for a single domain object node.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectNodeEditor<T extends DomainObject> extends Editor {

    public void loadDomainObject(T domainObject, final boolean isUserDriven, final Callable<Void> success);

    public void loadDomainObjectNode(DomainObjectNode<T> domainObjectNode, final boolean isUserDriven, final Callable<Void> success);

    public DomainObjectEditorState saveState();

    public void loadState(DomainObjectEditorState state);
}
