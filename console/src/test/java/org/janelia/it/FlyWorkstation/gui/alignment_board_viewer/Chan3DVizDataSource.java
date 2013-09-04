package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableDataSourceI;

import java.util.ArrayList;
import java.util.Collection;

/** Implements the data source, to avoid having to mock up an entire context just to test. */
public class Chan3DVizDataSource implements RenderableDataSourceI {

    private static final String MASK_EXTENSION = ".mask";
    private String[] filenames;
    public Chan3DVizDataSource(String[] filenames) {
        this.filenames = filenames;
    }

    public Chan3DVizDataSource() {
        filenames = new String[] {
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_1.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_1.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_2.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_2.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_3.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_3.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_4.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_4.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_5.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_5.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_6.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_6.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_7.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_7.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_8.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_8.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_9.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_9.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_10.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_10.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_11.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_11.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_12.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_12.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_13.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_13.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_14.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_14.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_15.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_15.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_16.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_16.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_17.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_17.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_18.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_18.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_19.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_19.chan",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_20.mask",
                "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_20.chan",
        };
    }

    @Override
    public String getName() {
        return Chan3DVizDataSource.class.getSimpleName() + " Data Source";
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
        for ( int i = 0; i < filenames.length; i ++ ) {
            if ( ! filenames[ i ].toLowerCase().contains( "/compartment_" ) ) {
                break;
            }
            String fn = filenames[ i ];
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
        for ( int i = nextArgument; i < filenames.length; i += 2 ) {
            RenderableBean renderableBean = new RenderableBean();

            String fn = filenames[ i ];
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
                data.setMaskPath( filenames[ i ] );
                data.setChannelPath( filenames[ i + 1 ] );
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
