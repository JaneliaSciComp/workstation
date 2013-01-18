/**
 * 
 */

package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.TrivialFileResolver;

import javax.swing.*;
import java.util.Arrays;

/**
 * @author brunsc
 *
 */
public class TestMaskedMip3d {

	/**
     * This is a test program for trying the full volume + mask functionality of the Mip3d widget.  This test
     * has the convenience of being able to use hardcoded (local) paths, and quicker cycle time than the
     * full-blown console application
     *
	 * @param args no arguments used at this time.
	 */
	public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Test MipWidget for Masking");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                JLabel label = new JLabel("Test MipWidget for Masking");
                frame.getContentPane().add(label);
                Mip3d mipWidget = new Mip3d();
                mipWidget.setClearOnLoad( true );
                mipWidget.refresh();
                FileResolver resolver = new TrivialFileResolver();
                try {
                    /*
                    ./1696292257579143266/ConsolidatedLabel.v3dpbd
                    ./1735579170638921826/ConsolidatedSignal2_25.mp4
                    ./1778036012035866722/ConsolidatedLabel.v3dpbd
                     */
                    //String fn = "/Volumes/jacsData/filestore/system/Separation/296/418/1778029752666296418/separate/ConsolidatedLabel.v3dpbd";
                    String fileBase = "/Users/fosterl/Documents/alignment_board/samples/";
                    String volumeFile1 = fileBase + "1735579170638921826/ConsolidatedSignal2_25.mp4";
                    //String volumeFile2 = fileBase + ""; //Unknown as yet.  Doing without...
                    String[] maskFiles = {
                            fileBase + "1735579170638921826/ConsolidatedLabel.v3dpbd", // This matches the signal file.
                            fileBase + "1696292257579143266/ConsolidatedLabel.v3dpbd",
                            fileBase + "1778036012035866722/ConsolidatedLabel.v3dpbd",
                    };
                    //  Bypass mask files.  May be misloading over other texture.
                    mipWidget.setMaskFiles( Arrays.asList( maskFiles ), resolver );
                    if ( ! mipWidget.loadVolume(volumeFile1, resolver) )  {
                        System.out.println("Volume load failed.");
                    }
                    mipWidget.setClearOnLoad( false );
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
