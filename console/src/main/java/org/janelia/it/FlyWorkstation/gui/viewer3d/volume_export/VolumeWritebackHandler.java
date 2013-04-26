package org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.FileExportLoadWorker;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.exporter.TiffExporter;
import org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements.CompletionListener;
import org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements.ControlsListener;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.ABContextDataSource;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/18/13
 * Time: 8:59 AM
 *
 * A handler for interpreting the positions of the sliders and writing back data.
 */
public class VolumeWritebackHandler {
    private RenderMappingI renderMapping;
    private Collection<float[]> cropCoords;
    private Mip3d mip3d;
    private CompletionListener completionListener;

    private Logger logger = LoggerFactory.getLogger( VolumeWritebackHandler.class );

    public VolumeWritebackHandler(
            RenderMappingI renderMapping,
            Collection<float[]> cropCoords,
            CompletionListener completionListener,
            Mip3d mip3d
    ) {

        this.cropCoords = cropCoords;
        this.renderMapping = renderMapping;
        this.completionListener = completionListener;
        this.mip3d = mip3d;
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
                    // Need to take a screen shot of the MIP3D object.
                    // Get a buffered image of the component.
                    java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
                            mip3d.getWidth(), mip3d.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB
                    );
                    Graphics2D graphics = bufferedImage.createGraphics();
                    mip3d.paint(graphics);

                    File writeBackFile = getUserFileChoice();
                    if ( writeBackFile != null ) {
                        // Write the tiff.
                        TiffExporter exporter = new TiffExporter();
                        exporter.export( bufferedImage, writeBackFile );
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
                    SessionMgr.getSessionMgr().handleException( ex );
                };
            };
            mipExportWorker.execute();

        }
        else {
            // Save back volume as three-D tiff.
            Map<Integer,byte[]> renderableIdVsRenderMethod = renderMapping.getMapping();

            ABContextDataSource dataSource = new ABContextDataSource(
                    SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext()
            );

            Collection<MaskChanRenderableData> searchDatas = new ArrayList<MaskChanRenderableData>();
            for ( MaskChanRenderableData data: dataSource.getRenderableDatas() ) {
                byte[] rendition = renderableIdVsRenderMethod.get( data.getBean().getTranslatedNum() );
                if ( rendition != null  &&  rendition[ 3 ] != RenderMappingI.NON_RENDERING ) {
                    searchDatas.add( data );
                }
            }

            FileExportLoadWorker.Callback callback = new ExportCallback();

            FileExportLoadWorker.FileExportParamBean paramBean = new FileExportLoadWorker.FileExportParamBean();
            paramBean.setMethod(method);
            paramBean.setCallback(callback);
            paramBean.setCropCoords( cropCoords );
            paramBean.setRenderableDatas( searchDatas );

            FileExportLoadWorker fileExportLoadWorker = new FileExportLoadWorker( paramBean );
            fileExportLoadWorker.setResolver(new CacheFileResolver());
            fileExportLoadWorker.execute();
        }
    }

    /** Prompt for the user's output file to save, and return it. */
    private File getUserFileChoice() {
        JFileChooser fileChooser = new JFileChooser( "Choose Export File" );
        fileChooser.setDialogTitle( "Save" );
        fileChooser.setToolTipText( "Pick an output location for the exported file." );
        fileChooser.showOpenDialog( null );

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
            SessionMgr.getSessionMgr().handleException( ex );
        }

        @Override
        public void loadVolume(TextureDataI texture) {
            File chosenFile = getUserFileChoice();
            if ( chosenFile != null ) {
                try {
                    TiffExporter exporter = new TiffExporter();
                    exporter.export( texture, chosenFile );
                    exporter.close();

                } catch ( Exception ex ) {
                    ex.printStackTrace();
                    logger.error( "Exception on tif export " + ex.getMessage() );
                    SessionMgr.getSessionMgr().handleException( ex );

                }

                frequencyReport(texture);
            }

        }

        /** This is a simple testing mechanism to sanity-check the contents of the texture being saved. */
        private void frequencyReport( TextureDataI texture ) {
            byte[] textureBytes = texture.getTextureData();

            Map<Byte,Integer> byteValToCount = new HashMap<Byte,Integer>();
            for ( int i = 0; i < textureBytes.length; i ++ ) {
                Integer oldVal = byteValToCount.get( textureBytes[ i ] );
                if ( oldVal == null ) {
                    oldVal = 0;
                }
                byteValToCount.put( textureBytes[i], ++oldVal );

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
}
