package org.janelia.it.workstation.gui.browser.api.facade.interfaces;

import java.util.Collection;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.shared.solr.SolrJsonResults;
import org.janelia.it.jacs.shared.solr.SolrParams;

/**
 * Implementations provide access to workspaces and related container objects. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface WorkspaceFacade {

    /**
     * Performs a search against the SolrServer and returns the results.
     * @param query the query to execute against the search server
     * @return the search results
     * @throws Exception something went wrong
     */
    public SolrJsonResults performSearch(SolrParams query) throws Exception;
    
    /**
     * Return the current user's default workspace.
     * @return workspace
     */
    public Workspace getDefaultWorkspace();

    /**
     * Return all of the workspaces that the current user can access. 
     * @return list of workspaces
     */
    public Collection<Workspace> getWorkspaces();

    /**
     * Create and return a new tree node. 
     * @param treeNode the tree node to create
     * @return the saved tree node
     * @throws Exception something went wrong
     */
    public TreeNode create(TreeNode treeNode) throws Exception;
    
    /**
     * Create a new object set. 
     * @param objectSet the object set to create, with null GUID
     * @return the saved object set
     * @throws Exception
     */
    public ObjectSet create(ObjectSet objectSet) throws Exception;

    /**
     * Create and return a new filter. 
     * @param filter the filter to create
     * @return the saved filter
     * @throws Exception something went wrong
     */
    public Filter create(Filter filter) throws Exception;

    /**
     * Update and return the given filter.
     * @param filter the filter to update, with an existing GUID
     * @return the saved filter
     * @throws Exception something went wrong
     */
    public Filter update(Filter filter) throws Exception;

    /**
     * Add the given references as children of the specified tree node, at some index. 
     * @param treeNode the tree node 
     * @param references collection of references to add
     * @param index the index at which to insert the new children, or null to add them at the end
     * @return the updated tree node
     * @throws Exception something went wrong
     */
    public TreeNode addChildren(TreeNode treeNode, Collection<Reference> references, Integer index) throws Exception;

    /**
     * Remove the given children from the given tree node. 
     * @param treeNode the tree node
     * @param references collection of references to remove
     * @return the updated tree node
     * @throws Exception something went wrong
     */
    public TreeNode removeChildren(TreeNode treeNode, Collection<Reference> references) throws Exception;

    /**
     * Reorder the children of the given tree node. 
     * @param treeNode the tree node
     * @param order permutation with the length of current children. The permutation lists the new positions of the original children, 
     * that is, for children <code>[A,B,C,D]</code> and permutation <code>[0,3,1,2]</code>, the final order would be <code>[A,C,D,B]</code>.
     * @return the updated tree node object
     * @throws Exception something went wrong
     */
    public TreeNode reorderChildren(TreeNode treeNode, int[] order) throws Exception;

    /**
     * Add the specified domain objects as members of the given object set. 
     * @param objectSet the object set
     * @param references collection of references to domain objects
     * @return the updated object set
     * @throws Exception something went wrong
     */
    public ObjectSet addMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception;

    /**
     * Remove the specified domain objects from the given object set. 
     * @param objectSet the object set
     * @param references collection of references to domain objects
     * @return the updated object set
     * @throws Exception something went wrong
     */
    public ObjectSet removeMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception;
    
    /**
     * Checks whether there are any TreeNode or ObjectSet references to this object
     * @param object the object set\
     * @return the updated object set
     * @throws Exception something went wrong
     */
    public List<Reference> getContainerReferences(DomainObject object) throws Exception;

}
