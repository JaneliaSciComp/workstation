package org.janelia.it.workstation.gui.alignment_board_viewer.volume_export;

import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFileChooser;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.alignment_board.util.ABItem;
import org.janelia.it.workstation.gui.alignment_board_viewer.FileExportLoadWorker;
import org.janelia.it.workstation.gui.alignment_board_viewer.creation.DomainHelper;
import org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements.AlignmentBoardControlsDialog;
import org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements.CompletionListener;
import org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements.ControlsListener;
import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.workstation.gui.alignment_board_viewer.texture.ABContextDataSource;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.workstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.volume_builder.VolumeDataChunk;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/18/13
 * Time: 8:59 AM
 *
 * A handler for interpreting the positions of the sliders and writing back data.
 */
public class VolumeWritebackHandler {
    private final RenderMappingI renderMapping;
    private final Collection<float[]> cropCoords;
    private final Mip3d mip3d;
    private final CompletionListener completionListener;
    private int filterSize;
    private int maxNeurons;
    private double gammaFactor;
    private byte[] backgroundColorArr;
    private File writeBackFile;
    private DomainHelper domainHelper;

    private final Logger logger = LoggerFactory.getLogger( VolumeWritebackHandler.class );

    public VolumeWritebackHandler(
            RenderMappingI renderMapping,
            Collection<float[]> cropCoords,
            CompletionListener completionListener,
            Mip3d mip3d,
            double gammaFactor,
            int filterSize,
            int maxNeurons
    ) {
        this.domainHelper = new DomainHelper();
        this.cropCoords = cropCoords;
        this.renderMapping = renderMapping;
        this.completionListener = completionListener;
        this.mip3d = mip3d;
        this.filterSize = filterSize;
        this.maxNeurons = maxNeurons;
        this.gammaFactor = gammaFactor;
        this.backgroundColorArr = colorToByte( mip3d.getVolumeModel().getBackgroundColorFArr() );
    }

    /**
     * This control-callback writes the user's selected volume to a file on disk.
     *
     * @param method how to write the file.
     */
    public void writeBackVolumeSelection( ControlsListener.ExportMethod method ) {
        if ( method == ControlsListener.ExportMethod.mip ) {

            SimpleWorker mipExportWorker = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    writeBackFile = getUserFileChoice();
                    if ( writeBackFile != null ) {
                        // Need to take a screen shot of the MIP3D object.
                        // Get a buffered image of the component.
                        BufferedImage bufferedImage = new BufferedImage(
                                mip3d.getWidth(), mip3d.getHeight(), BufferedImage.TYPE_INT_RGB
                        );
                        Graphics2D graphics = bufferedImage.createGraphics();
                        mip3d.paint(graphics);

                        // Write the tiff.
                        TiffExporter exporter = new TiffExporter();
                        exporter.export( bufferedImage, writeBackFile );

                        writeMetaFile(writeBackFile);
                    }
                }

                @Override
                protected void hadSuccess() {
                    completionListener.complete();
                }

                @Override
                protected void hadError(Throwable ex) {
                    completionListener.complete();

                    ex.printStackTrace();
                    logger.error( ex.getMessage() );
                    FrameworkImplProvider.handleException( ex );
                }
            };
            mipExportWorker.execute();

        }
        else {
            writeBackFile = getUserFileChoice();

            // Save back volume as three-D tiff.
            Map<Integer,byte[]> renderableIdVsRenderMethod = renderMapping.getMapping();

            ABContextDataSource dataSource = new ABContextDataSource(
                    AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext()
            );

            Collection<MaskChanRenderableData> searchDatas = new ArrayList<>();
            for ( MaskChanRenderableData data: dataSource.getRenderableDatas() ) {
                byte[] rendition = renderableIdVsRenderMethod.get( data.getBean().getTranslatedNum() );
                if ( rendition != null  &&  rendition[ 3 ] != RenderMappingI.NON_RENDERING ) {
                    searchDatas.add( data );
                    // Convert the rendition to the latest selected by the user.
                    data.getBean().setRgb( rendition );
                }
            }

            FileExportLoadWorker.Callback callback = new ExportCallback();

            FileExportLoadWorker.FileExportParamBean paramBean = new FileExportLoadWorker.FileExportParamBean();
            paramBean.setMethod(method);
            paramBean.setCallback(callback);
            paramBean.setCropCoords(cropCoords);
            paramBean.setRenderableDatas(searchDatas);
            paramBean.setFilterSize(filterSize);
            paramBean.setMaxNeuronCount(maxNeurons);
            paramBean.setGammaFactor(gammaFactor);
            paramBean.setBackgroundColorArr(backgroundColorArr);

            FileExportLoadWorker fileExportLoadWorker = new FileExportLoadWorker( paramBean );
            fileExportLoadWorker.setResolver(new CacheFileResolver());
            fileExportLoadWorker.execute();
        }
    }

    private void writeMetaFile(File writeBackFile) throws IOException {
        File metaFile = new File( writeBackFile.getParent(), writeBackFile.getName() + ".metadata.txt");
        PrintWriter pw = new PrintWriter( new FileWriter( metaFile ) );

        AlignmentBoardContext abContext = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
        List<AlignmentBoardItem> children = abContext.getAlignmentBoard().getChildren();
        for ( AlignmentBoardItem nextChild: children ) {
            ABItem dobj = domainHelper.getObjectForItem(nextChild);
            pw.println("Container Item: NAME=" + dobj.getName() + ", ID=" + dobj.getId());
            for ( AlignmentBoardItem grandChild: nextChild.getChildren() ) {
                ABItem gcDObj = domainHelper.getObjectForItem(grandChild);
                pw.println("Neuron: ID=" + gcDObj.getId() + " NAME=" + gcDObj.getName() + " TYPE=" + gcDObj.getType() + " OWNER=" + gcDObj.getOwnerKey());
            }
        }

        pw.close();
    }

    /** Prompt for the user's output file to save, and return it. */
    private File getUserFileChoice() {
        JFileChooser fileChooser = new JFileChooser( "Choose Export File" );
        fileChooser.setDialogTitle( "Save" );
        fileChooser.setToolTipText("Pick an output location for the exported file.");
        JDialog backingComponent = null;
        for ( Window dialog: JDialog.getWindows() ) {
            if (AlignmentBoardControlsDialog.CONTAINING_DIALOG_NAME.equals(dialog.getName()) ) {
                backingComponent = (JDialog)dialog;
            }
        }
        fileChooser.showDialog( backingComponent, "Save" );

        // Get the file.
        return fileChooser.getSelectedFile();
    }

    /**
     * This class has responsibility for exporting the collected texture.
     */
    private class ExportCallback implements FileExportLoadWorker.Callback {
        @Override
        public void loadSucceeded() {
            completionListener.complete();
        }

        @Override
        public void loadFailed(Throwable ex) {
            completionListener.complete();
            ex.printStackTrace();
            FrameworkImplProvider.handleException( ex );
        }

        @Override
        public void loadVolume(TextureDataI texture) {
            if ( writeBackFile != null ) {
                try {
                    TiffExporter exporter = new TiffExporter();
                    exporter.export( texture, writeBackFile );
                    exporter.close();

                    writeMetaFile( writeBackFile );

                } catch ( Exception ex ) {
                    ex.printStackTrace();
                    logger.error( "Exception on tif export " + ex.getMessage() );
                    FrameworkImplProvider.handleException( ex );

                }

                //frequencyReport(texture);
            }

        }

        /** This is a simple testing mechanism to sanity-check the contents of the texture being saved. */
        @SuppressWarnings("unused")
        private void frequencyReport( TextureDataI texture ) {
            Map<Byte,Integer> byteValToCount = new HashMap<>();
            for ( VolumeDataChunk chunk: texture.getTextureData().getVolumeChunks() ) {
                byte[] textureBytes = chunk.getData();

                for (byte textureByte : textureBytes) {
                    Integer oldVal = byteValToCount.get(textureByte);
                    if (oldVal == null) {
                        oldVal = 0;
                    }
                    byteValToCount.put(textureByte, ++oldVal);

                }
            }

            StringBuilder bldr = new StringBuilder( "---------------------" );
            bldr.append( System.getProperty( "line.separator" ) );
            for ( Byte b: byteValToCount.keySet() ) {
                bldr.append( String.format( "Value %d appears %d times.", b, byteValToCount.get( b ) ) );
                bldr.append( System.getProperty("line.separator") );
            }
            logger.info( bldr.toString() );

        }
    }
    
    private byte[] colorToByte( float[] colorFArr ) {
        byte[] rtnVal = null;
        if (colorFArr != null) {
            int zeroCount = 0;
            rtnVal = new byte[ VolumeModel.COLOR_MASK_ARR_SIZE ];
            for (int i = 0; i < VolumeModel.COLOR_MASK_ARR_SIZE; i++) {
                // Get around the vagaries of floating point.
                if (colorFArr[ i ] < 0.0001) {
                    rtnVal[ i ] = 0;
                    zeroCount++;
                } else {
                    rtnVal[ i ] = (byte)(255.0f * colorFArr[ i ]);
                }
            }
            if (zeroCount == VolumeModel.COLOR_MASK_ARR_SIZE) {
                rtnVal = null;
            }
        }
        
        return rtnVal;
    }
}
