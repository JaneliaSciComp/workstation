package org.janelia.it.FlyWorkstation.tracing;

import org.janelia.it.FlyWorkstation.geom.Vec3;

public class PathTraceRequest {

    private Vec3 xyz1;
    private Vec3 xyz2;
    private Long anchorGuid1;
    private Long anchorGuid2;
    private SegmentIndex segmentIndex; // for hashing

    public PathTraceRequest(Vec3 xyz1, Vec3 xyz2, Long guid1, Long guid2) {
        this.xyz1 = xyz1;
        this.xyz2 = xyz2;
        this.anchorGuid1 = guid1;
        this.anchorGuid2 = guid2;
        this.segmentIndex = new SegmentIndex(guid1, guid2);
    }

    public Vec3 getXyz1() {
        return xyz1;
    }

    public Vec3 getXyz2() {
        return xyz2;
    }
    
    @Override
	public int hashCode() {
		return segmentIndex.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PathTraceRequest other = (PathTraceRequest) obj;
		if (segmentIndex == null) {
			if (other.segmentIndex != null)
				return false;
		} else if (!segmentIndex.equals(other.segmentIndex))
			return false;
		return true;
	}

	public SegmentIndex getSegmentIndex() {
		return segmentIndex;
	}

	public static class SegmentIndex {
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

    }

    public long getAnchor1Guid() {
        return anchorGuid1;
    }

    public long getAnchor2Guid() {
        return anchorGuid2;
    }

}
