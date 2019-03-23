package org.janelia.it.workstation.browser.gui.editor;

import java.util.concurrent.Callable;

import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.model.domain.DomainObject;

/**
 * An editor for a single domain object node.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectNodeEditor<P extends DomainObject, T, S> extends Editor {

    /**
     * Reset the viewer state. This is usually done in preparation of loading a novel domain object.
     */
    public void resetState();

    /**
     * Save and return a snap shot of the current editor state.
     * 
     * @return Editor state
     */
    public DomainObjectEditorState<P,T,S> saveState();

    /**
     * Restore the given snap shot of the editor state.
     * 
     * @param state Saved editor state 
     */
    public void restoreState(DomainObjectEditorState<P,T,S> state);
    
    /**
     * Load the given domain object node into the editor. 
     * 
     * @param domainObjectNode
     * @param isUserDriven
     * @param success
     */
    public void loadDomainObjectNode(AbstractDomainObjectNode<P> domainObjectNode, final boolean isUserDriven, final Callable<Void> success);

    /**
     * Load the given domain object into the editor. This bypasses the need for a AbstractDomainObjectNode, 
     * but prevents some interaction with other viewers which might expect to hear about nodes being loaded.
     *  
     * @param domainObject
     * @param isUserDriven
     * @param success
     */
    public void loadDomainObject(P domainObject, final boolean isUserDriven, final Callable<Void> success);

}
