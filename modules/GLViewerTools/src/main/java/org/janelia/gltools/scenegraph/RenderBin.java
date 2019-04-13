package org.janelia.gltools.scenegraph;

/**
 *
 * @author Christopher Bruns
 */
public class RenderBin
{
    public static enum SortMode {DISTANCE, DONT_CARE};
    
    private SortMode sortMode;
    private int orderIndex;
    private String name;

    public RenderBin(int order, SortMode sortMode, String name) {
        this.name = name;
        this.orderIndex = order;
        this.sortMode = sortMode;
    }
}
