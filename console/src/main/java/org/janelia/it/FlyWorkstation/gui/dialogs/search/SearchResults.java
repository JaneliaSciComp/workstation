package org.janelia.it.FlyWorkstation.gui.dialogs.search;

import java.util.*;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Manages a set of search results, including the loaded pages of Solr results, and result tree mapping.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchResults {
	
	protected final List<ResultPage> pages = new ArrayList<ResultPage>();
	protected final Set<Long> allResultIds = new HashSet<Long>();
    protected final Set<Long> allMappedIds = new HashSet<Long>();
	protected final Map<Long,Integer> resultIdToRowIndex = new HashMap<Long,Integer>();
    protected final Map<Long,Integer> mappedIdToRowIndex = new HashMap<Long,Integer>();
    
    protected ResultTreeMapping resultTreeMapping;
    protected int numLoaded = 0;
    protected int numFound = 0;
    
    public SearchResults() {   
    }
    
    public int getNumLoadedPages() {
    	return pages.size();
    }
    
    public int getNumResultsLoaded() {
    	return numLoaded;
    }
    
    public int getNumResultsFound() {
    	return numFound;
    }
    
    public boolean hasMoreResults() {
    	return numFound>numLoaded;
    }
    
    public List<ResultPage> getPages() {
    	return pages;
    }
 
    public Integer getRowIndexForResultId(Long entityId) {
    	return resultIdToRowIndex.get(entityId);
    }
    
    public Integer getRowIndexForMappedId(Long entityId) {
    	return mappedIdToRowIndex.get(entityId);
    }
 
    public void clear() {
		pages.clear();
		allResultIds.clear();
		resultIdToRowIndex.clear();
		numLoaded = 0;
		clearMapping();
    }
    
    public void clearMapping() {
    	mappedIdToRowIndex.clear();
		allMappedIds.clear();
    }
    
    public void addPage(ResultPage resultPage) {
    	pages.add(resultPage);
    	for(Entity entity : resultPage.getSolrResults().getResultList()) {
    		Long entityId = entity.getId();
    		if (allResultIds.contains(entityId)) {
    			System.out.println("WARNING: Duplicate id found in results: "+entityId);
    		}
    		allResultIds.add(entityId);
    		resultIdToRowIndex.put(entityId, allResultIds.size()-1);
    	}
    	numLoaded += resultPage.getSolrResults().getResultList().size();
    	numFound = (int)resultPage.getSolrResults().getResponse().getResults().getNumFound();
    }

    public void projectResultPages() throws Exception {
    	for(ResultPage resultPage : pages) {
    		projectResultPage(resultPage);
    	}
    }
    
	public void projectResultPage(final ResultPage resultPage) throws Exception {
		resultPage.projectResults(resultTreeMapping, this);
		for(Entity mappedEntity : resultPage.getMappedResults()) {
			Long mappedId = mappedEntity.getId();
    		if (allMappedIds.contains(mappedId)) {
    			System.out.println("WARNING: Duplicate id found in mapping: "+mappedId);
    		}
			allMappedIds.add(mappedId);
			mappedIdToRowIndex.put(mappedId, allMappedIds.size()-1);
		}
	}

	public void setResultTreeMapping(ResultTreeMapping resultTreeMapping) {
		clearMapping();
		this.resultTreeMapping = resultTreeMapping;
	}

	public ResultTreeMapping getResultTreeMapping() {
		return resultTreeMapping;
	}	
	
	public boolean hasMappedEntity(Long mappedEntityId) {
		return allMappedIds.contains(mappedEntityId);
	}
}
