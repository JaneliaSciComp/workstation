
package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.WorkstationEnvironment;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardControllable;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.RenderablesLoadWorker;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.AlignmentBoardControlsDialog;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.ConfigurableColorMapping;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeBrickFactory;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * This test-run class / standalone program will pull in a mask/channel pair and display the results.
 *
 * @see org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder.TestMaskedMip3d
 *
 * @author fosterl
 */
@Category(TestCategories.InteractiveTests.class)
public class TestMaskChan3DViz {

    private static final Dimension FRAME_SIZE = new Dimension(100, 10);

    /**
	 * @param args filenames for all mask and all channel files, in that order, to be displayed.
	 */
	public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if ( args.length == 0 ) {
                    throw new IllegalArgumentException(
                            "Usage: java " + TestMaskChan3DViz.class.getName() +
                            " <mask-file> <chan-file> [<mask-file> <chan-file>]* "
                    );
                }

                if ( args.length % 2 != 0 ) {
                    throw new IllegalArgumentException("Need both mask and channel for all renderables.");
                }

                try {
                    new WorkstationEnvironment().invoke();

                    // Setup a testing color-wheel mapping.
                    JFrame frame = new JFrame("Test MipWidget for Masking");
                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    JLabel label = new JLabel("Test MipWidget for Masking");
                    frame.getContentPane().add(label);
                    frame.setSize( FRAME_SIZE );
                    Mip3d mipWidget = new Mip3d();

                    mipWidget.clear();

                    // Now, the vmb and the vcb are fully populated with all data. Can hand that into the
                    // mip3d.
                    AlignmentBoardSettings settings = new AlignmentBoardSettings();
                    settings.setShowChannelData( true );
                    settings.setGammaFactor( AlignmentBoardSettings.DEFAULT_GAMMA );
                    settings.setChosenDownSampleRate(AlignmentBoardControlsDialog.UNSELECTED_DOWNSAMPLE_RATE);

                    RenderMappingI renderMapping = new ConfigurableColorMapping();
                    RenderablesLoadWorker loadWorker = new RenderablesLoadWorker(
                            new Chan3DVizDataSource( args ),
                            renderMapping,
                            new TestControlCallback( mipWidget, renderMapping, frame.getContentPane() ),
                            settings,
                            new MultiMaskTracker()
                    );
                    //loadWorker.setResolver( new TrivialFileResolver() );
                    loadWorker.execute();

                    //Display the window.
                    //args.frame.pack();
                    frame.setSize( 800, 800 );
                    frame.setVisible(true);

                }
                catch (Exception exc) {
                	exc.printStackTrace();
                }
            }
        });
	}

    public static class TestControlCallback implements AlignmentBoardControllable {
        private Logger logger = LoggerFactory.getLogger( TestControlCallback.class );
        private Mip3d mip3d;
        private RenderMappingI renderMapping;

        private Container container;

        public TestControlCallback( Mip3d mip3d, RenderMappingI renderMapping, Container container ) {
            this.mip3d = mip3d;
            this.mip3d.resetView();
            this.renderMapping = renderMapping;
            this.container = container;
        }

        @Override
        public void clearDisplay() {
            mip3d.clear();
        }

        public void close() {
            mip3d.clear();
            System.exit( 0 );
        }

        @Override
        public void loadVolume(TextureDataI signalTexture, TextureDataI maskTexture) {
            mip3d.getVolumeModel().setGammaAdjustment( (float)AlignmentBoardSettings.DEFAULT_GAMMA );
            mip3d.getVolumeModel().setCameraDepth( new Vec3( 0.0, 0.0, 0.0 ) );
            VolumeBrickFactory volumeBrickFactory = new MultiTexVolumeBrickFactory();
            if ( ! mip3d.setVolume(
                    signalTexture, maskTexture, volumeBrickFactory, renderMapping, 1.0
            ) ) {
                logger.error( "Failed to load volume to mip3d." );
            }

        }

        @Override
        public void displayReady() {
            mip3d.refresh();

            // Strip any "show-loading" off the viewer.
            container.removeAll();

            // Add this last.  "show-loading" removes it.  This way, it is shown only
            // when it becomes un-busy.
            container.add(mip3d, BorderLayout.CENTER);
        }

        @Override
        public void loadCompletion(boolean successful, boolean loadFiles, Throwable error) {
            if ( successful ) {
                container.invalidate();
                container.validate();
                container.repaint();

                if ( loadFiles ) {
                    mip3d.refresh();
                }
                else {
                    mip3d.refreshRendering();
                }

            }
            else {
                container.removeAll();
                container.invalidate();
                container.validate();
                container.repaint();
                throw new RuntimeException( error );
            }
        }

        @Override
        public void renderModCompletion() {
            logger.info("Render Completion.");
        }
    }

}
