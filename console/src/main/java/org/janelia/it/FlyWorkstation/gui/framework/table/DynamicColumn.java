package org.janelia.it.FlyWorkstation.gui.framework.table;

/**
 * A configurable column in a dynamic table.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicColumn {
	
	private String name;
	private boolean visible;
	private boolean editable;
	private boolean switchable;
	
	public DynamicColumn(String name, boolean visible, boolean editable, boolean switchable) {
		this.name = name;
		this.visible = visible;
		this.editable = editable;
		this.switchable = switchable;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public boolean isSwitchable() {
		return switchable;
	}

	@Override
	public String toString() {
		return getName();
	}
	
}