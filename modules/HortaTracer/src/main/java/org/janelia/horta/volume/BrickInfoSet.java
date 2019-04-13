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
        } catch (KeySizeException | ArrayIndexOutOfBoundsException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }
    
    public Collection<BrickInfo> getClosestBricks(float[] xyz, int count) {
        double[] key = new double[] {xyz[0], xyz[1], xyz[2]};
        try {
            return centroidIndex.nearest(key, count);
        } catch (KeySizeException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
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
