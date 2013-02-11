/**
 * 
 */

package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.FragmentBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ColorMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ColorWheelColorMapping;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.MaskTextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This test-run class / standalone program will pull in a signal and an optional mask file and display the
 * results.
 *
 * Here are some various command line parameters:
 *
 * /Volumes/jacsData/filestore/system/Separation/294/370/1742138165818294370/separate/fastLoad/ConsolidatedSignal2_25.mp4.
 * /Volumes/jacsData/filestore/system/Separation/294/370/1742138165818294370/separate/ConsolidatedLabel.v3dpbd
 *
 * [same as above, on fosterl local disk]
 * /Users/fosterl/Documents/alignment_board/samples/174213816581829437/ConsolidatedSignal2_25.mp4
 * /Users/fosterl/Documents/alignment_board/samples/174213816581829437/ConsolidatedLabel.v3dpbd
 *
 * [single file]
 * /Volumes/jacsData/filestore/system/Separation/296/418/1778029752666296418/separate/ConsolidatedLabel.v3dpbd
 *
 * [Pattern Guide]    -- broken at the moment.
 * /Volumes/jacsData/filestore/system/Separation/974/754/1757362152491974754/separate/fastLoad/ConsolidatedSignal2_25.mp4
 * /Volumes/jacsData/MaskResources/Compartment/guide/LOP_R.v3dpbd
 *   -- these are alternates to LOP_R.
 * WED_L.v3dpbd
 * WED_R.v3dpbd
 *
 * /Users/fosterl/Documents/alignment_board/samples/1735579170638921826/ConsolidatedSignal2_25.mp4
 * /Users/fosterl/Documents/alignment_board/samples/1735579170638921826/ConsolidatedLabel.v3dpbd
 *
 * [This is ONLY the mask.  Use it with signal of similar path]
 * /Volumes/jacsData/filestore/system/Separation/974/754/1757362152491974754/separate/ConsolidatedLabel.v3dpbd
 * @author brunsc
 */
public class TestMaskedMip3d {

	/**
     * This is a test program for trying the full volume + mask functionality of the Mip3d widget.  This test
     * has the convenience of being able to use hardcoded (local) paths, and quicker cycle time than the
     * full-blown console application
     *
	 * @param args no arguments used at this time.
	 */
	public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Test MipWidget for Masking");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                JLabel label = new JLabel("Test MipWidget for Masking");
                frame.getContentPane().add(label);
                frame.setSize( new Dimension( 600, 622 ) );
                Mip3d mipWidget = new Mip3d();
                FileResolver resolver = new TrivialFileResolver();

                try {
                    /*
                       This combination causes the yellow-box problem.

                    ./1735579170638921826/ConsolidatedSignal2_25.mp4
                    ./1778036012035866722/ConsolidatedLabel.v3dpbd
                     */

                    String baseDir = "/Users/fosterl/Documents/alignment_board/samples/174213816581829437/";
                    String fn = baseDir + "ConsolidatedSignal2_25.mp4";
                    String mf = baseDir + "ConsolidatedLabel.v3dpbd";
                    if ( args.length > 0 ) {
                        fn = args[ 0 ];
                        mf = null;
                    }
                    if ( args.length > 1 ) {
                        mf = args[ 1 ];
                        mipWidget.setClearOnLoad( true );
                        mipWidget.refresh();
                    }

                    //String mf = fileBase + "1778036012035866722/ConsolidatedLabel.v3dpbd";
                    //String mf = fileBase + "1696292257579143266/ConsolidatedLabel.v3dpbd";
                    //String volumeFile2 = fileBase + ""; //Unknown as yet.  Doing without...
                    String guideBase = "/Volumes/jacsData/MaskResources/Compartment/guide/";
                    String separationBase = "/Volumes/jacsData/filestore/system/Separation/";

                    File fnFile = new File( fn );
                    if (! fnFile.canRead() ) {
                        throw new IllegalArgumentException( "Cannot open signal file " + fn );
                    }

                    VolumeMaskBuilder vmb = null;
                    // Load the file into the mask-builder.
                    if ( mf != null ) {
                        File mfFile = new File( mf );
                        if ( !mfFile.canRead() ) {
                            throw new IllegalArgumentException( "Cannot open mask file " + mf );
                        }

                        java.util.List<FragmentBean> beans = new ArrayList<FragmentBean>();
                        vmb = new VolumeMaskBuilder();
                        VolumeLoader vLoader = new VolumeLoader( resolver );

                        FragmentBean fragmentBean = new FragmentBean();
                        fragmentBean.setLabelFile( mf );
                        fragmentBean.setTranslatedNum(1);
                        fragmentBean.setLabelFileNum( 13 ); // Can modify this.
                        beans.add( fragmentBean );

                        fragmentBean = new FragmentBean();
                        fragmentBean.setLabelFile( mf );
                        fragmentBean.setTranslatedNum(2);
                        fragmentBean.setLabelFileNum( 14 );
                        beans.add( fragmentBean );

                        fragmentBean = new FragmentBean();
                        fragmentBean.setLabelFile( mf );
                        fragmentBean.setTranslatedNum(3);
                        fragmentBean.setLabelFileNum( 15 );
                        beans.add( fragmentBean );

                        vmb.setFragments( beans );

                        vLoader.loadVolume( mf );
                        vLoader.populateVolumeAcceptor( vmb );

                        // Setup a testing color-wheel mapping.
                        ColorMappingI colorMapper = new ColorWheelColorMapping();
                        mipWidget.setMaskColorMappings( colorMapper.getMapping( beans ) );
                    }

                    if ( ! mipWidget.loadVolume( fn, vmb, resolver ) ) {
                        throw new RuntimeException( "Failed to load " + fn );
                    }
                    mipWidget.setClearOnLoad( false );

                    frame.getContentPane().add(mipWidget);

                    //Display the window.
                    frame.pack();
                    frame.setSize( frame.getContentPane().getPreferredSize() );
                    frame.setVisible(true);
                }
                catch (Exception exc) {
                	exc.printStackTrace();
                }
            }
        });
	}

}
