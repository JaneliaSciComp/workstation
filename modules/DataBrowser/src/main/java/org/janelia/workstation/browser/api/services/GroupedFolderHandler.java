package org.janelia.workstation.browser.api.services;

import java.util.Arrays;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.workspace.GroupedFolder;
import org.janelia.model.domain.workspace.ProxyGroup;
import org.janelia.workstation.browser.gui.editor.GroupedFolderEditorPanel;
import org.janelia.workstation.common.nodes.GroupedFolderNode;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * A helper for working with TreeNodes and Filters.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DomainObjectHandler.class)
public class GroupedFolderHandler implements DomainObjectHandler {

    @Override
    public boolean isCompatible(DomainObject domainObject) {
        return isCompatible(domainObject.getClass());
    }
    
    @Override
    public boolean isCompatible(Class<? extends DomainObject> clazz) {
        if (GroupedFolder.class.isAssignableFrom(clazz)) {
            return true;
        }
        else if (ProxyGroup.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    @Override
    public Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception {
        if (domainObject instanceof GroupedFolder) {
            return new GroupedFolderNode(parentChildFactory, (GroupedFolder)domainObject);
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }    
    }

    @Override
    public Class<? extends ParentNodeSelectionEditor<? extends DomainObject,?,?>> getEditorClass(DomainObject domainObject) {
        if (GroupedFolder.class.isAssignableFrom(domainObject.getClass())) {
            return GroupedFolderEditorPanel.class;
        }
        return null;
    }
    
    @Override   
    public String getLargeIcon(DomainObject domainObject) {
        if (domainObject instanceof GroupedFolder) {
            return "folder_files_large.png";
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }

    @Override
    public boolean supportsRemoval(DomainObject domainObject) {
        if (domainObject instanceof GroupedFolder) {
            return true;
        }
        else if (domainObject instanceof ProxyGroup) {
            return true;
        }
        else {
            return false;
        }
    }
    
    @Override
    public void remove(DomainObject domainObject) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        if (domainObject instanceof GroupedFolder) {
            model.remove(Arrays.asList((GroupedFolder)domainObject));
        }
        else if (domainObject instanceof ProxyGroup) {
            model.remove(Arrays.asList((ProxyGroup)domainObject));
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }

}
