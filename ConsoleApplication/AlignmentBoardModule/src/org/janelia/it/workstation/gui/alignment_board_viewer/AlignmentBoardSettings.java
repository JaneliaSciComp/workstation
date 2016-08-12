package org.janelia.it.workstation.gui.alignment_board_viewer;

import java.util.Date;
import java.util.Observer;

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
    public static final long DEFAULT_NEURON_SIZE_CONSTRAINT = 1000;
    public static final long DEFAULT_MAX_NEURON_COUNT_CONSTRAINT = 100;
    public static final int NO_NEURON_SIZE_CONSTRAINT = -1;

    private double chosenDownSampleRate;
    private double gammaFactor =  DEFAULT_GAMMA;
    private boolean showChannelData = true;
    private boolean eventConnected = true;
    private double downSampleGuess;
    private Observer sampleRateObserver;
    private long minimumVoxelCount = DEFAULT_NEURON_SIZE_CONSTRAINT;
    private long maximumNeuronCount = DEFAULT_MAX_NEURON_COUNT_CONSTRAINT;

    private Date creationStamp;
    public String toString() {
        return "AlignmentBoardSettings created at " + creationStamp + " with guess of " + downSampleGuess;
    }

    public AlignmentBoardSettings() {
        super();
        creationStamp = new Date();
    }

    public AlignmentBoardSettings( double downSampleRate, double downSampleGuess, double gammaFactor, boolean showChannelData ) {
        this();
        this.chosenDownSampleRate = downSampleRate;
        this.gammaFactor = gammaFactor;
        this.showChannelData = showChannelData;
        this.downSampleGuess = downSampleGuess;
    }

    public double getChosenDownSampleRate() {
        return chosenDownSampleRate;
    }

    public void setChosenDownSampleRate(double downSampleRate) {
        this.chosenDownSampleRate = downSampleRate;
    }

    public void setDownSampleRateObserver( Observer o ) {
        this.sampleRateObserver = o;
    }

    /**
     * This "guess" is determined from graphics card, but never serialized.  If the user picks something
     * in particular, that will be used.  But if not, their guess will be used instead.
     *
     * @param downSampleRate will be used in cases where the "best guess" choice has been accepted by user.
     */
    public void setDownSampleGuess(double downSampleRate) {
        this.downSampleGuess = downSampleRate;
        if ( sampleRateObserver != null ) {
            sampleRateObserver.update( null, downSampleRate );
        }
    }

    public double getDownSampleGuess() {
        return downSampleGuess;
    }

    public String getDownSampleGuessStr() {
        String rtnVal = new Double( downSampleGuess ).toString();
        return rtnVal.substring( 0, rtnVal.indexOf( '.' ) );
    }

    /** Call this to get the downsample rate that is actually used onscreen. */
    public double getAcceptedDownsampleRate() {
        if ( chosenDownSampleRate == 0.0 ) {
            return getDownSampleGuess();
        }
        else {
            return getChosenDownSampleRate();
        }
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
        return new AlignmentBoardSettings( getChosenDownSampleRate(), getDownSampleGuess(), getGammaFactor(), isShowChannelData() );
    }

    public long getMinimumVoxelCount() {
        return minimumVoxelCount;
    }

    public void setMinimumVoxelCount(long minimumVoxelCount) {
        this.minimumVoxelCount = minimumVoxelCount;
    }

    public long getMaximumNeuronCount() {
        return maximumNeuronCount;
    }

    public void setMaximumNeuronCount(long maximumNeuronCount) {
        this.maximumNeuronCount = maximumNeuronCount;
    }

    public boolean isEventConnected() {
        return eventConnected;
    }

    public void setEventConnected(boolean eventConnected) {
        this.eventConnected = eventConnected;
    }

    /** For dumping the settings in semi-human-readable fashion. */
    public String printSettings() {
        return String.format(
                "uDnSamp=%4.2f;gDnSamp=%4.2f;gamma=%4.3f;chan=%s;conn=%s;minVox=%d;maxNeu=%d",
                chosenDownSampleRate, downSampleGuess,
                gammaFactor,
                new Boolean(showChannelData).toString(), 
                new Boolean(eventConnected).toString(),
                minimumVoxelCount, maximumNeuronCount
        );
    }
}
