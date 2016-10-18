package org.janelia.it.workstation.shared.util;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
* This class implements a Hashtable that allows for key collisions.  All elements
* within the Hashtable are Vectors.  Should a collision occur, the element will be
* added to the Vector.  All get methods will return the Vector for the key.  size
* and contains methods have been overridden to take the Vectors into account
*
*  Initially Written by: Peter Davies
*
*/
public class MultiHash extends Hashtable implements Serializable {
    /**
     * Put a value into the multiHash...
     * If we don't have a vector fo this key, create one.
     * Add this value to the vector...
     */
    public synchronized Object put(Object key, Object value) {
        Vector v;

        if (!this.containsKey(key)) {
            v = new Vector();
            v.add(value);
            super.put(key, v);

            return null;
        }

        v = (Vector) get(key);
        v.addElement(value);

        return v;
    }

    public int size() {
        int size = 0;

        for (Enumeration e = this.elements(); e.hasMoreElements();) {
            size += ((Vector) e.nextElement()).size();
        }

        return size;
    }

    public synchronized boolean contains(Object value) {
        boolean found = false;

        for (Enumeration e = this.elements(); e.hasMoreElements();) {
            found = ((Vector) e.nextElement()).contains(value);

            if (found) {
                break;
            }
        }

        return found;
    }
}

