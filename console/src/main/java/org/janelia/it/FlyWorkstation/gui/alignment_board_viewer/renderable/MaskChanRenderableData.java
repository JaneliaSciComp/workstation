package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/29/13
 * Time: 12:25 PM
 *
 * A parameter class to group all metadata about a renderable, suitable for fetching the binary component, especially
 * as used by Mask/Channel file pairs.
 */
public class MaskChanRenderableData {
    private RenderableBean bean;
    private String channelPath;
    private String maskPath;
    private boolean compartment;

    public RenderableBean getBean() {
        return bean;
    }

    public void setBean(RenderableBean bean) {
        this.bean = bean;
    }

    public String getChannelPath() {
        return channelPath;
    }

    public void setChannelPath(String channelPath) {
        this.channelPath = channelPath;
    }

    public String getMaskPath() {
        return maskPath;
    }

    public void setMaskPath(String maskPath) {
        this.maskPath = maskPath;
    }

    public boolean isCompartment() {
        return compartment;
    }

    public void setCompartment(boolean compartment) {
        this.compartment = compartment;
    }
}
