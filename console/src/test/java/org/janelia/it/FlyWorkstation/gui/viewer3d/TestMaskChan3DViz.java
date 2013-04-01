
package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.RenderablesLoadWorker;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.*;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableDataSourceI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.TrivialFileResolver;

import javax.swing.*;
import java.awt.Dimension;
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
                    RenderablesLoadWorker loadWorker = new RenderablesLoadWorker(
                            (JComponent)frame.getContentPane(), new Chan3DVizDataSource( args ), mipWidget
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

    /** Implements the data source, to avoid having to mock up an entire context just to test. */
    public static class Chan3DVizDataSource implements RenderableDataSourceI {

        private String[] args;
        public Chan3DVizDataSource( String[] args ) {
            this.args = args;
        }

        @Override
        public String getName() {
            return TestMaskChan3DViz.class.getSimpleName() + " Data Source";
        }

        @Override
        public Collection<MaskChanRenderableData> getRenderableDatas() {

            Collection<MaskChanRenderableData> rtnVal = new ArrayList<MaskChanRenderableData>();

            // Establish the renderable for the "signal".
            int nextTranslatedNum = 0;
            RenderableBean signalBean = new RenderableBean();
            signalBean.setLabelFileNum(0);
            signalBean.setTranslatedNum(nextTranslatedNum ++);
            signalBean.setRgb(
                    new byte[]{
                            (byte) 0f, (byte) 0f, (byte) 0f, RenderMappingI.NON_RENDERING
                    }
            );

            MaskChanRenderableData signalData = new MaskChanRenderableData();
            signalData.setBean(signalBean);
            signalData.setCompartment( false );

            // Must divie-up the inputs between compartments and Fragments.

            // Establish the compartment renderables.
            int nextArgument = 0;
            for ( int i = 0; i < args.length; i ++ ) {
                if ( ! args[ i ].toLowerCase().contains( "/compartment_" ) ) {
                    break;
                }
                String fn = args[ i ];
                if ( fn.endsWith( MASK_EXTENSION ) ) {
                    RenderableBean renderableBean = new RenderableBean();

                    // File name should tell its own index number.
                    int underPos = fn.lastIndexOf( '_' );
                    String beanNumStr = fn.substring( underPos + 1, fn.indexOf( MASK_EXTENSION ) );
                    int beanNum = Integer.parseInt( beanNumStr );
                    renderableBean.setLabelFileNum(beanNum);
                    renderableBean.setTranslatedNum(nextTranslatedNum);
                    renderableBean.setRgb(
                            new byte[]{0, 0, 0, RenderMappingI.COMPARTMENT_RENDERING}
                    );

                    MaskChanRenderableData data = new MaskChanRenderableData();
                    data.setBean( renderableBean );
                    data.setCompartment( true );
                    data.setMaskPath( fn );
                    rtnVal.add(data);

                    nextTranslatedNum ++;
                }
                nextArgument ++;
            }

            // Establish the fragment renderables.
            int colorDriver = 20;
            for ( int i = nextArgument; i < args.length; i += 2 ) {
                RenderableBean renderableBean = new RenderableBean();

                String fn = args[ i ];
                if ( fn.endsWith( MASK_EXTENSION ) ) {
                    // File name should tell its own index number.
                    int underPos = fn.lastIndexOf( '_' );
                    String beanNumStr = fn.substring( underPos + 1, fn.indexOf( MASK_EXTENSION ) );
                    int beanNum = Integer.parseInt( beanNumStr );

                    renderableBean.setTranslatedNum( nextTranslatedNum );
                    renderableBean.setLabelFileNum( beanNum );
                    int rComp = colorDriver % 5;
                    int gComp = colorDriver % 7;
                    int bComp = colorDriver % 11;
                    renderableBean.setRgb(
                            new byte[]{
                                    (byte) ((rComp * 31) % 255), (byte) ((gComp * 23) % 255), (byte) ((bComp * 19) % 255),
                                    RenderMappingI.FRAGMENT_RENDERING
                            }
                    );

                    MaskChanRenderableData data = new MaskChanRenderableData();
                    data.setBean( renderableBean );
                    data.setCompartment( false );
                    data.setMaskPath( args[ i ] );
                    data.setChannelPath( args[ i + 1 ] );
                    rtnVal.add(data);

                    nextTranslatedNum ++;
                    colorDriver ++;
                }
                else {
                    throw new IllegalArgumentException(
                            "Wrong file order.  Expecting .mask at position " + i + " in command-line arguments."
                    );
                }
            }

            return rtnVal;
        }
    }
}
