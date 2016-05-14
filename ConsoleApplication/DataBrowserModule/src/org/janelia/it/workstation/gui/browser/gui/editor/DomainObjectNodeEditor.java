package org.janelia.it.workstation.gui.browser.gui.editor;

import java.util.concurrent.Callable;

import org.janelia.it.workstation.gui.browser.api.navigation.NavigationHistory;
import org.janelia.it.workstation.gui.browser.navigation.DomainObjectEditorState;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;

/**
 * An editor for a single domain object node.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectNodeEditor<T extends DomainObjectNode> extends Editor {

    public void loadDomainObjectNode(T domainObjectNode, final boolean isUserDriven, final Callable<Void> success);

    public DomainObjectEditorState saveState();

    public void loadState(DomainObjectEditorState state);
}
