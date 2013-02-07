package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.SliceViewer;

public class TestSliceViewer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Test SliceViewer");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                JLabel label = new JLabel("Test SliceViewer");
                frame.getContentPane().add(label);
                SliceViewer sliceViewer = new SliceViewer();
                frame.getContentPane().add(sliceViewer);

                //Display the window.
                frame.pack();
                frame.setSize( frame.getContentPane().getPreferredSize() );
                frame.setVisible(true);
            }
        });
	}
}
