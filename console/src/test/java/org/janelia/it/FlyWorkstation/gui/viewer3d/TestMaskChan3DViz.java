
package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanMultiFileLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.*;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.jacs.model.entity.Entity;

import javax.swing.*;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * This test-run class / standalone program will pull in a mask/channel pair and display the results.
 *
 * @see TestMaskedMip3d
 *
 * @author fosterl
 */
public class TestMaskChan3DViz {

    private static final long MOCK_UID = 777L;
    private static final String MASK_EXTENSION = ".mask";

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
                    List<RenderableBean> renderableBeans = new ArrayList<RenderableBean>();

                    /* Establish all renderables to be displayed. */

                    // Establish the renderable for the "signal".
                    RenderableBean signalBean = new RenderableBean();
                    signalBean.setLabelFileNum(0);
                    signalBean.setTranslatedNum(0);
                    signalBean.setRgb(
                            new byte[]{
                                    (byte) 0f, (byte) 0f, (byte) 0f, RenderMappingI.NON_RENDERING
                            }
                    );
                    renderableBeans.add( signalBean );

                    // Establish the fragment renderables.
                    int colorDriver = 20;
                    Entity mockE = new Entity();
                    mockE.setId( MOCK_UID );
                    for ( int i = 0; i < args.length; i += 2 ) {
                        RenderableBean renderableBean = new RenderableBean();

                        String fn = args[ i ];
                        if ( fn.endsWith( MASK_EXTENSION ) ) {
                            // File name should tell its own index number.
                            int underPos = fn.lastIndexOf( '_' );
                            String beanNumStr = fn.substring( underPos + 1, fn.indexOf( MASK_EXTENSION ) );
                            int beanNum = Integer.parseInt( beanNumStr );

                            renderableBean.setTranslatedNum( beanNum );     // Leaving translation original.
                            renderableBean.setLabelFileNum( beanNum );
                            int rComp = colorDriver % 2;
                            int gComp = colorDriver % 7;
                            int bComp = colorDriver % 11;
                            renderableBean.setRgb(
                                    new byte[] {
                                            (byte)((rComp * 31) % 255), (byte)((gComp * 23) % 255), (byte)((bComp * 19) % 255),
                                            RenderMappingI.FRAGMENT_RENDERING
                                    }
                            );
                            //renderableBean.setRenderableEntity( mockE );
                            renderableBeans.add(renderableBean);

                            colorDriver ++;
                        }
                        else {
                            throw new IllegalArgumentException(
                                  "Wrong file order.  Expecting .mask at position " + i + " in command-line arguments."
                            );
                        }
                    }


                    /* Establish all volume builders for this test. */
                    ArrayList<MaskChanDataAcceptorI> acceptors = new ArrayList<MaskChanDataAcceptorI>();

                    // Establish the means for extracting the volume mask.
                    RenderablesMaskBuilder vmb = new RenderablesMaskBuilder();
                    vmb.setRenderables( renderableBeans );

                    // Establish the means for extracting the signal data.
                    RenderablesChannelsBuilder vcb = new RenderablesChannelsBuilder();

                    // Setup the loader to traverse all this data on demand.
                    MaskChanMultiFileLoader loader = new MaskChanMultiFileLoader();
                    acceptors.add( vmb );
                    acceptors.add( vcb );

                    loader.setAcceptors( acceptors );

                    // Iterating through these files will cause all the relevant data to be loaded into
                    // the acceptors, which here are the mask builder and the channels builder.
                    for ( int i = 0; i < args.length; i += 2 ) {
                        InputStream maskStream = new BufferedInputStream( new FileInputStream( args[ i ] ) );
                        InputStream chanStream = new BufferedInputStream( new FileInputStream( args[ i + 1 ] ) );

                        // Only the neuron fragment renderables are relevant to this load.  The signal renderable
                        // is kept separate.  Hence skip the first (0th) renderable.
                        loader.read( renderableBeans.get( (i/2) + 1 ), maskStream, chanStream );

                        maskStream.close();
                        chanStream.close();
                    }

                    // For DEBUG
                    //vcb.test();

                    // Setup a testing color-wheel mapping.
                    ConfigurableColorMapping colorMapping = new ConfigurableColorMapping();
                    colorMapping.setRenderables( renderableBeans );

                    JFrame frame = new JFrame("Test MipWidget for Masking");
                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    JLabel label = new JLabel("Test MipWidget for Masking");
                    frame.getContentPane().add(label);
                    frame.setSize( new Dimension( 600, 622 ) );
                    Mip3d mipWidget = new Mip3d();

                    mipWidget.clear();

                    // Now, the vmb and the vcb are fully populated with all data. Can hand that into the
                    // mip3d.
                    //    WORKS
                    //String fn = "/Volumes/jacsData/filestore/system/Separation/294/370/1742138165818294370/separate/fastLoad/ConsolidatedSignal2_25.mp4";
                    //VolumeLoader volumeLoader = new VolumeLoader(new TrivialFileResolver());
                    //volumeLoader.loadVolume( fn );

                    //mipWidget.loadVolume( fn, new TrivialFileResolver() );

                    if ( ! mipWidget.setVolume( vcb, vmb, colorMapping, 1.0f ) ) {
                        throw new RuntimeException( "Failed to load." );
                    }

                    //mipWidget.loadVolume( fn, vmb, new TrivialFileResolver(), colorMapping, 1.0f );
                    frame.getContentPane().add( mipWidget );

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
