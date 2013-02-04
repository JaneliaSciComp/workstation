package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/1/13
 * Time: 1:34 PM
 *
 * Holds information about a neuron fragment, or any other thing represented within a consolidated
 * label file by number.
 */
public class FragmentBean {
    private String labelFile;
    private int labelFileNum = -1;
    private int translatedNum;
    private byte[] rgb;
    private Entity fragment;

    public String getLabelFile() {
        return labelFile;
    }

    public void setLabelFile(String labelFile) {
        this.labelFile = labelFile;
    }

    public int getLabelFileNum() {
        return labelFileNum;
    }

    public int getTranslatedNum() {
        return translatedNum;
    }

    public void setTranslatedNum(int translatedNum) {
        this.translatedNum = translatedNum;
    }

    public byte[] getRgb() {
        return rgb;
    }

    public void setRgb(byte[] rgb) {
        this.rgb = rgb;
    }

    public Entity getFragment() {
        return fragment;
    }

    public void setFragment(Entity fragment) {
        this.fragment = fragment;
        String[] nameParts = fragment.getName().trim().split(" ");
        // In establishing the label file number, must add one to account for 0-based neuron numbering
        // by name.  The number 0 cannot be used to represent a neuron, since it is needed for "nothing".
        // Therefore, there is a discrepancy between the naming and the numbering as done in the luminance file.
        labelFileNum = (Integer.parseInt( nameParts[ nameParts.length - 1 ] )) + 1;
    }
}
