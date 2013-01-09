package org.janelia.it.FlyWorkstation.gui.framework.outline.ab;

import java.util.List;

/**
 * An aligned item which can be visualized on an alignment board.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignedItemFolder implements AlignedItem {

    private String name;
    private List<? extends AlignedItem> children;

    public AlignedItemFolder(String name, List<? extends AlignedItem> children) {
        this.name = name;
        this.children = children;
    }
    
    public String getName() {
        return name;
    }
    
    public List<? extends AlignedItem> getChildren() {
        return children;
    }
}
