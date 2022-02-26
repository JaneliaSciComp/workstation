package org.janelia.workstation.core.api.facade.interfaces;

import java.util.Collection;
import java.util.List;

import org.janelia.model.access.domain.search.DocumentSearchResults;
import org.janelia.model.access.domain.search.DocumentSearchParams;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.files.SyncedPath;
import org.janelia.model.domain.files.SyncedRoot;
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
    DocumentSearchResults performSearch(DocumentSearchParams query) throws Exception;
    
    /**
     * Return the current user's default workspace.
     * @return workspace
     */
    Workspace getDefaultWorkspace() throws Exception;

    /**
     * Return all of the workspaces that the current user can access. 
     * @return list of workspaces
     */
    Collection<Workspace> getWorkspaces() throws Exception;

    /**
     * Create and return a new tree node. 
     * @param treeNode the tree node to create
     * @return the saved tree node
     * @throws Exception something went wrong
     */
    TreeNode create(TreeNode treeNode) throws Exception;

    /**
     * Create and return a new filter. 
     * @param filter the filter to create
     * @return the saved filter
     * @throws Exception something went wrong
     */
    Filter create(Filter filter) throws Exception;

    /**
     * Update and return the given filter.
     * @param filter the filter to update, with an existing GUID
     * @return the saved filter
     * @throws Exception something went wrong
     */
    Filter update(Filter filter) throws Exception;

    /**
     * Retrieve the paginated children of a given node, sorted according to the given sortCriteria.
     * @param node the node (TreeNode/Workspace/etc)
     * @param sortCriteria sort direction and field, e.g. "+id" or "-name"
     * @param page index of the page to return
     * @param pageSize size of pages
     * @return list of children on the page requested
     * @throws Exception
     */
    List<DomainObject> getChildren(Node node, String sortCriteria, int page, int pageSize) throws Exception;

    /**
     * Add the given references as children of the specified node, at some index. 
     * @param node the node 
     * @param references collection of references to add
     * @param index the index at which to insert the new children, or null to add them at the end
     * @return the updated node
     * @throws Exception something went wrong
     */
    <T extends Node> T addChildren(T node, Collection<Reference> references, Integer index) throws Exception;

    /**
     * Remove the given children from the given node. 
     * @param node the node
     * @param references collection of references to remove
     * @return the updated node
     * @throws Exception something went wrong
     */
    <T extends Node> T removeChildren(T node, Collection<Reference> references) throws Exception;

    /**
     * Reorder the children of the given node. 
     * @param node the node
     * @param order permutation with the length of current children. The permutation lists the new positions of the original children, 
     * that is, for children <code>[A,B,C,D]</code> and permutation <code>[0,3,1,2]</code>, the final order would be <code>[A,C,D,B]</code>.
     * @return the updated node object
     * @throws Exception something went wrong
     */
    <T extends Node> T reorderChildren(T node, int[] order) throws Exception;

    /**
     * Checks whether there are any TreeNode or ObjectSet references to this object
     * @param object the object set
     * @return the updated object set
     * @throws Exception something went wrong
     */
    List<Reference> getContainerReferences(DomainObject object) throws Exception;

    /**
     * Returns all of the synced roots accessible by the current user.
     * @return list of roots
     * @throws Exception something went wrong
     */
    List<SyncedRoot> getSyncedRoots() throws Exception;

    /**
     * Returns all the paths which are part of the given synced root.
     * @param root the synced root object
     * @return list of children paths
     * @throws Exception something went wrong
     */
    List<SyncedPath> getChildren(SyncedRoot root) throws Exception;

    /**
     * Create the given root in the database.
     * @param syncedRoot object populated with properties to persist
     * @return saved object with populated id
     * @throws Exception something went wrong
     */
    SyncedRoot create(SyncedRoot syncedRoot) throws Exception;

    /**
     * Update the properties of the given root.
     * @param syncedRoot object populated with properties to persist
     * @return saved object
     * @throws Exception something went wrong
     */
    SyncedRoot update(SyncedRoot syncedRoot) throws Exception;

    /**
     * Remove the given object from the system, along with all of its discovered child paths.
     * @param syncedRoot the root object
     * @throws Exception something went wrong
     */
    void remove(SyncedRoot syncedRoot) throws Exception;

}
