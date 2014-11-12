/**
 * 
 */

package org.janelia.it.workstation.gui.viewer3d;

import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.static_view.RGBExcludableVolumeBrick;
import org.janelia.it.workstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;

import javax.swing.*;

/**
 * @author brunsc
 *
 */
public class TestMip3d {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Test MipWidget");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                JLabel label = new JLabel("Test MipWidget");
                frame.getContentPane().add(label);
                Mip3d mipWidget = new Mip3d();
                mipWidget.clear();
                try {
                	// mipWidget.loadVolume("/Users/brunsc/smallRefTest.tif");

                    // This load-up works perfectly.
                    //      String fn = "/Volumes/jacsData/brunsTest/3d_test_images/ConsolidatedSignal2_25.v3dpbd";
                    //      String fn = "/Volumes/flylight/LES Foster/B1_T1_20120427_1CC_38C04wtFlp_2D_R1_L02.lsm";
                    //      String fn = "/Volumes/flylight/flip/confocalStacks/20120929/FLFL_20121001154228067_25581.lsm";
                    //      String fn = "/Users/fosterl/Documents/LSM_3D/FLFL_20121001154228067_25581.lsm";
                    //String fn = "/Volumes/flylight/RJ/For Les Foster/B01_T01_20121023_PMB1_MB057B_20X_R1_L01.lsm";
                    //                    String fn = "/Volumes/jacsData/filestore/MaskResources/GiantFiber/guide/EPA_R.v3dpbd";
                    // Checked 11/7/2014.
                    //String fn = "/Volumes/nobackup/jacs/jacsData/filestore/MaskResources/GiantFiber/guide/LAL_L.v3dpbd";
                    // Checked 11/7/2014.
                    //String fn = "/Volumes/nobackup/jacs/jacsData/filestore/system/Separation/296/418/1778029752666296418/separate/fastload/Reference2_50.mp4";
                    //String fileBase = "/Users/fosterl/Documents/alignment_board/samples/";
                    //String fn = fileBase + "1735579170638921826/ConsolidatedSignal2_25.mp4";
                    // Black as of 11/7/2014.
                    //String fn = "/Volumes/nobackup/jacs/jacsData/filestore/system/Separation/951/842/1742124964321951842/separate/ConsolidatedLabel.v3dpbd";
                    // Checked 11/7/2014.
                    //String fn = "/Volumes/nobackup/jacs/jacsData/filestore/system/Separation/294/370/1742138165818294370/separate/fastLoad/ConsolidatedSignal2_25.mp4";

                    // WORKING:
                    // Compartment masks.
                    // Checked 11/7/2014 (all).
                    String guideBase = "/Volumes/nobackup/jacs/jacsData/filestore/MaskResources/Compartment/guide/";
                    String fn = guideBase + "LOP_R.v3dpbd";
                    //String fn = guideBase + "Mask.v3dpbd";
                    //String fn = guideBase + "../maskIndex.v3dpbd";

                    // White box as of 11/7/2014.
                    //String fn = "/Users/fosterl/Whitebackground_color.tif";
                    // Checked 11/7/2014.
                    //String fn = "/Users/fosterl/Erasmus_Color.tiff";
                    // Checked 11/7/2014.
                    //String fn = "/Users/fosterl/Regression_12032013a_color_blocks.tiff";
                    // Checked 11/7/2014.
                    //String fn = "/Users/fosterl/Documents/tiff_mousebrain/00297-ngc.0.tif";
                    // Checked 11/7/2014.
                    //String fn = "/Users/fosterl/Documents/tiff_mousebrain/00138-ngc.0.tif";
                    // All assumed OK 11/7/2014.
                    //String fn = "/Users/fosterl/Documents/tiff_mousebrain/01494-ngc.0.tif";
                    //String fn = "/Users/fosterl/Documents/tiff_mousebrain/00507-ngc.1.tif";
                    //String fn = "/Users/fosterl/Documents/tiff_mousebrain/00507-ngc.0.tif";

                    // Black as of 11/7/2014.
                    //String fn = "/Volumes/nobackup/jacs/jacsData/filestore/system/Separation/296/418/1778029752666296418/separate/ConsolidatedLabel.v3dpbd";
                    // Checked 11/7/2014.
                    //String fn = "/Volumes/nobackup/jacs/jacsData/filestore/system/Separation/143/266/1696292257579143266/separate/ConsolidatedLabel.v3dpbd";
                    // Checked 11/7/2014.
                    //String fn = "/Volumes/nobackup/jacs/jacsData/filestore/simpsonlab/Separation/424/805/2018719198523424805/separate/fastLoad/ConsolidatedSignal2_25.mp4";

                    // White vertical stripes as of 11/7/2014.
                    //String fn = "/Users/fosterl/Documents/alignment_board/lsm/FLCO_20111215135504421_540.lsm";
                    
                    // All black.  String fn = "/Volumes/jacsData/filestore/MaskResources/Compartment/maskRGB.v3dpbd";
                    if ( args.length > 0 ) {
                        fn = args[ 0 ];
                    }

                    VolumeBrickFactory factory = new VolumeBrickFactory() {
                        @Override
                        public VolumeBrickI getVolumeBrick(VolumeModel model) {
                            return new RGBExcludableVolumeBrick( model );
                        }

                        @Override
                        public VolumeBrickI getVolumeBrick(VolumeModel model, TextureDataI maskTextureData, TextureDataI colorMapTextureData) {
                            return null;
                        }
                    };

                    VolumeBrickActorBuilder actorBuilder = new VolumeBrickActorBuilder();
                    GLActor actor = actorBuilder.buildVolumeBrickActor(
                            mipWidget.getVolumeModel(), factory, new TrivialFileResolver(), fn
                    );

                    if ( actor == null )
                        System.out.println("Volume load failed.");
                    else
                        mipWidget.addActor(actor);

                	// mipWidget.loadVolume("/Users/brunsc/projects/fast_load/test_dir2/fastLoad/ConsolidatedSignal2_25.v3dpbd", new TrivialFileResolver());
                	// mipWidget.loadVolume("/Users/brunsc/projects/fast_load/test_dir2/fastLoad/ConsolidatedSignal2_25.v3draw");
                	// mipWidget.loadVolume("/Users/brunsc/projects/fast_load/test_dir/fastLoad/ConsolidatedSignal2_25.mp4");
                	// mipWidget.loadVolume("/Users/brunsc/projects/lsm_compression/GMR_18A04_AE_01_05_cmp.lsm");
                }
                catch (Exception exc) {
                	exc.printStackTrace();
                }
                frame.getContentPane().add(mipWidget);

                //Display the window.
                frame.pack();
                frame.setSize( frame.getContentPane().getPreferredSize() );
                frame.setVisible(true);
            }
        });
	}

}
