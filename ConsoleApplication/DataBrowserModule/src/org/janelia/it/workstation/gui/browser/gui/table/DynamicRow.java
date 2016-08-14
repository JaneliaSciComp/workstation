package org.janelia.it.workstation.gui.browser.gui.table;

/**
 * A row in a dynamic table.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicRow {
	
	private Object userObject;

	public DynamicRow(Object userObject) {
		this.userObject = userObject;
	}

	public Object getUserObject() {
		return userObject;
	}
}