package org.janelia.it.workstation.tracing;

public class PathTraceRequest {

    private org.janelia.it.workstation.geom.Vec3 xyz1;
    private org.janelia.it.workstation.geom.Vec3 xyz2;
    private Long anchorGuid1;
    private Long anchorGuid2;
    private SegmentIndex segmentIndex; // for hashing

    public PathTraceRequest(org.janelia.it.workstation.geom.Vec3 xyz1, org.janelia.it.workstation.geom.Vec3 xyz2, Long guid1, Long guid2) {
        this.xyz1 = xyz1;
        this.xyz2 = xyz2;
        this.anchorGuid1 = guid1;
        this.anchorGuid2 = guid2;
        this.segmentIndex = new SegmentIndex(guid1, guid2);
    }

    public org.janelia.it.workstation.geom.Vec3 getXyz1() {
        return xyz1;
    }

    public org.janelia.it.workstation.geom.Vec3 getXyz2() {
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

    public long getAnchor1Guid() {
        return anchorGuid1;
    }

    public long getAnchor2Guid() {
        return anchorGuid2;
    }

}
