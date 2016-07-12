package org.janelia.it.workstation.gui.browser.model.search;

import java.util.*;

import javax.swing.SwingUtilities;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.gui.search.criteria.*;
import org.janelia.it.jacs.model.domain.gui.search.criteria.TreeNodeCriteria;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainObjectAttribute;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.SearchType;
import org.janelia.it.jacs.shared.solr.FacetValue;
import org.janelia.it.jacs.shared.solr.SolrJsonResults;
import org.janelia.it.jacs.shared.solr.SolrParams;
import org.janelia.it.jacs.shared.solr.SolrQueryBuilder;
import org.janelia.it.jacs.shared.solr.SolrUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

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
    private String sortCriteria;
    
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

    public String getSortCriteria() {
        return sortCriteria;
    }

    public void setSortCriteria(String sortCriteria) {
        this.sortCriteria = sortCriteria;
    }

    public final void setSearchClass(Class<? extends DomainObject> searchClass) {

    	this.searchClass = searchClass;
       
        // Clear
        searchAttrs.clear();
        facets.clear();
        facetValues.clear();
        
        if (searchClass==null) return;

        filter.setSearchClass(searchClass.getName());
 
        for(DomainObjectAttribute attr : DomainUtils.getSearchAttributes(searchClass)) {
            if (attr.isDisplay()) {
                String facetKey = attr.getFacetKey();
                if (facetKey!=null) {
                    facets.add(facetKey);
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

    public DomainObjectAttribute getDomainObjectAttribute(String name) {
        return searchAttrs.get(name);
    }
    
    public List<FacetValue> getFacetValues(String facetKey) {
        return facetValues.get(facetKey);
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
            log.debug("Setting query string: {}",filter.getSearchString());
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
                    if (attr!=null) {
                        if (attr.getFacetKey()==null) {
                            log.warn("Search requests facet {} but it does not have a defined facet key on {}",fc.getAttributeName(),searchType);
                        }
                        else {
                            filters.put(attr.getFacetKey(), fc.getValues());
                        }
                    }
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
                else if (criteria instanceof TreeNodeCriteria) {
                    TreeNodeCriteria sc = (TreeNodeCriteria) criteria;
                    Reference ref = sc.getTreeNodeReference();
                    log.debug("Setting query root: {}",ref.getTargetId());
                    builder.setRootId(ref.getTargetId());
                }
            }
        }
        
        if (aux.length()>0) {
            log.debug("Adding aux query string: {}",aux);
            builder.setAuxString(aux.toString());
        }
        
        if (auxAnnot.length()>0) {
            log.debug("Adding aux annotation query string: {}",auxAnnot);
            builder.setAuxAnnotationQueryString(auxAnnot.toString());
        }
        
        log.debug("Adding facets: {}",facets);
        builder.getFacets().addAll(facets);
        
        log.debug("Adding facet filters: {}",filters);
        builder.getFilters().putAll(filters);

        if (!StringUtils.isEmpty(sortCriteria)) {
            String sortField = (sortCriteria.startsWith("-")||sortCriteria.startsWith("+")) ? sortCriteria.substring(1) : sortCriteria;
            log.debug("Setting sort: {}",sortCriteria);
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

        StopWatch stopWatch = new LoggingStopWatch();

        query.setStart(pageSize * page);
        query.setRows(pageSize);
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        SolrParams queryParams = SolrQueryBuilder.serializeSolrQuery(query);
        SolrJsonResults results = model.search(queryParams);

        List<Reference> refs = new ArrayList<>();
        long numFound = 0;

        if (results != null) {
            for (SolrDocument doc : results.getResults()) {
                Long id = new Long(doc.get("id").toString());
                String type = (String) doc.getFieldValue(SOLR_TYPE_FIELD);
                String className = DomainUtils.getClassNameForSearchType(type);
                if (className != null) {
                    refs.add(Reference.createFor(className, id));
                } else {
                    log.warn("Unrecognized type has no collection mapping: " + type);
                }
            }
            
            numFound = results.getNumFound();

            facetValues.clear();
            if (results.getFacetValues()!=null) {

                // Sort each facet list in place. The mutability isn't great, but no one else will see this list.
                for(String facet : results.getFacetValues().keySet()) {
                    List<FacetValue> facetValueList = results.getFacetValues().get(facet);
                    Collections.sort(facetValueList, new Comparator<FacetValue>() {
                        @Override
                        public int compare(FacetValue o1, FacetValue o2) {
                            return ComparisonChain.start()
                                    .compare(o1.getValue(), o2.getValue(), Ordering.natural())
                                    .result();
                        }
                    });
                }

                facetValues.putAll(results.getFacetValues());
            }
        }

        stopWatch.lap("performSolrSearch");

        List<DomainObject> domainObjects = model.getDomainObjects(refs);
        List<Annotation> annotations = model.getAnnotations(refs);
        log.info("Search found {} objects. Current page includes {} objects and {} annotations.", numFound, domainObjects.size(), annotations.size());

        stopWatch.stop("performMongoSearch");

        return new ResultPage(domainObjects, annotations, numFound);
    }
}
