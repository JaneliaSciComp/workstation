package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeModel;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export.CropCoordSet;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 6/6/13
 * Time: 9:16 AM
 *
 * Tests the exchange of strings.
 */
public class UserSettingSerializerStringTest {

    final double doubleDelta = 0.1;

    @Test
    @Category(TestCategories.FastTests.class)
    public void testRoundTripParse() {
        VolumeModel volumeModel = new VolumeModel();
        volumeModel.setCropOutLevel( 0.5f );
        volumeModel.setGammaAdjustment(0.088f);
        CropCoordSet cropCoords = new CropCoordSet();
        float[] cropCoordArr = new float[] { 0.1f, 0.1f, 0.1f, 0.25f, 0.25f, 0.25f };
        cropCoords.setAcceptedCoordinates(Collections.singletonList( cropCoordArr )) ;
        volumeModel.setCropCoords( cropCoords );

        AlignmentBoardSettings settings = new AlignmentBoardSettings();
        UserSettingSerializer serializer;
        serializer = new UserSettingSerializer( null, volumeModel, settings );

        String settingsString = serializer.getSettingsString();

        VolumeModel returnedVolumeModel = new VolumeModel();
        AlignmentBoardSettings returnedAlignmentBoardSettings = new AlignmentBoardSettings();
        UserSettingSerializer outputSerializer = new UserSettingSerializer( null, returnedVolumeModel, returnedAlignmentBoardSettings );

        outputSerializer.parseSettings( settingsString );

        assertEquals("CropOutLevel Differs", volumeModel.getCropOutLevel(), returnedVolumeModel.getCropOutLevel(), doubleDelta);
        assertEquals("Gamma Adjustment Differs", volumeModel.getGammaAdjustment(), returnedVolumeModel.getGammaAdjustment(), doubleDelta);

        // DEBUG System.out.println("Crop Out Level = " + volumeModel.getCropOutLevel() + ", gamma adjustment = " + volumeModel.getGammaAdjustment() );
        float[] returnedCoordArr = returnedVolumeModel.getCropCoords().getAcceptedCoordinates().iterator().next();
        for ( int i = 0; i < returnedCoordArr.length; i++ ) {
            assertEquals("Coordinate " + i + " differs", returnedCoordArr[i], cropCoordArr[i], doubleDelta);
        }
    }

}
