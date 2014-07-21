package org.janelia.it.workstation.gui.viewer3d;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.workstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.workstation.gui.viewer3d.texture.RenderMapTextureBean;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;

/**
 * Carries out step(s) required to make a volume brick actor.
 *
 * Created by fosterl on 6/5/14.
 */
public class VolumeBrickActorBuilder {
    public GLActor buildVolumeBrickActor(
            VolumeModel volumeModel, VolumeBrickFactory volumeBrickFactory, FileResolver resolver, String fileName
    ) {
        GLActor returnValue = null;
        VolumeLoader volumeLoader = new VolumeLoader(resolver);
        if (volumeLoader.loadVolume(fileName)) {
            volumeModel.removeAllListeners();
            volumeModel.resetToDefaults();
            VolumeBrickI brick = volumeBrickFactory.getVolumeBrick( volumeModel );
            volumeLoader.populateVolumeAcceptor(brick);

            returnValue = brick;
        }
        return returnValue;
    }

    /**
     * This overload, for a simple signal volume, may be used if the signal texture must be built at
     * some upstream process.
     *
     * @param signalTexture the pre-built texture.
     * @param volumeModel for creating volume brick.
     * @param volumeBrickFactory for creating volume brick.
     */
    public GLActor buildVolumeBrickActor(VolumeModel volumeModel, VolumeBrickFactory volumeBrickFactory, TextureDataI signalTexture) {
        GLActor returnValue = null;
        if ( signalTexture != null ) {
            volumeModel.removeAllListeners();
            volumeModel.resetToDefaults();
            VolumeBrickI brick = volumeBrickFactory.getVolumeBrick(volumeModel);
            brick.setTextureData(signalTexture);
            returnValue = brick ;
        }
        return returnValue;
    }

    /**
     * A null-friendly overload.  The texture objects may be
     * built at the caller's leisure, rather than being requested of 
     * passed-in builders.  This method does NOT reset the view.
     *
     * @param volumeModel for delegated methods.
     * @param factory for creating volume brick.
     * @param signalTexture for the intensity data.
     * @param maskTexture for the labels.
     * @param renderMapping for the mapping of labels to rendering techniques.
     * @return a new brick actor if sufficient params passed.
     */
    public GLActor buildVolumeBrickActor(
            VolumeModel volumeModel,
            TextureDataI signalTexture,
            TextureDataI maskTexture,
            VolumeBrickFactory factory,
            RenderMappingI renderMapping) {

        GLActor actor = null;
        if ( signalTexture != null ) {
            VolumeBrickI brick = null;
            if ( maskTexture != null ) {
                RenderMapTextureBean renderMapTextureData = new RenderMapTextureBean();
                renderMapTextureData.setMapping( renderMapping );
                renderMapTextureData.setVolumeModel( volumeModel );

                brick = factory.getVolumeBrick( volumeModel, maskTexture, renderMapTextureData );
            }
            else {
                brick = factory.getVolumeBrick( volumeModel );
            }
            brick.setTextureData( signalTexture );
            actor = brick;
        }
        else {
            actor = buildAxesActor( createBoundsOfVolumeModel(volumeModel), 1.0 );
        }
        return actor;
    }

    /**
     * This overload makes multiple actors, to accommodate OpenGL/JOGL limits.
     *
     * @param volumeModel for delegated methods.
     * @param factory for creating volume brick.
     * @param signalTexture for the intensity data.
     * @param maskTexture for the labels.
     * @param renderMapping for the mapping of labels to rendering techniques.
     * @return all required actors.
     */
    public GLActor[] buildVolumeBrickActors(
            VolumeModel volumeModel,
            TextureDataI signalTexture,
            TextureDataI maskTexture,
            VolumeBrickFactory factory,
            RenderMappingI renderMapping) {
        
        checkActorParameters(signalTexture, maskTexture);
        int chunkCount = signalTexture.getTextureData().getVolumeChunks().length;

        // Making one actor per chunk.
//chunkCount = 1; // TEMP;
        GLActor[] actors = new GLActor[ chunkCount ];
        for (int i = 0; i < chunkCount; i++) {
            RenderMapTextureBean renderMapTextureData = new RenderMapTextureBean();
            renderMapTextureData.setMapping(renderMapping);
            renderMapTextureData.setVolumeModel(volumeModel);

            VolumeBrickI brick = factory.getPartialVolumeBrick(volumeModel, signalTexture, maskTexture, renderMapTextureData, i);
            actors[ i ] = brick;
        }
        
        return actors;
    }

    /**
     * Creates the actor to draw the axes on the screen.
     *
     * @param boundingBox tells extrema for the axes.
     * @param axisLengthDivisor applies downsampling abbreviation of axes.
     * @return the actor.
     */
    public GLActor buildAxesActor(BoundingBox3d boundingBox, double axisLengthDivisor) {
        AxesActor axes = new AxesActor();
        axes.setAxisLengths( boundingBox.getWidth(), boundingBox.getHeight(), boundingBox.getDepth() );
        axes.setAxisLengthDivisor( axisLengthDivisor );
        axes.setFullAxes( true );
        return axes;
    }

    private BoundingBox3d createBoundsOfVolumeModel(VolumeModel volumeModel) {
        BoundingBox3d boundingBox = new BoundingBox3d();
        boundingBox.setMax(
                new Vec3(
                        volumeModel.getVoxelDimensions()[0],
                        volumeModel.getVoxelDimensions()[1],
                        volumeModel.getVoxelDimensions()[2])
        );
        boundingBox.setMin(new Vec3(0, 0, 0));
        return boundingBox;
    }

    private void checkActorParameters(TextureDataI signalTexture, TextureDataI maskTexture) throws IllegalArgumentException {
        if ( signalTexture == null || maskTexture == null ) {
            throw new IllegalArgumentException( "Mask and signal must both be non-null." );
        }
        final int signalChunkCount = signalTexture.getTextureData().getVolumeChunks().length;
        final int maskChunkCount = maskTexture.getTextureData().getVolumeChunks().length;
        
        if ( signalChunkCount != maskChunkCount ) {
            String msg = String.format( 
                    "Mask and signal must have same number of chunks. Signal has %d; mask has %d.",
                    signalChunkCount, maskChunkCount
            );
            throw new IllegalArgumentException( msg );
        }
    }

}
