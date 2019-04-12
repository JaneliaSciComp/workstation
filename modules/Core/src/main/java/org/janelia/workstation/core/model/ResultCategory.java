package org.janelia.workstation.core.model;

public enum ResultCategory {

    ORIGINAL("Original LSM images"),
    PROCESSED("Processed (e.g. merged, stitched)"),
    POST_PROCESSED("Post-processed (e.g. polarity masking)"),
    ALIGNED("Aligned (e.g. JBA, ANTS, CMTK)");
    
    private final String label;
    
    private ResultCategory(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }

    public static ResultCategory getByLabel(String label) {
        ResultCategory[] values = ResultCategory.values();
        for (int i=0; i<values.length; i++) {
            if (values[i].getLabel().equals(label)) {
                return values[i];
            }
        }
        return null;
    }

}
