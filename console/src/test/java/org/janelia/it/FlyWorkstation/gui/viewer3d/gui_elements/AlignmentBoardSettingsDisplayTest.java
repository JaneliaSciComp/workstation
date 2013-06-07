package org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeModel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.CropCoordSet;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/18/13
 * Time: 3:48 PM
 *
 * Simple main-run program to show how the AB settings dialog looks.  No functional tests performed against
 * action listeners, etc.
 */
public class AlignmentBoardSettingsDisplayTest {
    public static void main(String[] args) {
        VolumeModel volumeModel = new VolumeModel();
        volumeModel.setCropCoords( new CropCoordSet() );
        AlignmentBoardControlsDialog testDialog = new AlignmentBoardControlsDialog(
                new JFrame(), volumeModel
        );
        testDialog.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing( e );
                System.exit( 0 );
            }
        });
        testDialog.setVisible( true );
    }
}
