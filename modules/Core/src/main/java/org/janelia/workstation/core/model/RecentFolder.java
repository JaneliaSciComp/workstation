package org.janelia.workstation.core.model;

public class RecentFolder {
    
    private String path;
    private String label;
    
    public RecentFolder(String path, String label) {
        this.path = path;
        this.label = label;
    }
    
    public String getPath() {
        return path;
    }

    public String getLabel() {
        return label;
    }
    
}