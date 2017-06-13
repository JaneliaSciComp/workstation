package org.janelia.it.workstation.browser.model;

public enum ResultCategory {

    OriginalLSM("Original LSM images"),
    PreAligned("Pre-alignment (e.g. merged, stitched)"),
    PostAligned("Post-alignment (e.g. JBA, ANTS, CMTK)");
    
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
