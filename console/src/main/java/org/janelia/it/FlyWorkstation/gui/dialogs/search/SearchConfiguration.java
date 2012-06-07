package org.janelia.it.FlyWorkstation.gui.dialogs.search;

import java.util.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchAttribute.DataStore;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchAttribute.DataType;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.compute.api.support.SageTerm;
import org.janelia.it.jacs.compute.api.support.SolrUtils;
import org.janelia.it.jacs.model.entity.EntityAttribute;

/**
 * The configuration of search attributes available for query and display.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchConfiguration {

    /** Fields to use as columns */
    protected static final String[] columnFields = {"id", "name", "entity_type", "username", "creation_date", "updated_date", "annotations", "score"};
    
    /** Labels to use on the columns */
    protected static final String[] columnLabels = {"GUID", "Name", "Type", "Owner", "Date Created", "Date Last Updated", "Annotations", "Score"};

    /** Labels to use on the columns */
    protected static final String[] columnDescriptions = {"Unique identifier", "Name", "Type", "Owner's username", "Date Created", "Date Last Updated", "Annotations", "Search relevancy score"};
    
    /** Which columns are sortable */
    protected static final boolean[] columnSortable = {true, true, true, true, true, true, false, true};
    
    /** Data types of the columns */
    protected static final DataType[] columnTypes = {DataType.STRING, DataType.STRING, DataType.STRING, DataType.STRING, DataType.DATE, DataType.DATE, DataType.STRING, DataType.STRING};
    
    /** Which columns are available in the entity model */
    protected static final boolean[] columnIsInEntity = {true, true, true, true, true, true, false, false};
    
    // Data
    protected final Map<AttrGroup, List<SearchAttribute>> attributeGroups = new LinkedHashMap<AttrGroup, List<SearchAttribute>>();
    protected List<SearchAttribute> attributes = new ArrayList<SearchAttribute>();
    protected Map<String, SageTerm> vocab;
    
    protected List<SearchConfigurationListener> listeners = new ArrayList<SearchConfigurationListener>();
    
    public enum AttrGroup {
    	BASIC("Basic Attributes"),
    	EXT("Extended Attributes"),
    	SAGE("SAGE Attributes");
    	private String label;
    	private AttrGroup(String label) {
    		this.label = label;
    	}
    	public String getLabel() {
    		return label;
    	}
    }
    
	public SearchConfiguration() {
	}
	
	public void load() {
		
		attributeGroups.clear();
		attributes.clear();
		vocab = null;
		
    	SimpleWorker attrLoadingWorker = new SimpleWorker() {
			
			@Override
			protected void doStuff() throws Exception {
			
				
				List<SearchAttribute> attrListBasic = new ArrayList<SearchAttribute>();
				attributeGroups.put(AttrGroup.BASIC, attrListBasic);
				for(int i=0; i<columnFields.length; i++) {
					String name = columnFields[i];
					String label = columnLabels[i];
					DataType dataType = columnTypes[i];
					SearchAttribute attr = new SearchAttribute(name, label, columnDescriptions[i], dataType, columnIsInEntity[i]?DataStore.ENTITY:DataStore.SOLR, columnSortable[i]);
					attrListBasic.add(attr);
					attributes.add(attr);
				}
				
				List<EntityAttribute> attrs = ModelMgr.getModelMgr().getEntityAttributes();
				Collections.sort(attrs, new Comparator<EntityAttribute>() {
					@Override
					public int compare(EntityAttribute o1, EntityAttribute o2) {
						return o1.getName().compareTo(o2.getName());
					}
				});
				
				List<SearchAttribute> attrListExt = new ArrayList<SearchAttribute>();
				attributeGroups.put(AttrGroup.EXT, attrListExt);
				for(EntityAttribute entityAttr : attrs) {
					String name = SolrUtils.getDynamicFieldName(entityAttr.getName());
					String label = entityAttr.getName();
					SearchAttribute attr = new SearchAttribute(name, label, label, DataType.STRING, DataStore.ENTITY_DATA, !name.endsWith("_txt"));
					attrListExt.add(attr);
					attributes.add(attr);
				}
				
				vocab = ModelMgr.getModelMgr().getFlyLightVocabulary();
				List<SageTerm> terms = new ArrayList<SageTerm>(vocab.values());
				Collections.sort(terms, new Comparator<SageTerm>() {
					@Override
					public int compare(SageTerm o1, SageTerm o2) {
						return o1.getName().compareTo(o2.getName());
					}
				});
				
				List<SearchAttribute> attrListSage = new ArrayList<SearchAttribute>();
				attributeGroups.put(AttrGroup.SAGE, attrListSage);
				for(SageTerm sageTerm : terms) {
					String name = SolrUtils.getSageFieldName(sageTerm);
					String label = sageTerm.getDisplayName();
					SearchAttribute attr = new SearchAttribute(name, label, sageTerm.getDefinition(), "date_time".equals(sageTerm.getDataType())?DataType.DATE:DataType.STRING, DataStore.SOLR, true);
					attrListSage.add(attr);
					attributes.add(attr);
				}
			}

			@Override
			protected void hadSuccess() {
		        dataLoadComplete();
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);		    	
			}
    	};
		
        attrLoadingWorker.execute();
	}
	
	public List<SearchAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<SearchAttribute> attributes) {
		this.attributes = attributes;
	}	

	public Map<String, SageTerm> getVocab() {
		return vocab;
	}

	public void setVocab(Map<String, SageTerm> vocab) {
		this.vocab = vocab;
	}

	public Map<AttrGroup, List<SearchAttribute>> getAttributeGroups() {
		return attributeGroups;
	}
	
	public void addConfigurationChangeListener(SearchConfigurationListener listener) {
		listeners.add(listener);
	}

	protected void dataLoadComplete() {
		SearchConfigurationEvent evt = new SearchConfigurationEvent(this);
		for(SearchConfigurationListener listener : listeners) {
			listener.configurationChange(evt);
		}
	}
}
