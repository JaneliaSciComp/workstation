package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/11/13
 * Time: 10:20 AM
 *
 * This is an exchange bean for settings, so that load-workers do not have to know about the GUI, and so that
 * multiple small settings can be kept together and cut down parameter list sizes.
 */
public class AlignmentBoardSettings {
    public static final double DEFAULT_GAMMA = 1.0;

    private double downSampleRate;
    private double gammaFactor =  DEFAULT_GAMMA;
    private boolean showChannelData;

    public AlignmentBoardSettings() {
        super();
    }

    public AlignmentBoardSettings( double downSampleRate, double gammaFactor, boolean showChannelData ) {
        this();
        this.downSampleRate = downSampleRate;
        this.gammaFactor = gammaFactor;
        this.showChannelData = showChannelData;
    }

    public double getDownSampleRate() {
        return downSampleRate;
    }

    public void setDownSampleRate(double downSampleRate) {
        this.downSampleRate = downSampleRate;
    }

    public double getGammaFactor() {
        return gammaFactor;
    }

    public void setGammaFactor(double gammaFactor) {
        this.gammaFactor = gammaFactor;
    }

    public boolean isShowChannelData() {
        return showChannelData;
    }

    public void setShowChannelData(boolean showChannelData) {
        this.showChannelData = showChannelData;
    }

    public AlignmentBoardSettings clone() throws CloneNotSupportedException {
        //super.clone();
        return new AlignmentBoardSettings( getDownSampleRate(), getGammaFactor(), isShowChannelData() );
    }
}
