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
                		mipWidget.loadVolume("/Users/brunsc/projects/fast_load/test_dir2/fastLoad/ConsolidatedSignal2_25.v3draw");
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
