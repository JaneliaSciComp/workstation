/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.horta.volume;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.janelia.geometry3d.Vector3;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class BrickInfoSet implements Set<BrickInfo> {

    // TODO - maybe just use the KDTree and not the HashSet?
    private final Set<BrickInfo> set = new HashSet<>();
    private final KDTree<BrickInfo> centroidIndex = new KDTree<>(3);
    
    public BrickInfo getBestContainingBrick(Vector3 xyz) {
        double[] key = new double[] {xyz.getX(), xyz.getY(), xyz.getZ()};
        try {
            return centroidIndex.nearest(key);
        } catch (KeySizeException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }
    
    @Override
    public int size() {
        return centroidIndex.size();
    }

    @Override
    public boolean isEmpty() {
        return centroidIndex.size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public Iterator<BrickInfo> iterator() {
        return set.iterator();
    }

    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return set.toArray(a);
    }

    @Override
    public boolean add(BrickInfo brickInfo) {
        boolean result = set.add(brickInfo);
        
        // Insert into KDTree for quick search
        // TODO - update tree for all other inserting/deleting operations
        Vector3 cv = brickInfo.getBoundingBox().getCentroid();
        double[] ca = new double[] {cv.getX(), cv.getY(), cv.getZ()};
        try {
            centroidIndex.insert(ca, brickInfo);
        } catch (KeySizeException ex) {
            Exceptions.printStackTrace(ex);
        } catch (KeyDuplicateException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return result;
    }

    @Override
    public boolean remove(Object o) {
        return set.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return set.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends BrickInfo> c) {
        return set.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return set.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return set.removeAll(c);
    }

    @Override
    public void clear() {
        set.clear();
    }
    
}
