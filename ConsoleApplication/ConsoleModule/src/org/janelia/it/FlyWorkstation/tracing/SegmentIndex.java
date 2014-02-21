package org.janelia.it.FlyWorkstation.tracing;

public class SegmentIndex {
    private long smallerGuid;
    private long largerGuid;

    public SegmentIndex(long anchor1Guid, long anchor2Guid) {
        if (anchor1Guid <= anchor2Guid) {
            smallerGuid = anchor1Guid;
            largerGuid = anchor2Guid;
        }
        else {
            smallerGuid = anchor2Guid;
            largerGuid = anchor1Guid;
        }
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (largerGuid ^ (largerGuid >>> 32));
        result = prime * result
                + (int) (smallerGuid ^ (smallerGuid >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SegmentIndex other = (SegmentIndex) obj;
        if (largerGuid != other.largerGuid)
            return false;
        if (smallerGuid != other.smallerGuid)
            return false;
        return true;
    }

    public long getAnchor1Guid() {
        return smallerGuid;
    }

    public long getAnchor2Guid() {
        return largerGuid;
    }

}