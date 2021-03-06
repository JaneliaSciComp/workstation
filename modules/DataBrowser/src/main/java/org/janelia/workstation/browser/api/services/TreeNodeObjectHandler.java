package org.janelia.workstation.browser.api.services;

import java.util.Arrays;
import java.util.Collections;

import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.janelia.workstation.browser.gui.editor.TreeNodeEditorPanel;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.common.nodes.TreeNodeNode;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.workspace.TreeNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * A helper for working with Nodes and Filters.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DomainObjectHandler.class)
public class TreeNodeObjectHandler implements DomainObjectHandler {

    @Override
    public boolean isCompatible(DomainObject domainObject) {
        return isCompatible(domainObject.getClass());
    }
    
    @Override
    public boolean isCompatible(Class<? extends DomainObject> clazz) {
        return TreeNode.class.isAssignableFrom(clazz);
    }

    @Override
    public Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception {
        return new TreeNodeNode(parentChildFactory, (TreeNode)domainObject);    
    }

    @Override
    public Class<? extends ParentNodeSelectionEditor<? extends DomainObject,?,?>> getEditorClass(DomainObject domainObject) {
        return TreeNodeEditorPanel.class;
    }
    
    @Override   
    public String getLargeIcon(DomainObject domainObject) {
        return "folder_large.png";
    }

    @Override
    public boolean supportsRemoval(DomainObject domainObject) {
        return true;
    }
    
    @Override
    public void remove(DomainObject domainObject) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        model.remove(Collections.singletonList((TreeNode) domainObject));
    }

}
