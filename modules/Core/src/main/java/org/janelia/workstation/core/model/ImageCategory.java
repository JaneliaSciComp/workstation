package org.janelia.workstation.core.model;

public enum ImageCategory {

    Image3d("3d Image (e.g. LSM, PBD, H5J)"),
    Image2d("2d Image (e.g. MIP)");
    
    private final String label;
    
    private ImageCategory(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }

    public static ImageCategory getByLabel(String label) {
        ImageCategory[] values = ImageCategory.values();
        for (int i=0; i<values.length; i++) {
            if (values[i].getLabel().equals(label)) {
                return values[i];
            }
        }
        return null;
    }

}
