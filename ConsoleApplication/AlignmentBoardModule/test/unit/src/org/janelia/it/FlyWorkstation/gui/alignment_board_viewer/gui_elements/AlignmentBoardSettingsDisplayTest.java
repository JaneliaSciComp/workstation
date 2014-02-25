package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements;

import org.janelia.it.FlyWorkstation.gui.WorkstationEnvironment;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeModel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CropCoordSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static Logger logger = LoggerFactory.getLogger( AlignmentBoardSettingsDisplayTest.class );
    public static void main(String[] args) {
        logger.info( "Run this to see how the Alignment Board Settings panel looks, and play with controls." );
        // Setup mock WS environment for this test.
        new WorkstationEnvironment().invoke();
        VolumeModel volumeModel = new VolumeModel();
        volumeModel.setCropCoords( new CropCoordSet() );
        AlignmentBoardControlsDialog testDialog = new AlignmentBoardControlsDialog(
                new JFrame(), volumeModel, new AlignmentBoardSettings()
        );
        testDialog.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing( e );
                System.exit( 0 );
            }

            @Override
            public void windowClosed(WindowEvent e) {
                System.exit( 0 );
            }
        });
        testDialog.setVisible( true );
    }
}
