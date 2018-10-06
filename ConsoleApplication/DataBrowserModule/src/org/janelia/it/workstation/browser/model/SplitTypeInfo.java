package org.janelia.it.workstation.browser.model;

/**
 * For a given fly line, does it have an AD or DBD split half available? 
 *
 * @author rokickik
 */
public class SplitTypeInfo {

    private String fragName;
    private boolean hasAD;
    private boolean hasDBD;
    
    public SplitTypeInfo(String fragName, boolean hasAD, boolean hasDBD) {
        this.fragName = fragName;
        this.hasAD = hasAD;
        this.hasDBD = hasDBD;
    }
        
    public String getFragName() {
        return fragName;
    }
    
    public boolean hasAD() {
        return hasAD;
    }
    
    public boolean hasDBD() {
        return hasDBD;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SplitTypeInfo[");
        if (fragName != null) {
            builder.append("fragName=");
            builder.append(fragName);
            builder.append(", ");
        }
        builder.append("hasAD=");
        builder.append(hasAD);
        builder.append(", hasDBD=");
        builder.append(hasDBD);
        builder.append("]");
        return builder.toString();
    }
}
