package org.janelia.it.workstation.browser.api.services;

import java.util.Arrays;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.nodes.FilterNode;
import org.janelia.it.workstation.browser.nodes.TreeNodeNode;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.gui.search.Filtering;
import org.janelia.model.domain.workspace.TreeNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * A helper for working with TreeNodes and Filters.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DomainObjectHelper.class, path = DomainObjectHelper.DOMAIN_OBJECT_LOOKUP_PATH)
public class TreeNodeObjectHelper implements DomainObjectHelper {

    @Override
    public boolean isCompatible(DomainObject domainObject) {
        return isCompatible(domainObject.getClass());
    }
    
    @Override
    public boolean isCompatible(Class<? extends DomainObject> clazz) {
        if (TreeNode.class.isAssignableFrom(clazz)) {
            return true;
        }
        else if (Filtering.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    @Override
    public Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception {
        if (TreeNode.class.isAssignableFrom(domainObject.getClass())) {
            return new TreeNodeNode(parentChildFactory, (TreeNode)domainObject);
        }
        else if (Filtering.class.isAssignableFrom(domainObject.getClass())) {
            return new FilterNode(parentChildFactory, (Filtering)domainObject);
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }    
    }
    
    @Override
    public String getLargeIcon(DomainObject domainObject) {
        if (domainObject instanceof TreeNode) {
            return "folder_large.png";
        }
        else if (domainObject instanceof Filtering) {
            return "search_large.png";
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }

    @Override
    public boolean supportsRemoval(DomainObject domainObject) {
        if (TreeNode.class.isAssignableFrom(domainObject.getClass())) {
            return true;
        }
        else if (Filter.class.isAssignableFrom(domainObject.getClass())) {
            return true;
        }
        else {
            return false;
        }
    }
    
    @Override
    public void remove(DomainObject domainObject) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        if (TreeNode.class.isAssignableFrom(domainObject.getClass())) {
            model.remove(Arrays.asList((TreeNode)domainObject));
        }
        else if (Filter.class.isAssignableFrom(domainObject.getClass())) {
            model.remove(Arrays.asList(((Filter)domainObject)));
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }

}
