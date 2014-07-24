package org.janelia.it.workstation.gui.alignment_board_viewer;

import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.UnitVec3;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.viewer3d.CropCoordSet;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;

import java.util.Collection;

/**
 * This implementation of a serialization adapter will push its values directly/pull directly from the
 * objects whose state are to be serialized.
 */
public class DirectStateSerializationAdapter implements UserSettingSerializer.SerializationAdapter {

    private static final int X_OFFS = 0;
    private static final int Y_OFFS = 1;
    private static final int Z_OFFS = 2;

    private VolumeModel volumeModel;
    private AlignmentBoardSettings settings;
    public DirectStateSerializationAdapter(VolumeModel volumeModel, AlignmentBoardSettings settings) {
        this.volumeModel = volumeModel;
        this.settings = settings;
    }


    @Override
    public float getGammaAdjustment() {
        return volumeModel.getGammaAdjustment();
    }

    @Override
    public float getCropOutLevel() {
        return volumeModel.getCropOutLevel();
    }

    @Override
    public long getMinimumVoxelCount() {
        return settings.getMinimumVoxelCount();
    }

    @Override
    public long getMaximumNeuronCount() {
        return settings.getMaximumNeuronCount();
    }

    @Override
    public Rotation3d getRotation() {
        return volumeModel.getCamera3d().getRotation();
    }

    @Override
    public boolean isShowChannelData() {
        return settings.isShowChannelData();
    }

    @Override
    public Vec3 getCameraDepth() {
        return volumeModel.getCameraDepth();
    }

    @Override
    public Vec3 getFocus() {
        return volumeModel.getCamera3d().getFocus();
    }

    @Override
    public CropCoordSet getCropCoords() {
        return volumeModel.getCropCoords();
    }

    @Override
    public boolean isSaveColorBrightness() {
        return volumeModel.isColorSaveBrightness();
    }

    @Override
    public void setSaveColorBrightness(boolean b) {
        volumeModel.setColorSaveBrightness( b );
    }

    @Override
    public boolean isWhiteBackground() {        
        return volumeModel.isWhiteBackground();
    }

    @Override
    public void setWhiteBackground(boolean b) {
        volumeModel.setWhiteBackground( b );
    }

    @Override
    public void setGammaAdjustment(float gamma) {
        settings.setGammaFactor( gamma );
        volumeModel.setGammaAdjustment(gamma);
    }        

    @Override
    public void setCropOutLevel(float level) {
        volumeModel.setCropOutLevel(level);
    }

    @Override
    public void setMinimumVoxelCount(long count) {
        settings.setMinimumVoxelCount(count);
    }

    @Override
    public void setMaximumNeuronCount(long count) {
        settings.setMaximumNeuronCount(count);
    }

    @Override
    public void setCropCoords(CropCoordSet coordSet) {
        // Stuff _any_one_ of the accepted coords into the current one, so that selection reflects
        // something accurate.
        float[] cropCoordArray = coordSet.getAcceptedCoordinates().iterator().next();
        if ( cropCoordArray != null ) {
            coordSet.setCurrentCoordinates( cropCoordArray );
        }
        volumeModel.setCropCoords(coordSet);
    }

    @Override
    public void setShowChannelData(boolean show) {
        settings.setShowChannelData(show);
    }

    @Override
    public void setFocus(double[] cameraFocusArr ) {
        volumeModel.getCamera3d().setFocus(cameraFocusArr[X_OFFS], cameraFocusArr[Y_OFFS], cameraFocusArr[Z_OFFS]);
    }

    @Override
    public void setCameraDepth(double[] cameraDepthArr) {
        volumeModel.setCameraDepth( new Vec3( cameraDepthArr[0], cameraDepthArr[1], cameraDepthArr[2] ));
    }

    @Override
    public void setRotation(Collection<double[]> coordSets) {
        int i = 0;
        for ( double[] coordinateSet: coordSets ) {
            volumeModel.getCamera3d().getRotation().setWithCaution(
                    i++,
                    new UnitVec3(coordinateSet[X_OFFS], coordinateSet[Y_OFFS], coordinateSet[Z_OFFS])
            );
        }
    }

}
