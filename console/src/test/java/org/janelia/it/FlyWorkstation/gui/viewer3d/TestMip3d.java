/**
 * 
 */

package org.janelia.it.FlyWorkstation.gui.viewer3d;

import javax.swing.*;

/**
 * @author brunsc
 *
 */
public class TestMip3d {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Test MipWidget");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                JLabel label = new JLabel("Test MipWidget");
                frame.getContentPane().add(label);
                Mip3d mipWidget = new Mip3d();
                try {
                	// mipWidget.loadVolume("/Users/brunsc/smallRefTest.tif");

                    // This load-up works perfectly.
                    //      String fn = "/Volumes/jacsData/brunsTest/3d_test_images/ConsolidatedSignal2_25.v3dpbd";
                    //      String fn = "/Volumes/flylight/LES Foster/B1_T1_20120427_1CC_38C04wtFlp_2D_R1_L02.lsm";
                    //      String fn = "/Volumes/flylight/flip/confocalStacks/20120929/FLFL_20121001154228067_25581.lsm";
                    //      String fn = "/Users/fosterl/Documents/LSM_3D/FLFL_20121001154228067_25581.lsm";
                    //String fn = "/Volumes/flylight/RJ/For Les Foster/B01_T01_20121023_PMB1_MB057B_20X_R1_L01.lsm";
                    //                    String fn = "/Volumes/jacsData/MaskResources/GiantFiber/guide/EPA_R.v3dpbd";
                    String fn = "/Volumes/jacsData/MaskResources/GiantFiber/guide/LAL_L.v3dpbd";
                    if ( ! mipWidget.loadVolume(fn) )
                        System.out.println("Volume load failed.");
                	// mipWidget.loadVolume("/Users/brunsc/projects/fast_load/test_dir2/fastLoad/ConsolidatedSignal2_25.v3dpbd");
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
