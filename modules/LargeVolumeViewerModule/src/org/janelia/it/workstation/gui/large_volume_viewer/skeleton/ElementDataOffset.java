package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

/**
 * Created by murphys on 4/18/2016.
 */
public class ElementDataOffset {
    public ElementDataOffset(Long id, int size, long offset) { this.id=id; this.size=size; this.offset=offset; }
    public Long id;
    public int size;
    public long offset;
}
