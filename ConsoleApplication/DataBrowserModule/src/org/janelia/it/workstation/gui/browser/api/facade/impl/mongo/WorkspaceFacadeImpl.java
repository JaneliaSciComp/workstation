package org.janelia.it.workstation.gui.browser.api.facade.impl.mongo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.shared.solr.FacetValue;
import org.janelia.it.jacs.shared.solr.SolrJsonResults;
import org.janelia.it.jacs.shared.solr.SolrParams;
import org.janelia.it.jacs.shared.solr.SolrQueryBuilder;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.WorkspaceFacade;

public class WorkspaceFacadeImpl implements WorkspaceFacade {

    private final DomainDAO dao;

    public WorkspaceFacadeImpl() throws Exception {
        this.dao = DomainDAOManager.getInstance().getDao();
    }

    @Override
    public SolrJsonResults performSearch(SolrParams queryParams) throws Exception {
        SolrQuery query = SolrQueryBuilder.deSerializeSolrQuery(queryParams);
        SolrResults sr = ModelMgr.getModelMgr().searchSolr(query, false);
        Map<String,List<FacetValue>> facetValues = new HashMap<>();
        for (final FacetField ff : sr.getResponse().getFacetFields()) {
            List<FacetValue> favetValues = new ArrayList<>();
            if (ff.getValues()!=null) {
                for (final FacetField.Count count : ff.getValues()) {
                    favetValues.add(new FacetValue(count.getName(),count.getCount()));
                }
            }
            facetValues.put(ff.getName(), favetValues);
        }
        return new SolrJsonResults(sr.getResponse().getResults(), facetValues, sr.getResponse().getResults().getNumFound());
    }
    
    @Override
    public Workspace getDefaultWorkspace() {
        return dao.getDefaultWorkspace(AccessManager.getSubjectKey());
    }

    @Override
    public Collection<Workspace> getWorkspaces() {
        return dao.getWorkspaces(AccessManager.getSubjectKey());
    }

    @Override
    public TreeNode create(TreeNode treeNode) throws Exception {
        return (TreeNode) updateIndex (dao.save(AccessManager.getSubjectKey(), treeNode));
    }

    @Override
    public Filter create(Filter filter) throws Exception {
        return (Filter) updateIndex(dao.save(AccessManager.getSubjectKey(), filter));
    }

    @Override
    public Filter update(Filter filter) throws Exception {
        return (Filter) updateIndex(dao.save(AccessManager.getSubjectKey(), filter));
    }

    @Override
    public TreeNode addChildren(TreeNode treeNode, Collection<Reference> references, Integer index) throws Exception {
        TreeNode updatedNode = dao.addChildren(AccessManager.getSubjectKey(), treeNode, references, index);
        List<DomainObject> children = dao.getDomainObjects(AccessManager.getSubjectKey(), new ArrayList<>(references));
        for (DomainObject child: children) {
            ModelMgr.getModelMgr().addAncestorToIndex(child.getId(), updatedNode.getId());
        }
        return updatedNode;
    }

    @Override
    public TreeNode removeChildren(TreeNode treeNode, Collection<Reference> references) throws Exception {
        TreeNode updatedNode = dao.removeChildren(AccessManager.getSubjectKey(), treeNode, references);
        List<DomainObject> children = dao.getDomainObjects(AccessManager.getSubjectKey(), new ArrayList<>(references));
        for (DomainObject child: children) {
            updateIndex(child);
        }
        return updatedNode;
    }

    @Override
    public TreeNode reorderChildren(TreeNode treeNode, int[] order) throws Exception {
        return dao.reorderChildren(AccessManager.getSubjectKey(), treeNode, order);
    }

    @Override
    public List<Reference> getContainerReferences(DomainObject object) throws Exception {
        return dao.getContainerReferences(object);
    }

    private DomainObject updateIndex(DomainObject obj) throws Exception {
        ModelMgr.getModelMgr().updateIndex(obj);
        return obj;
    }
}
