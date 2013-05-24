
package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ConfigurableColorMapping;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    private static final long MOCK_UID = 777L;

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
                    }
                    mipWidget.clear();
                    mipWidget.refresh();

                    //String mf = fileBase + "1778036012035866722/ConsolidatedLabel.v3dpbd";
                    //String mf = fileBase + "1696292257579143266/ConsolidatedLabel.v3dpbd";
                    //String volumeFile2 = fileBase + ""; //Unknown as yet.  Doing without...
                    String guideBase = "/Volumes/jacsData/MaskResources/Compartment/guide/";
                    String separationBase = "/Volumes/jacsData/filestore/system/Separation/";

                    File fnFile = new File( fn );
                    if (! fnFile.canRead() ) {
                        throw new IllegalArgumentException( "Cannot open signal file " + fn );
                    }

                    Map<Integer,byte[]> finalMapping = null;
                    VolumeMaskBuilder vmb = null;
                    ConfigurableColorMapping colorMapping = null;

                    // Load the file into the mask-builder.
                    if ( mf != null ) {
                        File mfFile = new File( mf );
                        if ( !mfFile.canRead() ) {
                            throw new IllegalArgumentException( "Cannot open mask file " + mf );
                        }

                        java.util.List<RenderableBean> beans = new ArrayList<RenderableBean>();
                        vmb = new VolumeMaskBuilder();
                        VolumeLoader vLoader = new VolumeLoader( resolver );

                        RenderableBean signalBean = new RenderableBean();
                        signalBean.setLabelFileNum(0);
                        signalBean.setTranslatedNum(0);
                        signalBean.setRgb(
                                new byte[]{
                                        (byte) 0f, (byte) 0f, (byte) 0f, RenderMappingI.NON_RENDERING
                                }
                        );
                        beans.add( signalBean );

                        /*
                        RenderableBean bean = new RenderableBean();
                        bean.setLabelFile( finalLabelFile );
                        bean.setLabelUid( labelEntity.getId() );
                        bean.setSignalFile( signalFilename );
                        bean.setRenderableEntity(fragment);
                        bean.setTranslatedNum( fragmentOffset++ );

                         */

                        Entity mockE = new Entity();
                        mockE.setId( MOCK_UID );
                        EntityType mockEType = new EntityType();
                        mockEType.setName( EntityConstants.TYPE_NEURON_FRAGMENT );
                        mockE.setEntityType( mockEType );
                        mockE.setName("Entity: " + MOCK_UID);

                        RenderableBean renderableBean = new RenderableBean();
                        renderableBean.setTranslatedNum(1);
                        renderableBean.setLabelFileNum( 1 ); // In signal=label=reference scenario, this shows edgy outline
                        renderableBean.setRgb(
                                new byte[] {
                                        (byte)255f, (byte)0f, (byte)255f, RenderMappingI.FRAGMENT_RENDERING
                                }
                        );
                        beans.add(renderableBean);

                        renderableBean = new RenderableBean();
                        renderableBean.setTranslatedNum(2);
                        renderableBean.setLabelFileNum(1);
                        renderableBean.setRenderableEntity( mockE );
                        beans.add(renderableBean);

                        renderableBean = new RenderableBean();
                        renderableBean.setTranslatedNum(3);
                        renderableBean.setLabelFileNum(2);
                        renderableBean.setRenderableEntity( mockE );
                        beans.add(renderableBean);

                        renderableBean = new RenderableBean();
                        renderableBean.setTranslatedNum(4);
                        renderableBean.setLabelFileNum(3);
                        renderableBean.setRenderableEntity( mockE );
                        beans.add(renderableBean);

                        renderableBean = new RenderableBean();
                        renderableBean.setTranslatedNum(6);
                        renderableBean.setLabelFileNum(5);
                        renderableBean.setRenderableEntity( mockE );
                        beans.add(renderableBean);

                        renderableBean = new RenderableBean();
                        renderableBean.setTranslatedNum(8);
                        renderableBean.setLabelFileNum(7);
                        renderableBean.setRenderableEntity( mockE );
                        beans.add(renderableBean);

                        for ( int i = 8; i < 21; i++ ) {
                            renderableBean = new RenderableBean();
                            renderableBean.setTranslatedNum(i);
                            renderableBean.setLabelFileNum(i+1);
                            renderableBean.setRenderableEntity( mockE );
                            beans.add(renderableBean);
                        }
//                        /**
//                         * special renderables for the ref channel.
//                         */
//                        for ( int i = 1; i < 128; i++ ) {
//                            renderableBean = new RenderableBean();
//                            renderableBean.setTranslatedNum(1 + i);
//                            renderableBean.setLabelFileNum(i);
//                            renderableBean.setRenderableEntity( mockE );
//                            beans.add(renderableBean);
//                        }

                        vmb.setRenderables(beans);

                        vLoader.loadVolume( mf );
                        vLoader.populateVolumeAcceptor( vmb );

                        // Setup a testing color-wheel mapping.
                        colorMapping = new ConfigurableColorMapping();
                        if ( args.length > 2 ) {
                            // This mock-entity needs ONLY its ID, a type, and a name.
                            renderableBean.setRenderableEntity(mockE);

                            // Here, establish a map suitable for testing.  It will get a UID-vs-render-method.
                            // This affects only the last-entered file.
                            Map<Long,Integer> guidToRenderMethod = new HashMap<Long,Integer>();
                            guidToRenderMethod.put( MOCK_UID, Integer.parseInt( args[ 2 ] ) );

                            colorMapping.setGuidToRenderMethod( guidToRenderMethod );
                        }
                        colorMapping.setRenderables( beans );
                    }

                    if ( ! mipWidget.loadVolume( fn, vmb, resolver, colorMapping, null, 1.0f ) ) {
                        throw new RuntimeException( "Failed to load " + fn );
                    }

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
