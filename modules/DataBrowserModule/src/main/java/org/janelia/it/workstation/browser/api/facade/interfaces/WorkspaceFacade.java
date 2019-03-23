package org.janelia.it.workstation.browser.api.facade.interfaces;

import java.util.Collection;
import java.util.List;

import org.janelia.it.jacs.shared.solr.SolrJsonResults;
import org.janelia.it.jacs.shared.solr.SolrParams;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.model.domain.workspace.Workspace;

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
    public Workspace getDefaultWorkspace() throws Exception;

    /**
     * Return all of the workspaces that the current user can access. 
     * @return list of workspaces
     */
    public Collection<Workspace> getWorkspaces() throws Exception;

    /**
     * Create and return a new tree node. 
     * @param treeNode the tree node to create
     * @return the saved tree node
     * @throws Exception something went wrong
     */
    public TreeNode create(TreeNode treeNode) throws Exception;

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
     * Add the given references as children of the specified node, at some index. 
     * @param node the node 
     * @param references collection of references to add
     * @param index the index at which to insert the new children, or null to add them at the end
     * @return the updated node
     * @throws Exception something went wrong
     */
    public <T extends Node> T addChildren(T node, Collection<Reference> references, Integer index) throws Exception;

    /**
     * Remove the given children from the given node. 
     * @param node the node
     * @param references collection of references to remove
     * @return the updated node
     * @throws Exception something went wrong
     */
    public <T extends Node> T removeChildren(T node, Collection<Reference> references) throws Exception;

    /**
     * Reorder the children of the given node. 
     * @param node the node
     * @param order permutation with the length of current children. The permutation lists the new positions of the original children, 
     * that is, for children <code>[A,B,C,D]</code> and permutation <code>[0,3,1,2]</code>, the final order would be <code>[A,C,D,B]</code>.
     * @return the updated node object
     * @throws Exception something went wrong
     */
    public <T extends Node> T reorderChildren(T node, int[] order) throws Exception;

    /**
     * Checks whether there are any TreeNode or ObjectSet references to this object
     * @param object the object set
     * @return the updated object set
     * @throws Exception something went wrong
     */
    public List<Reference> getContainerReferences(DomainObject object) throws Exception;

}
