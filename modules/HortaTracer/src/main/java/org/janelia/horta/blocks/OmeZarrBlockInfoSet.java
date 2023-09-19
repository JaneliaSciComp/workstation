package org.janelia.horta.blocks;

import edu.wlu.cs.levy.CG.KDTree;
import org.janelia.geometry3d.ConstVector3;
import org.openide.util.Exceptions;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class OmeZarrBlockInfoSet implements Set<OmeZarrBlockTileKey> {

    // TODO - maybe just use the KDTree and not the HashSet?
    private final Set<OmeZarrBlockTileKey> set = new HashSet<>();
    private final KDTree<OmeZarrBlockTileKey> centroidIndex = new KDTree<>(3);

    private final double[] voxelSize;

    private final int[] chunkSize;

    public OmeZarrBlockInfoSet(double[] voxelSize, int[] chunkSize) {
        this.voxelSize = voxelSize;
        this.chunkSize = chunkSize;
    }
    public OmeZarrBlockTileKey getBestContainingBrick(ConstVector3 xyz, int maxCount) {
        double[] key = new double[]{xyz.getX(), xyz.getY(), xyz.getZ()};

        try {
            return centroidIndex.nearest(key);
        } catch (Exception ex) {
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
    public Iterator<OmeZarrBlockTileKey> iterator() {
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
    public boolean add(OmeZarrBlockTileKey brickInfo) {
        boolean result = set.add(brickInfo);

        // Insert into KDTree for quick search
        // TODO - update tree for all other inserting/deleting operations
        ConstVector3 cv = brickInfo.getCentroid();

        try {
            double[] ca = new double[]{cv.getX(), cv.getY(), cv.getZ()};
            centroidIndex.insert(ca, brickInfo);
        } catch (Exception ignored) {
        }

        return result;
    }

    @Override
    public boolean addAll(Collection<? extends OmeZarrBlockTileKey> c) {
        boolean result = false;

        for (OmeZarrBlockTileKey chunk : c) {
            result |= add(chunk);
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
