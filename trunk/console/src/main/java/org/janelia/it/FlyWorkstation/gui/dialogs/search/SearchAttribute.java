package org.janelia.it.FlyWorkstation.gui.dialogs.search;

/**
 * A typed, searchable attribute, indexed in SOLR. The dataStore tells us where the attribute actually originated, 
 * so that we can get the most up-to-date value for it. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchAttribute {

	public enum DataStore {
		ENTITY,
		ENTITY_DATA,
		SOLR;
	}
	
	public enum DataType {
		STRING,
		DATE;
	}
	
	private DataStore dataStore;
	private DataType dataType;
	private String name;
	private String label;
	private String description;
	private boolean sortable;
	
	public SearchAttribute(String name, String label, String description, DataType dataType, DataStore dataStore, boolean sortable) {
		this.name = name;
		this.label = label;
		this.description = description;
		this.dataType = dataType;
		this.dataStore = dataStore;
		this.sortable = sortable;
	}

	public DataStore getDataStore() {
		return dataStore;
	}

	public DataType getDataType() {
		return dataType;
	}

	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}

	public String getDescription() {
		return description;
	}
	
	public boolean isSortable() {
		return sortable;
	}

	@Override
	public String toString() {
		return label;
	}
}
