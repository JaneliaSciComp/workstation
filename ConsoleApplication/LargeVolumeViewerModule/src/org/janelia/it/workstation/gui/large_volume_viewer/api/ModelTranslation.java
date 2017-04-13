package org.janelia.it.workstation.gui.large_volume_viewer.api;

import java.awt.Color;

import org.janelia.console.viewerapi.model.ChannelColorModel;
import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmChannelColorModel;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmColorModel;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.shared.utils.ColorUtils;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;

/**
 * Translate between the TM domain object and the LVV object model.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ModelTranslation {

    /**
     * Create a TmColorModel representing the given ImageColorModel.
     * @param colorModel data source
     * @return new TmColorModel
     */
    public static TmColorModel translateColorModel(ImageColorModel colorModel) {

        TmColorModel tmColorModel = new TmColorModel();
        tmColorModel.setChannelCount(colorModel.getChannelCount());
        tmColorModel.setBlackSynchronized(colorModel.isBlackSynchronized());
        tmColorModel.setGammaSynchronized(colorModel.isGammaSynchronized());
        tmColorModel.setWhiteSynchronized(colorModel.isWhiteSynchronized());

        for(int i=0; i<colorModel.getChannelCount(); i++) {
            ChannelColorModel channelModel = colorModel.getChannel(i);
            TmChannelColorModel tmChannelModel = new TmChannelColorModel();
            tmChannelModel.setBlackLevel(channelModel.getBlackLevel());
            tmChannelModel.setGamma(channelModel.getGamma());
            tmChannelModel.setWhiteLevel(channelModel.getWhiteLevel());
            tmChannelModel.setColor(channelModel.getColor());
            tmChannelModel.setVisible(channelModel.isVisible());
            tmChannelModel.setCombiningConstant(channelModel.getCombiningConstant());
            tmColorModel.getChannels().add(tmChannelModel);
        }

        return tmColorModel;
    }

    /**
     * Update the given ImageColorModel with information from the TmColorModel.
     * @param tmColorModel data source
     * @param colorModel model to update
     */
    public static void updateColorModel(TmColorModel tmColorModel, ImageColorModel colorModel) {

        if (colorModel.getChannelCount()!=tmColorModel.getChannelCount()) {
            throw new IllegalStateException("Channel count does not match ("+colorModel.getChannelCount()+"!="+tmColorModel.getChannelCount()+")");
        }
        colorModel.setBlackSynchronized(tmColorModel.isBlackSynchronized());
        colorModel.setGammaSynchronized(tmColorModel.isGammaSynchronized());
        colorModel.setWhiteSynchronized(tmColorModel.isWhiteSynchronized());

        int index = 0;
        for(TmChannelColorModel tmChannelModel : tmColorModel.getChannels()) {
            ChannelColorModel channelModel = colorModel.getChannel(index++);
            channelModel.setBlackLevel(tmChannelModel.getBlackLevel());
            channelModel.setGamma(tmChannelModel.getGamma());
            channelModel.setWhiteLevel(tmChannelModel.getWhiteLevel());
            channelModel.setColor(tmChannelModel.getColor());
            channelModel.setVisible(tmChannelModel.isVisible());
            channelModel.setCombiningConstant((float)tmChannelModel.getCombiningConstant());
        }
    }

    public static NeuronStyle translateNeuronStyle(TmNeuronMetadata tmNeuronMetadata) {
        if (tmNeuronMetadata.getColor()==null) {
            return NeuronStyle.getStyleForNeuron(tmNeuronMetadata.getId(), tmNeuronMetadata.isVisible());
        }
        else {
            return new NeuronStyle(tmNeuronMetadata.getColor(), tmNeuronMetadata.isVisible());
        }
    }

    public static void updateNeuronStyle(NeuronStyle style, TmNeuronMetadata tmNeuronMetadata) {
        tmNeuronMetadata.setColor(style.getColor());
        tmNeuronMetadata.setVisible(style.isVisible());
    }

    public static String getColorHex(Color color) {
        return color==null?null:ColorUtils.toHex(color);
    }
    
}
