package org.janelia.it.FlyWorkstation.tracing;

import org.janelia.it.FlyWorkstation.geom.Vec3;

public class PathTraceRequest {

    private Vec3 xyz1;
    private Vec3 xyz2;
    private Long anchorGuid1;
    private Long anchorGuid2;

    public PathTraceRequest(Vec3 xyz1, Vec3 xyz2, Long guid1, Long guid2) {
        this.xyz1 = xyz1;
        this.xyz2 = xyz2;
        this.anchorGuid1 = guid1;
        this.anchorGuid2 = guid2;
    }

    public Vec3 getXyz1() {
        return xyz1;
    }

    public Vec3 getXyz2() {
        return xyz2;
    }

}
