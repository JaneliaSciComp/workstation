package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.geom.Rotation3d;
import org.janelia.it.FlyWorkstation.geom.UnitVec3;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export.CropCoordSet;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 10/23/13
 * Time: 5:36 PM
 *
 * Bag-o-data impl of serialization adapter.  Good for pulling the serialization settings back for alternate uses.
 */
public class SerializationAdapterBean implements UserSettingSerializer.SerializationAdapter {
    private float gamma;
    private float cropOutLevel;
    private long minimumVoxelCount;
    private Rotation3d rotation;
    private boolean showChannelData;
    private boolean saveColorBrightness;
    private Vec3 focus;
    private Vec3 focusInGround;
    private CropCoordSet cropCoordSet;

    @Override
    public float getGammaAdjustment() {
        return gamma;
    }

    @Override
    public float getCropOutLevel() {
        return cropOutLevel;
    }

    @Override
    public long getMinimumVoxelCount() {
        return minimumVoxelCount;
    }

    @Override
    public Rotation3d getRotation() {
        return rotation;
    }

    @Override
    public boolean isShowChannelData() {
        return showChannelData;
    }

    @Override
    public Vec3 getFocusInGround() {
        return focusInGround;
    }

    @Override
    public Vec3 getFocus() {
        return focus;
    }

    @Override
    public CropCoordSet getCropCoords() {
        return cropCoordSet;
    }

    @Override
    public boolean isSaveColorBrightness() {
        return saveColorBrightness;
    }

    @Override
    public void setSaveColorBrightness(boolean b) {
        saveColorBrightness = b;
    }

    @Override
    public void setGammaAdjustment(float gamma) {
        this.gamma = gamma;
    }

    @Override
    public void setCropOutLevel(float level) {
        this.cropOutLevel = level;
    }

    @Override
    public void setMinimumVoxelCount(long count) {
        this.minimumVoxelCount = count;
    }

    @Override
    public void setCropCoords(CropCoordSet coordSet) {
        this.cropCoordSet = coordSet;
    }

    @Override
    public void setShowChannelData(boolean show) {
        showChannelData = show;
    }

    @Override
    public void setFocus(double[] focus) {
        this.focus = new Vec3( focus[ 0 ], focus[ 1 ], focus[ 2 ] );
    }

    @Override
    public void setFocusInGround(double[] cameraFocusArr) {
        this.focusInGround = new Vec3( cameraFocusArr[ 0 ], cameraFocusArr[ 1 ], cameraFocusArr[ 2 ] );
    }

    @Override
    public void setRotation(Collection<double[]> rotation) {
        int i = 0;

        this.rotation = new Rotation3d();
        for ( double[] coordinateSet: rotation ) {
            this.rotation.setWithCaution(
                    i++,
                    new UnitVec3(coordinateSet[0], coordinateSet[1], coordinateSet[2])
            );
        }
    }
}
