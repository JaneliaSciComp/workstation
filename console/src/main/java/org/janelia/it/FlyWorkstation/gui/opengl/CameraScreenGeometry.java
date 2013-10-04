package org.janelia.it.FlyWorkstation.gui.opengl;

import org.janelia.it.FlyWorkstation.gui.camera.Camera3d;

/**
 * Data structure containing information necessary to create perspective projection.
 * @author brunsc
 *
 */
public class CameraScreenGeometry {

    private Camera3d camera;
    private double screenEyeDistanceCm;
    private double screenPixelsPerCm;
    private double intraocularDistanceCm;
    
    public CameraScreenGeometry(Camera3d camera,
            double screenEyeDistanceCm,
            double screenPixelsPerCm,
            double intraOcularDistanceCm)
    {
        this.camera = camera;
        this.screenEyeDistanceCm = screenEyeDistanceCm;
        this.screenPixelsPerCm = screenPixelsPerCm;
        this.intraocularDistanceCm = intraOcularDistanceCm;
    }

    public Camera3d getCamera() {
        return camera;
    }

    public double getScreenEyeDistanceCm() {
        return screenEyeDistanceCm;
    }

    public double getScreenPixelsPerCm() {
        return screenPixelsPerCm;
    }

    public double getIntraocularDistanceCm() {
        return intraocularDistanceCm;
    }

}
