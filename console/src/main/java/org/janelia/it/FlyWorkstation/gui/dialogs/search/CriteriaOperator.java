package org.janelia.it.FlyWorkstation.gui.dialogs.search;

/**
 * Criterion operator on a SearchAttribute. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum CriteriaOperator {
	
	NOT_NULL("Is Not Empty"),
	CONTAINS("Contains"),
	BETWEEN("Is Between");
	
	private String label;
	
	private CriteriaOperator(String label) {
		this.label = label;
	}
	
	@Override
	public String toString() {
		return label;
	}
	
}