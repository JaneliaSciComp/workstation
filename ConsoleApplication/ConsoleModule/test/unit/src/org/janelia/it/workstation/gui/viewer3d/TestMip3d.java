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
                    //String fn = "/Volumes/jacsData/filestore/MaskResources/GiantFiber/guide/LAL_L.v3dpbd";
                    //String fn = "/Volumes/jacsData/filestore/system/Separation/296/418/1778029752666296418/separate/fastload/Reference2_50.mp4";
                    String fileBase = "/Users/fosterl/Documents/alignment_board/samples/";
                    //String fn = fileBase + "1735579170638921826/ConsolidatedSignal2_25.mp4";
                    //String fn = "/Volumes/jacsData/filestore/system/Separation/951/842/1742124964321951842/separate/ConsolidatedLabel.v3dpbd";
                    //String fn = "/Volumes/jacsData/filestore/system/Separation/294/370/1742138165818294370/separate/fastLoad/ConsolidatedSignal2_25.mp4";

                    // Compartment masks.
                    String guideBase = "/Volumes/jacsData/filestore/MaskResources/Compartment/guide/";
                    //String fn = guideBase + "LOP_R.v3dpbd";
                    //String fn = guideBase + "Mask.v3dpbd";
                    //String fn = guideBase + "../maskIndex.v3dpbd";

                    //String fn = "/Users/fosterl/.JaneliaWorkstationSuite/Console/.jacs-file-cache/active/WebDAV/nobackup/jacs/jacsData/filestore/leetlab/Separation/956/258/1834565641604956258/separate/fastLoad/ConsolidatedSignal2_25.mp4";

                    //String fn = "/Users/fosterl/mean_brain_295_ref_local_with_compartment_edges.tif";
                    //String fn = "/Users/fosterl/test1_256.tif";

                    //String fn = "/Volumes/jacsData/filestore/system/Separation/296/418/1778029752666296418/separate/ConsolidatedLabel.v3dpbd";
                    //String fn = "/Volumes/jacsData/filestore/system/Separation/143/266/1696292257579143266/separate/ConsolidatedLabel.v3dpbd";
                    //String fn = "/Volumes/jacsData/filestore/system/Separation/921/826/1735579170638921826/separate/ConsolidatedSignal.v3dpbd";
                    //String fn = "/Volumes/jacsData/filestore/asoy/Separation/206/690/1823963551044206690/separate/ConsolidatedSignal.v3dpbd";   // 1672 x 1024 x 360
                    //String fn = "/Volumes/jacsData/filestore/asoy/Separation/998/562/1823854828887998562/separate/ConsolidatedSignal.v3dpbd";
                    //String fn = "/Volumes/jacsData/filestore/asoy/Separation/475/490/1823622563356475490/separate/ConsolidatedSignal.v3dpbd";
                    //String fn = "/Volumes/jacsData/filestore/asoy/Separation/309/922/1822667866399309922/separate/ConsolidatedSignal.v3dpbd";
                    //String fn = "/Volumes/mousebrainmicro/render/2013-03-25-Liver/default.0.tif";
                    String fn = "/Volumes/nobackup/jacs/jacsData/filestore/system/Separation/143/266/1696292257579143266/separate/ConsolidatedLabel.v3dpbd";


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
