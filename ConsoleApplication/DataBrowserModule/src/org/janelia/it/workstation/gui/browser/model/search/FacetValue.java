package org.janelia.it.workstation.gui.browser.model.search;

/**
 * The count for a facet value. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FacetValue {
    
    private final String value;
    private final long count;

    public FacetValue(String value, long count) {
        this.value = value;
        this.count = count;
    }

    public String getValue() {
        return value;
    }

    public long getCount() {
        return count;
    }
}