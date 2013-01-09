package org.janelia.it.FlyWorkstation.gui.framework.outline.ab;

import java.util.List;

public interface AlignedItem {
    
    public String getName();
    
    public List<? extends AlignedItem> getChildren();
    
}
