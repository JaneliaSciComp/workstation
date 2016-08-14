package org.janelia.it.workstation.gui.viewer3d;

import org.janelia.it.workstation.gui.viewer3d.axes.AxesActor;
import org.janelia.it.jacs.shared.geom.Vec3;
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
            brick.setPrimaryTextureData(signalTexture);
            returnValue = brick ;
        }
        return returnValue;
    }

    /**
     * A multi-thread-load-friendly overload of the set-volume method.  The texture objects may be
     * built at the caller's leisure, rather than being requested of passed-in builders.  This
     * method does NOT reset the view.
     *
     * @param volumeModel for delegated methods.
     * @param factory for creating volume brick.
     * @param signalTexture for the intensity data.
     * @param maskTexture for the labels.
     * @param renderMapping for the mapping of labels to rendering techniques.
     * @return true if sufficient params passed.
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
            brick.addTextureData( signalTexture );
            actor = brick;
        }
        else {
            actor = buildAxesActor( createBoundsOfVolumeModel(volumeModel), 1.0, volumeModel );
        }
        return actor;
    }

    /**
     * Creates the actor to draw the axes on the screen.
     *
     * @param boundingBox tells extrema for the axes.
     * @param axisLengthDivisor applies downsampling abbreviation of axes.
     * @param volumeModel tells the axes actor whether its background will be white.
     * @return the actor.
     */
    public GLActor buildAxesActor(org.janelia.it.jacs.shared.viewer3d.BoundingBox3d boundingBox, double axisLengthDivisor, VolumeModel volumeModel) {
        AxesActor axes = new AxesActor();
        axes.setVolumeModel(volumeModel);
        axes.setAxisLengths( boundingBox.getWidth(), boundingBox.getHeight(), boundingBox.getDepth() );
        axes.setAxisLengthDivisor( axisLengthDivisor );
        axes.setFullAxes( true );
        return axes;
    }

    private org.janelia.it.jacs.shared.viewer3d.BoundingBox3d createBoundsOfVolumeModel(VolumeModel volumeModel) {
        org.janelia.it.jacs.shared.viewer3d.BoundingBox3d boundingBox = new org.janelia.it.jacs.shared.viewer3d.BoundingBox3d();
        boundingBox.setMax(
                new Vec3(
                        volumeModel.getVoxelDimensions()[0],
                        volumeModel.getVoxelDimensions()[1],
                        volumeModel.getVoxelDimensions()[2])
        );
        boundingBox.setMin(new Vec3(0, 0, 0));
        return boundingBox;
    }

}
