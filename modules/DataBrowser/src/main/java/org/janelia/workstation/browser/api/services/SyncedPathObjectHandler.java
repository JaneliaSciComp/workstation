package org.janelia.workstation.browser.api.services;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.files.N5Container;
import org.janelia.model.domain.files.SyncedPath;
import org.janelia.model.domain.files.SyncedRoot;
import org.janelia.workstation.browser.gui.editor.FilterEditorPanel;
import org.janelia.workstation.browser.nodes.SyncedPathNode;
import org.janelia.workstation.browser.nodes.SyncedRootNode;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * A helper for working with SyncedRoots and SyncedPaths.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DomainObjectHandler.class)
public class SyncedPathObjectHandler implements DomainObjectHandler {

    @Override
    public boolean isCompatible(DomainObject domainObject) {
        return isCompatible(domainObject.getClass());
    }
    
    @Override
    public boolean isCompatible(Class<? extends DomainObject> clazz) {
        return SyncedPath.class.isAssignableFrom(clazz);
    }

    @Override
    public Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception {
        if (domainObject instanceof SyncedRoot) {
            return new SyncedRootNode(parentChildFactory, (SyncedRoot)domainObject);
        }
        else if (domainObject instanceof SyncedPath) {
            return new SyncedPathNode((SyncedPath)domainObject);
        }
        return null;
    }

    @Override
    public Class<? extends ParentNodeSelectionEditor<? extends DomainObject,?,?>> getEditorClass(DomainObject domainObject) {
        if (domainObject instanceof SyncedRoot) {
            return FilterEditorPanel.class;
        }
        else if (domainObject instanceof N5Container) {
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
        if (domainObject instanceof SyncedRoot) {
            return true;
        }
        return false;
    }
    
    @Override
    public void remove(DomainObject domainObject) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        model.remove((SyncedRoot)domainObject);
    }

    @Override
    public int getMaxReferencesBeforeRemoval(DomainObject domainObject) {
        return 0;
    }
}
