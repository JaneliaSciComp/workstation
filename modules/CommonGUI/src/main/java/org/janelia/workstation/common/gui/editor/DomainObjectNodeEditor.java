package org.janelia.workstation.common.gui.editor;

import java.util.concurrent.Callable;

import org.janelia.workstation.core.nodes.DomainObjectNode;
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
    void resetState();

    /**
     * Save and return a snap shot of the current editor state.
     * 
     * @return Editor state
     */
    DomainObjectEditorState<P,T,S> saveState();

    /**
     * Restore the given snap shot of the editor state.
     * 
     * @param state Saved editor state 
     */
    void restoreState(DomainObjectEditorState<P,T,S> state);
    
    /**
     * Load the given domain object node into the editor. 
     * 
     * @param domainObjectNode
     * @param isUserDriven
     * @param success
     */
    void loadDomainObjectNode(DomainObjectNode<P> domainObjectNode, final boolean isUserDriven, final Callable<Void> success);

    /**
     * Load the given domain object into the editor. This bypasses the need for a AbstractDomainObjectNode, 
     * but prevents some interaction with other viewers which might expect to hear about nodes being loaded.
     *  
     * @param domainObject
     * @param isUserDriven
     * @param success
     */
    void loadDomainObject(P domainObject, final boolean isUserDriven, final Callable<Void> success);

}
