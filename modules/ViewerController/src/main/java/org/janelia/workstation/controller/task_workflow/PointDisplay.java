package org.janelia.workstation.controller.task_workflow;

import org.janelia.workstation.geom.Vec3;

/**
 *
 * @author schauderd
 * interface represent the way the review points are shown in the GUI.  For neurons,
 * this is using a dendrogram.  For automated points, this might be slightly different.
 */


public interface PointDisplay {
    public boolean isReviewed();
    public void setReviewed(boolean review);
    public Vec3 getVertexLocation();
    public boolean isFolded();
    public void toggleFolded();
}
