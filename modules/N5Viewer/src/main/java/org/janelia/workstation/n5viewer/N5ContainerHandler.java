package org.janelia.workstation.n5viewer;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.files.N5Container;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * Provides services for supporting N5Containers.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DomainObjectHandler.class, position = 200)
public class N5ContainerHandler implements DomainObjectHandler {

    @Override
    public boolean isCompatible(DomainObject domainObject) {
        return isCompatible(domainObject.getClass());
    }
    
    @Override
    public boolean isCompatible(Class<? extends DomainObject> clazz) {
        return N5Container.class.isAssignableFrom(clazz);
    }

    @Override
    public Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception {
        if (domainObject instanceof N5Container) {
            return new N5ContainerNode((N5Container)domainObject);
        }
        return null;
    }

    @Override
    public Class<? extends ParentNodeSelectionEditor<? extends DomainObject,?,?>> getEditorClass(DomainObject domainObject) {
        if (domainObject instanceof N5Container) {
            // TODO: create editor for opening N5 here
        }
        return null;
    }
    
    @Override   
    public String getLargeIcon(DomainObject domainObject) {
        return "folder_large.png";
    }

    @Override
    public boolean supportsRemoval(DomainObject domainObject) {
        return false;
    }
    
    @Override
    public void remove(DomainObject domainObject) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMaxReferencesBeforeRemoval(DomainObject domainObject) {
        return 0;
    }
}
