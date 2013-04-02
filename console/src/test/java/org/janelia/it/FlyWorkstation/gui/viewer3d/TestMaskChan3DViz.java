
package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.RenderablesLoadWorker;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ConfigurableColorMapping;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.TrivialFileResolver;

import javax.swing.*;
import java.awt.Dimension;

/**
 * This test-run class / standalone program will pull in a mask/channel pair and display the results.
 *
 * @see TestMaskedMip3d
 *
 * @author fosterl
 */
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
                    RenderMappingI renderMapping = new ConfigurableColorMapping();
                    RenderablesLoadWorker loadWorker = new RenderablesLoadWorker(
                            (JComponent)frame.getContentPane(),
                            new Chan3DVizDataSource( args ),
                            mipWidget,
                            renderMapping
                    );
                    loadWorker.setResolver( new TrivialFileResolver() );
                    loadWorker.execute();

                    //Display the window.
                    //args.frame.pack();
                    //frame.setSize( frame.getContentPane().getPreferredSize() );
                    frame.setVisible(true);

                }
                catch (Exception exc) {
                	exc.printStackTrace();
                }
            }
        });
	}

}
