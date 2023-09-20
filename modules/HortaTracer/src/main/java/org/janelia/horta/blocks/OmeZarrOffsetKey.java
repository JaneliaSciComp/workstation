package org.janelia.horta.blocks;

import java.util.Arrays;

public class OmeZarrOffsetKey {

    private final int[] readOffset;

    public OmeZarrOffsetKey(int[] readOffset) {
        this.readOffset = readOffset;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return 53 * hash + Arrays.hashCode(this.readOffset);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OmeZarrOffsetKey other = (OmeZarrOffsetKey) obj;

        return Arrays.equals(readOffset, other.readOffset);
    }
}