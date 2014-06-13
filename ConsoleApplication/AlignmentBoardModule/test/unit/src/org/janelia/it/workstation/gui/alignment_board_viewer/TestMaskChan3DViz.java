
package org.janelia.it.workstation.gui.alignment_board_viewer;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.WorkstationEnvironment;
import org.janelia.it.workstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements.AlignmentBoardControlsDialog;
import org.janelia.it.workstation.gui.alignment_board_viewer.masking.ConfigurableColorMapping;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.VolumeBrickActorBuilder;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;
import org.janelia.it.workstation.gui.viewer3d.VolumeBrickFactory;
import org.janelia.it.workstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * This test-run class / standalone program will pull in a mask/channel pair and display the results.
 *
 * @see org.janelia.it.workstation.gui.alignment_board_viewer.volume_builder.TestMaskedMip3d
 *
 * @author fosterl
 */
@Category(TestCategories.InteractiveTests.class)
public class TestMaskChan3DViz {

    private static final Dimension FRAME_SIZE = new Dimension(100, 10);
    // The launching of a "workstation environment" facilitates the use of the database for fetching
    // input files.  However, it also _forces_ that to happen.  This boolean switch can be used
    // to make that go whichever way is needed.
    private static final boolean USE_LOCAL_RESOURCE_DEFAULT = true;
    private static boolean useLocalResource = USE_LOCAL_RESOURCE_DEFAULT;

    /**
	 * @param args filenames for all mask and all channel files, in that order, to be displayed.
	 */
	public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if ( args.length == 0 ) {
                    throw new IllegalArgumentException(
                            "Usage: java " + TestMaskChan3DViz.class.getName() +
                            " <mask-file> <chan-file> [<mask-file> <chan-file>]* (use-local-resources)" +
                            "\n       Where optional final boolean says 'use a file', not 'use a URL'"
                    );
                }

                // Note: any multiple of 2 implies pairs of files, m/c.
                if ( args.length < 2 ) {
                    throw new IllegalArgumentException("Need both mask and channel for all renderables.");
                }

                // Beyond final of the group may be a boolean.
                if ( args.length % 2 > 0 ) {
                    useLocalResource = Boolean.parseBoolean( args[ args.length - 1 ] );
                }

                try {
                    if ( ! useLocalResource ) {
                        new WorkstationEnvironment().invoke();
                    }

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
                            new Chan3DVizDataSource( roundDownArgs( args ) ),
                            renderMapping,
                            new TestControlCallback( mipWidget, renderMapping, frame.getContentPane() ),
                            settings,
                            new MultiMaskTracker()
                    );
                    if ( useLocalResource ) {
                        loadWorker.setResolver( new TrivialFileResolver() );
                    }
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

    /** This ensures that we have only an even number of arguments to pass along. */
    private static final String[] roundDownArgs( String[] args ) {
        int arraySize = args.length - ( args.length % 2 );
        String[] finalArgs = new String[ arraySize ];
        for ( int i = 0; i < finalArgs.length; i++ ) {
            finalArgs[ i ] = args[ i ];
        }
        return finalArgs;
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
            VolumeModel volumeModel = mip3d.getVolumeModel();
            volumeModel.setGammaAdjustment((float) AlignmentBoardSettings.DEFAULT_GAMMA);
            volumeModel.setCameraDepth(new Vec3(0.0, 0.0, 0.0));
            VolumeBrickFactory volumeBrickFactory = new MultiTexVolumeBrickFactory();
            VolumeBrickActorBuilder actorBuilder = new VolumeBrickActorBuilder();
            GLActor brickActor = actorBuilder.buildVolumeBrickActor(volumeModel, signalTexture, maskTexture, volumeBrickFactory, renderMapping);
            if ( brickActor == null ) {
                logger.error( "Failed to load volume to mip3d." );
            }
            else {
                GLActor axesActor = actorBuilder.buildAxesActor(brickActor.getBoundingBox3d(), 1.0);
                boolean isMac = SystemInfo.OS_NAME.contains("mac");
                if ( isMac ) {
                    // Enforce opaque, transparent ordering of actors.
                    mip3d.addActor( brickActor );
                }
                if ( axesActor != null ) {
                    mip3d.addActor( axesActor );
                }
                if ( ! isMac ) {
                    // Must add the brick _after_ the axes for non-Mac systems.
                    mip3d.addActor( brickActor );
                }
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
