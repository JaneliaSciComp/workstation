package org.janelia.workstation.core.model.search;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;

import org.apache.commons.lang3.StringUtils;
import org.janelia.model.access.domain.search.*;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainObjectAttribute;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.gui.search.criteria.*;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.support.SearchType;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.*;

/**
 * A faceted search for domain objects of a certain type. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SearchConfiguration.class);

    private static final boolean LOG_TIME_ELAPSED = false;

    private static final String SOLR_TYPE_FIELD = "search_type";
    private static final String SOLR_TYPES_FIELD = "search_type_sm";
    
    // Source state
    private final Filter filter;
    private final int pageSize;
    private String sortCriteria = "+id";
    
    // Derived from source state
    private Class<? extends DomainObject> searchClass;
    private final Map<String,DomainObjectAttribute> searchAttrs = new LinkedHashMap<>();
    private final List<String> facets = new ArrayList<>();
    private final Map<String,List<FacetValue>> facetValues = new HashMap<>();

    // Actual query
    private SolrQuery query;
    private String displayQueryString;

    // Options
    private boolean fetchAnnotations = true;
    
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

    public boolean isFetchAnnotations() {
        return fetchAnnotations;
    }

    public void setFetchAnnotations(boolean fetchAnnotations) {
        this.fetchAnnotations = fetchAnnotations;
    }

    public final void setSearchClass(Class<? extends DomainObject> searchClass) {

    	this.searchClass = searchClass;
       
        // Clear
        searchAttrs.clear();
        facets.clear();
        facetValues.clear();
        
        if (searchClass==null) return;

        filter.setSearchClass(searchClass.getName());
 
        // Order alphabetically by label
        List<DomainObjectAttribute> searchAttributes = DomainUtils.getSearchAttributes(searchClass);
        searchAttributes.sort(Comparator.comparing(DomainObjectAttribute::getLabel));
        
        for(DomainObjectAttribute attr : searchAttributes) {
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
    private SolrQueryBuilder getQueryBuilder() {

        SolrQueryBuilder builder = new SolrQueryBuilder();
        
        for (String subjectKey : AccessManager.getReaderSet()) {
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
        filters.put(SearchConfiguration.SOLR_TYPES_FIELD,Sets.newHashSet(searchType));
        
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
            DomainObjectAttribute sortAttr = searchAttrs.get(sortField);
            if (sortAttr!=null) {
                log.debug("Setting sort: {}",sortCriteria);
                builder.setSortField(sortAttr.getSearchKey());
                builder.setAscending(!sortCriteria.startsWith("-"));
            }
            else {
                log.debug("Unknown sort field: {}",sortCriteria);
            }

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
        log.info("Searching for: {}", builder.getSearchString());
        log.debug("  auxString={}", builder.getAuxString());
        log.debug("  auxAnnotationQuery={}", builder.getAuxAnnotationQueryString());
        log.debug("  facets={}", builder.getFacets());
        log.debug("  filters={}", builder.getFilters());
        log.debug("  rootId={}", builder.getRootId());
        log.debug("  ownerKeys={}", builder.getOwnerKeys());
        log.debug("  sortBy={}", builder.getSortField());

        DomainObjectResultPage firstPage = performSearch(0);
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
    DomainObjectResultPage performSearch(int page) throws Exception {

        if (SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("SearchConfiguration.performSearch called in the EDT");
        }

        StopWatch stopWatch = new LoggingStopWatch();

        query.setStart(pageSize * page);
        query.setRows(pageSize);
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        DocumentSearchParams queryParams = SolrQueryBuilder.serializeSolrQuery(query);
        DocumentSearchResults results = model.search(queryParams);

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
                    facetValueList.sort((o1, o2) -> ComparisonChain.start()
                            .compare(o1.getValue(), o2.getValue(), Ordering.natural())
                            .result());
                }

                facetValues.putAll(results.getFacetValues());
            }
        }

        if (LOG_TIME_ELAPSED) stopWatch.lap("performSolrSearch");

        List<DomainObject> domainObjects = model.getDomainObjects(refs);
        List<Annotation> annotations = fetchAnnotations ? model.getAnnotations(refs) : Collections.emptyList();
        log.info("Search found {} objects. Current page {} includes {} objects and {} annotations.",
                numFound, page, domainObjects.size(), annotations.size());

        if (refs.size()>domainObjects.size()) {
            log.warn("SOLR index is out of date! It refers to {} objects which no longer exist.",
                    refs.size()-domainObjects.size());

            int c = 0;
            for(Reference ref : refs) {
                if (model.getDomainObject(ref)==null) {
                    log.warn("Could not find "+ref);
                    c++;
                }
                if (c>=20) {
                    log.warn("Reached max logging of out of date objects.");
                    break;
                }
            }
        }
        
        if (LOG_TIME_ELAPSED) stopWatch.stop("performMongoSearch");

        return new DomainObjectResultPage(domainObjects, annotations, numFound);
    }
}
