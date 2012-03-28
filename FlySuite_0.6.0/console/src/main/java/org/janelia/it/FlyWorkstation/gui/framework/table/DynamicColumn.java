package org.janelia.it.FlyWorkstation.gui.framework.table;

/**
 * A configurable column in a dynamic table.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicColumn {
	
	private String name;
	private String label;
	private boolean visible;
	private boolean editable;
	private boolean switchable;
	private boolean sortable;
	
	public DynamicColumn(String name, String label, boolean visible, boolean editable, boolean switchable, boolean sortable) {
		this.name = name;
		this.label = label;
		this.visible = visible;
		this.editable = editable;
		this.switchable = switchable;
		this.sortable = sortable;
	}
	
	public String getName() {
		return name;
	}
	
	public String getLabel() {
		return label;
	}
	
	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public boolean isSwitchable() {
		return switchable;
	}

	public void setSwitchable(boolean switchable) {
		this.switchable = switchable;
	}

	public boolean isSortable() {
		return sortable;
	}

	public void setSortable(boolean sortable) {
		this.sortable = sortable;
	}

	@Override
	public String toString() {
		return getLabel();
	}
	
}