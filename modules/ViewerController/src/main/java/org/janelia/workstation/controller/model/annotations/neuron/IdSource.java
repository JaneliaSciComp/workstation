package org.janelia.workstation.controller.model.annotations.neuron;

import org.janelia.workstation.integration.util.FrameworkAccess;

import java.util.Iterator;
import java.util.List;

/**
 * This is a fixed-block-oriented ID source, pulling IDs from an
 * id generator in such a way to optimize database
 * use, but appear seamless to caller.
 *
 * @author fosterl
 */
public class IdSource implements Iterator<Long> {
    private int positionInList = 0;
    private List<Long> ids = null;
    private int blockSize;
    
    public IdSource(int blockSize) {
        this.blockSize = blockSize;
        refreshIdList();
    }
    
    public IdSource() {
        this(10000);
    }
    
    @Override
    public Long next() {
        if (positionInList >= blockSize) {
            refreshIdList();
        }
        return ids.get(positionInList ++);
    }
    
    private void refreshIdList() {
        ids = FrameworkAccess.generateGUIDs(blockSize);
        positionInList = 0;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }

}
