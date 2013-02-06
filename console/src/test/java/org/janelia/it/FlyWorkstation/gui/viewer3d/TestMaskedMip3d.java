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
import java.util.ArrayList;
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
                frame.setSize( new Dimension( 600, 622 ) );
                Mip3d mipWidget = new Mip3d();
                mipWidget.setClearOnLoad( true );
                mipWidget.refresh();
                FileResolver resolver = new TrivialFileResolver();

                /*
                /Volumes/jacsData/filestore/system/Separation/294/370/1742138165818294370/separate/fastLoad/ConsolidatedSignal2_25.mp4.
                /Volumes/jacsData/filestore/system/Separation/294/370/1742138165818294370/separate/ConsolidatedLabel.v3dpbd
                 */
                try {
                    /*
                       This combination causes the yellow-box problem.

                    ./1735579170638921826/ConsolidatedSignal2_25.mp4
                    ./1778036012035866722/ConsolidatedLabel.v3dpbd
                     */
                    //String fn = "/Volumes/jacsData/filestore/system/Separation/296/418/1778029752666296418/separate/ConsolidatedLabel.v3dpbd";
                    //String fileBase = "/Users/fosterl/Documents/alignment_board/samples/";
                    //String fn = fileBase + "1735579170638921826/ConsolidatedSignal2_25.mp4";
                    //String mf = fileBase + "1735579170638921826/ConsolidatedLabel.v3dpbd";

                    //String fn = "/Volumes/jacsData/filestore/system/Separation/294/370/1742138165818294370/separate/fastLoad/ConsolidatedSignal2_25.mp4";
                    //String mf = "/Volumes/jacsData/filestore/system/Separation/294/370/1742138165818294370/separate/ConsolidatedLabel.v3dpbd";

                    String baseDir = "/Users/fosterl/Documents/alignment_board/samples/174213816581829437/";
                    String fn = baseDir + "ConsolidatedSignal2_25.mp4";
                    String mf = baseDir + "ConsolidatedLabel.v3dpbd";

                    //String mf = fileBase + "1778036012035866722/ConsolidatedLabel.v3dpbd";
                    //String mf = fileBase + "1696292257579143266/ConsolidatedLabel.v3dpbd";
                    //String volumeFile2 = fileBase + ""; //Unknown as yet.  Doing without...
                    String guideBase = "/Volumes/jacsData/MaskResources/Compartment/guide/";
                    String separationBase = "/Volumes/jacsData/filestore/system/Separation/";
//                    String fn = "/Volumes/jacsData/filestore/system/Separation/974/754/1757362152491974754/separate/fastLoad/ConsolidatedSignal2_25.mp4";
//                    String[] maskFiles = {
//                            guideBase + "LOP_R.v3dpbd",
//                            guideBase + "WED_L.v3dpbd",
//                            guideBase + "WED_R.v3dpbd",
//                    };

//                    String mf = "/Volumes/jacsData/filestore/system/Separation/974/754/1757362152491974754/separate/ConsolidatedLabel.v3dpbd";

                    java.util.List<FragmentBean> beans = new ArrayList<FragmentBean>();

                    // Load the file into the mask-builder.
                    VolumeMaskBuilder vmb = new VolumeMaskBuilder();
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
                    mipWidget.setVolumeMaskBuilder( vmb );

//                    if ( ! mipWidget.loadVolume(volumeFile1, resolver) )  {
//                        System.out.println("Volume load failed.");
//                    }
                    //fn
                    if ( ! mipWidget.loadVolume( fn, resolver ) ) {
                        throw new RuntimeException( "Failed to load " + fn );
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
