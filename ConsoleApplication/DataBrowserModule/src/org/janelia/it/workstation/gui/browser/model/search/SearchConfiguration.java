package org.janelia.it.workstation.gui.browser.model.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.SwingUtilities;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.gui.search.criteria.AttributeCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.AttributeValueCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.DateRangeCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.FacetCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.ObjectSetCriteria;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.SearchType;
import org.janelia.it.jacs.shared.solr.*;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.model.DomainObjectAttribute;
import org.janelia.it.workstation.gui.dialogs.search.CriteriaOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import org.janelia.it.jacs.model.domain.support.DomainUtils;

/**
 * A faceted search for domain objects of a certain type. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SearchConfiguration.class);

    public static final String SOLR_TYPE_FIELD = "search_type";
    
    // Source state
    private final Filter filter;
    private final int pageSize;
    
    // Derived from source state
    private Class<? extends DomainObject> searchClass;
    private final Map<String,DomainObjectAttribute> searchAttrs = new TreeMap<>();
    private final List<String> facets = new ArrayList<>();
    private final Map<String,List<FacetValue>> facetValues = new HashMap<>();

    // Actual query
    private SolrQuery query;
    private String displayQueryString;
    
    public SearchConfiguration(Filter filter, int pageSize) {
        this.filter = filter;
        this.pageSize = pageSize;
        setSearchClass(DomainUtils.getObjectClassByName(filter.getSearchClass()));
    }
    
    public Filter getFilter() {
        return filter;
    }

    public final void setSearchClass(Class<? extends DomainObject> searchClass) {

        log.info("Setting search class: {}",searchClass);
    	this.searchClass = searchClass;
       
        // Clear
        searchAttrs.clear();
        facets.clear();
        facetValues.clear();
        
        if (searchClass==null) return;
               
        filter.setSearchClass(searchClass.getName());
 
        for(DomainObjectAttribute attr : ClientDomainUtils.getSearchAttributes(searchClass)) {
            if (attr.isDisplay()) {
                if (attr.isFacet()) {
                    facets.add(attr.getSearchKey());
                }
                searchAttrs.put(attr.getName(),attr);
            }
        }
        
        // Remove any criteria which are no longer relevant
        if (filter.hasCriteria()) {
            for(Iterator<Criteria> i=filter.getCriteriaList().iterator(); i.hasNext(); ) {
                Criteria criteria = i.next();
                if (criteria instanceof AttributeCriteria) {
                   AttributeCriteria ac = (AttributeCriteria)criteria;
                   if (getDomainObjectAttribute(ac.getAttributeName())==null) {
                       i.remove();
                   }
                }
            }
        }
    }
    
    public Class<? extends DomainObject> getSearchClass() {
        return searchClass;
    }

    public Collection<DomainObjectAttribute> getDomainObjectAttributes() {
        return searchAttrs.values();
    }

    public Collection<DomainObjectAttribute> getVisibleDomainObjectAttributes() {
        return searchAttrs.values();
    }
    
    public DomainObjectAttribute getDomainObjectAttribute(String name) {
        return searchAttrs.get(name);
    }
    
    public List<FacetValue> getFacetValues(String searchKey) {
        return facetValues.get(searchKey);
    }

	public String getDisplayQueryString() {
		return displayQueryString;
	}
    
    /**
     * Returns a query builder for the current search parameters.
     *
     * @return
     */
    public SolrQueryBuilder getQueryBuilder() {

        SolrQueryBuilder builder = new SolrQueryBuilder();
        
        for (String subjectKey : AccessManager.getSubjectKeys()) {
            log.trace("Adding query owner key: {}",subjectKey);
            builder.addOwnerKey(subjectKey);
        }
        
        if (filter.getSearchString()!=null) {
            log.info("Setting query string: {}",filter.getSearchString());
            builder.setSearchString(filter.getSearchString());
        }

        StringBuilder aux = new StringBuilder();
        StringBuilder auxAnnot = new StringBuilder();

        final Map<String, Set<String>> filters = new HashMap<>();
        SearchType searchTypeAnnot = searchClass.getAnnotation(SearchType.class);
        String searchType = searchTypeAnnot.key();
        filters.put(SearchConfiguration.SOLR_TYPE_FIELD,Sets.newHashSet(searchType));
        
        if (filter.hasCriteria()) {
            for (Criteria criteria : filter.getCriteriaList()) {
                if (criteria instanceof FacetCriteria) {
                    FacetCriteria fc = (FacetCriteria) criteria;
                    DomainObjectAttribute attr = searchAttrs.get(fc.getAttributeName());
                    filters.put(attr.getSearchKey(), fc.getValues());
                }
                else if (criteria instanceof AttributeCriteria) {
                    AttributeCriteria ac = (AttributeCriteria) criteria;
                    // TODO: allow user to select operator
                    CriteriaOperator operator = CriteriaOperator.CONTAINS;
                    String value1 = null;
                    String value2 = null;

                    if (criteria instanceof DateRangeCriteria) {
                        operator = CriteriaOperator.BETWEEN;
                        DateRangeCriteria drc = (DateRangeCriteria) criteria;
                        Date startCal = drc.getStartDate();
                        Date endCal = drc.getEndDate();
                        value1 = startCal == null ? "*" : SolrUtils.formatDate(startCal);
                        value2 = endCal == null ? "*" : SolrUtils.formatDate(endCal);
                    }
                    else if (criteria instanceof AttributeValueCriteria) {
                        AttributeValueCriteria avc = (AttributeValueCriteria) criteria;
                        value1 = avc.getValue();
                    }
                    else {
                        log.warn("Unsupported criteria type: {}",criteria.getClass().getName());
                    }
                    
                    if (value1 == null && value2 == null) {
                        continue;
                    }

                    if ("annotations".equals(ac.getAttributeName())) {
                        if (auxAnnot.length()>1) {
                            auxAnnot.append(" ");
                        }
                        switch (operator) {
                            case NOT_NULL:
                                auxAnnot.append("*");
                                break;
                            default:
                                auxAnnot.append(value1);
                                break;
                        }
                        continue;
                    }

                    if (aux.length() > 0) {
                        aux.append(" ");
                    }
                    aux.append("+");
                    DomainObjectAttribute attr = searchAttrs.get(ac.getAttributeName());
                    aux.append(attr.getSearchKey());
                    aux.append(":");

                    switch (operator) {
                        case CONTAINS:
                            if (criteria instanceof DateRangeCriteria) {
                                aux.append("[");
                                aux.append(value1);
                                aux.append(" TO ");
                                aux.append(value1);
                                aux.append("+1DAY]");
                            }
                            else {
                                aux.append(value1);
                            }
                            break;
                        case BETWEEN:
                            aux.append("[");
                            aux.append(value1);
                            aux.append(" TO ");
                            aux.append(value2);
                            aux.append("]");
                            break;
                        case NOT_NULL:
                            aux.append("*");
                            break;
                    }

                }
                else if (criteria instanceof ObjectSetCriteria) {
                    ObjectSetCriteria sc = (ObjectSetCriteria) criteria;
                    Reference ref = sc.getObjectSetReference();
                    log.info("Setting query root: {}",ref.getTargetId());
                    builder.setRootId(ref.getTargetId());
                }
            }
        }
        
        if (aux.length()>0) {
            log.info("Adding aux query string: {}",aux);
            builder.setAuxString(aux.toString());
        }
        
        if (auxAnnot.length()>0) {
            log.info("Adding aux annotation query string: {}",auxAnnot);
            builder.setAuxAnnotationQueryString(auxAnnot.toString());
        }
        
        log.info("Adding facets: {}",facets);
        builder.getFacets().addAll(facets);
        
        log.info("Adding facet filters: {}",filters);
        builder.getFilters().putAll(filters);

        String sortCriteria = filter.getSort();
        if (!StringUtils.isEmpty(sortCriteria)) {
            String sortField = (sortCriteria.startsWith("-")||sortCriteria.startsWith("+")) ? sortCriteria.substring(1) : sortCriteria;
            log.info("Setting sort: {}",sortCriteria);
            DomainObjectAttribute sortAttr = searchAttrs.get(sortField);
            builder.setSortField(sortAttr.getSearchKey());
            builder.setAscending(!sortCriteria.startsWith("-"));
        }
        
        return builder;
    }

    public SolrSearchResults performSearch() throws Exception {
        SolrQueryBuilder builder = getQueryBuilder();
        
        // We don't want to display all the system level query parameters, so build a simplified version of 
        // the query string for display purposes. 
        StringBuilder qs = new StringBuilder();
        if (builder.getAuxString()!=null) {
            qs.append(builder.getAuxString());
        }
        if (qs.length()>0) {
            qs.append(" ");
        }
        if (builder.getSearchString()!=null) {
            qs.append(builder.getSearchString());
        }
        this.displayQueryString = qs.toString();
        
        this.query = builder.getQuery();
        ResultPage firstPage = performSearch(0);
        SolrSearchResults searchResults = new SolrSearchResults(this, firstPage);
        log.debug("Got {} results", firstPage.getNumPageResults());
        return searchResults;
    }
    
    /**
     * Perform a search for a specific page. This method is package-protected because it should
     * only be used by the SolrSearchResults class to get additional data, once the original SearchResults
     * are returned by performSearch().
     * @param page page of results to search for and return
     * @return
     * @throws Exception
     */
    ResultPage performSearch(int page) throws Exception {

        if (SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("SearchConfiguration.performSearch called in the EDT");
        }

        query.setStart(pageSize * page);
        query.setRows(pageSize);
        DomainModel model = DomainMgr.getDomainMgr().getModel();
       // SolrParams queryParams = SolrQueryBuilder.serializeSolrQuery(query);
        String solrquery = query.toString();
        SolrJsonResults results = model.search(solrquery);

        List<Reference> refs = new ArrayList<>();

        if (results != null) {
            for (SolrDocument doc : results.getResults()) {
                Long id = new Long(doc.get("id").toString());
                String type = (String) doc.getFieldValue(SOLR_TYPE_FIELD);
                String className = DomainUtils.getClassNameForSearchType(type);
                if (className != null) {
                    refs.add(new Reference(className, id));
                } else {
                    log.warn("Unrecognized type has no collection mapping: " + type);
                }
            }
        }


        List<DomainObject> domainObjects = model.getDomainObjects(refs);
        List<Annotation> annotations = model.getAnnotations(refs);
        
        int numFound = (int)results.getResults().getNumFound();
        
        log.info("Search found {} objects", numFound);
        log.info("Page contains {} objects and {} annotations", domainObjects.size(), annotations.size());
        
        facetValues.clear();
        if (results.getFacetValues()!=null) {
            facetValues.putAll(results.getFacetValues());
        }
        
        return new ResultPage(domainObjects, annotations, numFound);
    }
}
