package org.janelia.it.FlyWorkstation.tracing;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.SharedVolumeImage;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TextureCache;

/**
 * this class encapsulates a request for a path to be traced from an
 * anchor to its parent; the request stores the image data, because
 * that's how we trace the path; the expected use pattern is:
 *
 * -- GUI creates the request and throws in first anchor (which was the click target)
 * -- 2D view adds in image data (because it has it)
 * -- something with access to model data identifies parent and adds second anchor and locations
 * -- request sent to tracing algorithm, which doesn't care about how any of the data got there
 *
 * djo, 1/14
 */
public class PathTraceToParentRequest {


    private Vec3 xyz1;
    private Vec3 xyz2;
    private Long anchorGuid1;
    private Long anchorGuid2;
    private SegmentIndex segmentIndex; // for hashing

    private SharedVolumeImage imageVolme;
    private TextureCache textureCache;


    public PathTraceToParentRequest(Long anchorGuid) {
        setAnchorGuid1(anchorGuid);
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
            if (other.getSegmentIndex() != null)
                return false;
        } else if (!segmentIndex.equals(other.getSegmentIndex()))
            return false;
        return true;
    }

    public SegmentIndex getSegmentIndex() {
        return segmentIndex;
    }

    public Vec3 getXyz1() {
        return xyz1;
    }

    public void setXyz1(Vec3 xyz1) {
        this.xyz1 = xyz1;
    }

    public Vec3 getXyz2() {
        return xyz2;
    }

    public void setXyz2(Vec3 xyz2) {
        this.xyz2 = xyz2;
    }

    public Long getAnchorGuid1() {
        return anchorGuid1;
    }

    public void setAnchorGuid1(Long anchorGuid1) {
        this.anchorGuid1 = anchorGuid1;
    }

    public Long getAnchorGuid2() {
        return anchorGuid2;
    }

    public void setAnchorGuid2(Long anchorGuid2) {
        this.anchorGuid2 = anchorGuid2;
    }

    public SharedVolumeImage getImageVolme() {
        return imageVolme;
    }

    public void setImageVolme(SharedVolumeImage imageVolme) {
        this.imageVolme = imageVolme;
    }

    public TextureCache getTextureCache() {
        return textureCache;
    }

    public void setTextureCache(TextureCache textureCache) {
        this.textureCache = textureCache;
    }


}
