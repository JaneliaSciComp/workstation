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

                //Add the ubiquitous "Hello World" label.
                JLabel label = new JLabel("Test MipWidget");
                frame.getContentPane().add(label);
                Mip3d mipWidget = new Mip3d();
                frame.getContentPane().add(mipWidget);

                //Display the window.
                frame.pack();
                frame.setSize( frame.getContentPane().getPreferredSize() );
                frame.setVisible(true);
            }
        });
	}

}
