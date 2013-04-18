package org.janelia.it.FlyWorkstation.gui.viewer3d;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/18/13
 * Time: 5:17 PM
 *
 * Use of this object allows values to be set for the use of the Volume Brick, but while preserving
 * the GLActor abstraction.
 */
public class VolumeModel {
    private float[] cropCoords;
    private float gammaAdjustment = 1.0f;
    private float cropOutLevel = Mip3d.DEFAULT_CROPOUT;
    private float[] colorMask = { 1.0f, 1.0f, 1.0f };

    public float[] getCropCoords() {
        return cropCoords;
    }

    public void setCropCoords(float[] cropCoords) {
        this.cropCoords = cropCoords;
    }

    public float getGammaAdjustment() {
        return gammaAdjustment;
    }

    public void setGammaAdjustment(float gammaAdjustment) {
        this.gammaAdjustment = gammaAdjustment;
    }

    public float getCropOutLevel() {
        return cropOutLevel;
    }

    public void setCropOutLevel(float cropOutLevel) {
        this.cropOutLevel = cropOutLevel;
    }

    public float[] getColorMask() {
        return colorMask;
    }

    public void setColorMask(float[] colorMask) {
        this.colorMask = colorMask;
    }
}
