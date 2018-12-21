package org.janelia.it.workstation.gui.task_workflow;

import org.janelia.it.jacs.shared.geom.Vec3;

/**
 *
 * @author schauderd
 * interface represent the way the review points are shown in the GUI.  For neurons,
 * this is using a dendrogram.  For automated points, this might be slightly different.
 */


public interface PointDisplay {
    public void setReviewed(boolean review);
    public Vec3 getVertexLocation();
}
