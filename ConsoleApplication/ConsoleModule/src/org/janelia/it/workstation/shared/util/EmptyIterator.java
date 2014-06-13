package org.janelia.it.workstation.shared.util;

import java.util.Iterator;

public class EmptyIterator implements Iterator {

    public boolean hasNext() {
        return false;
    }

    public Object next() {
        return null;
    }

    public void remove() {
    }
}
