package org.janelia.it.FlyWorkstation.gui.dialogs.search;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchAttribute.DataStore;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchAttribute.DataType;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.compute.api.support.EntityDocument;
import org.janelia.it.jacs.compute.api.support.SageTerm;
import org.janelia.it.jacs.compute.api.support.SolrUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configuration of search attributes available for query and display.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SearchConfiguration.class);
    
	/** Format for displaying dates */
	protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	/** Format for displaying scores */
	protected static final DecimalFormat decFormat = new DecimalFormat("#.##");
    
	/** Number of characters before cell values are truncated */
	protected static final int MAX_CELL_LENGTH = 50;
	
    /** Fields to use as columns */
    protected static final String[] columnFields = {"id", "name", "entity_type", "username", "creation_date", "updated_date", "annotations", "score"};
    
    /** Labels to use on the columns */
    protected static final String[] columnLabels = {"GUID", "Name", "Type", "Owner", "Date Created", "Date Last Updated", "Annotations", "Relevance"};

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
 	protected final Map<String, SearchAttribute> attrByName = new HashMap<String, SearchAttribute>();
    protected List<SearchAttribute> attributes = new ArrayList<SearchAttribute>();
    protected Map<String, SageTerm> vocab;
    
    protected List<SearchConfigurationListener> listeners = new ArrayList<SearchConfigurationListener>();
    protected boolean ready = false;
    
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
					attrByName.put(attr.getName(), attr);
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
					attrByName.put(attr.getName(), attr);
				}
				
				vocab = ModelMgr.getModelMgr().getFlyLightVocabulary();
				List<SageTerm> terms = new ArrayList<SageTerm>(vocab.values());
				Collections.sort(terms, new Comparator<SageTerm>() {
					@Override
					public int compare(SageTerm o1, SageTerm o2) {
						return o1.getDisplayName().compareTo(o2.getDisplayName());
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
					attrByName.put(attr.getName(), attr);
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
	
	public boolean isReady() {
		return ready;
	}

	public List<SearchAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<SearchAttribute> attributes) {
		this.attributes = attributes;
	}
	
	public SearchAttribute getAttributeByName(String name) {
		return attrByName.get(name);
	}

	public Map<AttrGroup, List<SearchAttribute>> getAttributeGroups() {
		return attributeGroups;
	}
	
	public void addConfigurationChangeListener(SearchConfigurationListener listener) {
		listeners.add(listener);
	}

	protected void dataLoadComplete() {
		this.ready  = true;
		SearchConfigurationEvent evt = new SearchConfigurationEvent(this);
		for(SearchConfigurationListener listener : listeners) {
			listener.configurationChange(evt);
		}
	}
	

    /**
     * Return the value of the specified column for the given object.
     * @param userObject
     * @param fieldName
     * @return
     */
	public String getValue(Object userObject, String fieldName) {
		Entity entity = null;
		SolrDocument doc  = null;
		if (userObject instanceof EntityDocument)  {
			EntityDocument entityDoc = (EntityDocument)userObject;	
			entity = entityDoc.getEntity();
			doc = entityDoc.getDocument();
		}
		else if (userObject instanceof Entity) {
			entity = (Entity)userObject;
		}
		else {
			throw new IllegalArgumentException("User object must be Entity or EntityDocument");
		}
		
		if (entity == null) {
			throw new IllegalArgumentException("Entity may not be null in user object="+userObject);
		}
		;
		Object value = null;
		if ("id".equals(fieldName)) {
			value = entity.getId();
		}
		else if ("name".equals(fieldName)) {
			value = entity.getName();
		}
		else if ("entity_type".equals(fieldName)) {
			value = entity.getEntityTypeName();
		}
		else if ("username".equals(fieldName)) {
			value = ModelMgrUtils.getNameFromSubjectKey(entity.getOwnerKey());
		}
		else if ("creation_date".equals(fieldName)) {
			value = entity.getCreationDate();
		}
		else if ("updated_date".equals(fieldName)) {
			value = entity.getUpdatedDate();
		}
		else if (doc!=null) {
			if ("annotations".equals(fieldName)) {
				StringBuffer sb = new StringBuffer();
				for(String subjectKey : SessionMgr.getSubjectKeys()) {
				    String owner = subjectKey.contains(":") ? subjectKey.split(":")[1] : subjectKey;
					Object v = doc.getFieldValues(owner+"_annotations");
					if (v!=null) {
						if (sb.length()>0) sb.append(" ");
						sb.append(getFormattedFieldValue(v, fieldName));
					}
				}
				value = sb.toString();
			}
			else {
				value = doc.getFieldValues(fieldName);	
			}
		}
		else {
		    // SOLR document is not available. This is probably a projected value.
//            if ("annotations".equals(fieldName) || "score".equals(fieldName)) {
//                return "";
//            }
            
            String attributeName = SolrUtils.getAttributeNameFromSolrFieldName(fieldName);
            value = entity.getValueByAttributeName(attributeName);
            
            if (value==null) {
                log.error("Unknown field '"+fieldName+"'");
            }
		}

		return getFormattedFieldValue(value, fieldName);
	}

    /**
     * Returns the human-readable label for the specified value in the given field. 
     * @param fieldName
     * @param value
     * @return
     */
	protected String getFormattedFieldValue(Object value, String fieldName) {
		if (value==null) return null;

		// Convert to collection
		Collection<Object> coll = null;
		if (value instanceof Collection) {
			coll = (Collection)value;
    	}
		else {
			coll = new ArrayList<Object>();
			coll.add(value);
		}
		
		// Format every value in the collection
		List<String> formattedValues = new ArrayList<String>();
		for(Object v : coll) {
			String formattedValue = v.toString();
			if (v instanceof Date) {
				formattedValue = df.format((Date)v);
			}
			else if (v instanceof Float) {
				formattedValue = decFormat.format((Float)v);
			}
			else if (v instanceof Double) {
				formattedValue = decFormat.format((Double)v);
			}
			else {
				if ("tiling_pattern_txt".equals(fieldName)) {
		    		formattedValue = StringUtils.underscoreToTitleCase(formattedValue);
		    	}	
			}
			formattedValues.add(formattedValue);
		}

		// Combine values
    	return StringUtils.getCommaDelimited(formattedValues, MAX_CELL_LENGTH);
    }
}
