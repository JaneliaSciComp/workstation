package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import junit.framework.Assert;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeModel;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export.CropCoordSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.UserSettingSerializer;

/**
 * Created with IntelliJ IDEA. User: fosterl Date: 6/6/13 Time: 9:16 AM
 *
 * Tests the exchange of strings.
 */
public class UserSettingSerializerStringTest {

    @Before
    public void init() {
    }

    @After
    public void close() {

    }

    @Test
    public void testRoundTripParse() {
        VolumeModel volumeModel = new VolumeModel();
        volumeModel.setCropOutLevel(0.5f);
        volumeModel.setGammaAdjustment(0.088f);
        CropCoordSet cropCoords = new CropCoordSet();
        float[] cropCoordArr = new float[]{0.1f, 0.1f, 0.1f, 0.25f, 0.25f, 0.25f};
        cropCoords.setAcceptedCoordinates(Collections.singletonList(cropCoordArr));
        volumeModel.setCropCoords(cropCoords);

        AlignmentBoardSettings settings = new AlignmentBoardSettings();
        UserSettingSerializer serializer;
        serializer = new UserSettingSerializer(null, volumeModel, settings);

        String settingsString = serializer.getSettingsString();

        VolumeModel returnedVolumeModel = new VolumeModel();
        AlignmentBoardSettings returnedAlignmentBoardSettings = new AlignmentBoardSettings();
        UserSettingSerializer outputSerializer = new UserSettingSerializer(null, returnedVolumeModel, returnedAlignmentBoardSettings);

        outputSerializer.parseSettings(settingsString);

        Assert.assertEquals("CropOutLevel Differs", volumeModel.getCropOutLevel(), returnedVolumeModel.getCropOutLevel());
        Assert.assertEquals("Gamma Adjustment Differs", volumeModel.getGammaAdjustment(), returnedVolumeModel.getGammaAdjustment());

        // DEBUG System.out.println("Crop Out Level = " + volumeModel.getCropOutLevel() + ", gamma adjustment = " + volumeModel.getGammaAdjustment() );
        float[] returnedCoordArr = returnedVolumeModel.getCropCoords().getAcceptedCoordinates().iterator().next();
        for (int i = 0; i < returnedCoordArr.length; i++) {
            Assert.assertEquals("Coordinate " + i + " differs", returnedCoordArr[ i], cropCoordArr[ i]);
        }
    }

}
