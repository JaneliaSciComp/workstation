package org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.FileExportLoadWorker;
import org.janelia.it.FlyWorkstation.gui.viewer3d.exporter.TiffExporter;
import org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements.CompletionListener;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.ABContextDataSource;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private float[] cropCoords;
    private CompletionListener completionListener;

    private Logger logger = LoggerFactory.getLogger( VolumeWritebackHandler.class );

    public VolumeWritebackHandler(
            RenderMappingI renderMapping,
            float[] cropCoords,
            CompletionListener completionListener
    ) {

        this.cropCoords = cropCoords;
        this.renderMapping = renderMapping;
        this.completionListener = completionListener;

    }

    /** This control-callback writes the user's selected volume to a file on disk. */
    public void writeBackVolumeSelection() {
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

        FileExportLoadWorker.Callback callback = new FileExportLoadWorker.Callback() {
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
                byte[] textureBytes = texture.getTextureData();

                Map<Byte,Integer> byteValToCount = new HashMap<Byte,Integer>();
                for ( int i = 0; i < textureBytes.length; i ++ ) {
                    Integer oldVal = byteValToCount.get( textureBytes[ i ] );
                    if ( oldVal == null ) {
                        oldVal = new Integer( 0 );
                    }
                    byteValToCount.put( textureBytes[i], ++oldVal );

                }

                try {
                    TiffExporter exporter = new TiffExporter();
                    exporter.export( texture );
                    exporter.close();

                } catch ( Exception ex ) {
                    ex.printStackTrace();
                    logger.error( "Exception on tif export " + ex.getMessage() );
                    SessionMgr.getSessionMgr().handleException( ex );

                }

                for ( Byte b: byteValToCount.keySet() ) {
                    System.out.println("Value " + b + " appears " + byteValToCount.get( b ) + " times.");
                }
            }
        };

        FileExportLoadWorker fileExportLoadWorker = new FileExportLoadWorker(
                searchDatas, cropCoords, callback
        );
        fileExportLoadWorker.setResolver(new CacheFileResolver());
        fileExportLoadWorker.execute();
    }

}
