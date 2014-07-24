package org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements;

import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.alignment_board_viewer.AlignmentBoardSettings;
import org.janelia.it.workstation.gui.alignment_board_viewer.volume_export.CoordCropper3D;
import org.janelia.it.workstation.gui.dialogs.search.alignment_board.ABTargetedSearchDialog;
import org.janelia.it.workstation.gui.viewer3d.CropCoordSet;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/4/13
 * Time: 4:43 PM
 *
 * This will have the responsibility of presenting the user with choices for modifying data and behavior about
 * the Alignment Board Viewer.
 */
public class AlignmentBoardControls {
    public static final double UNSELECTED_DOWNSAMPLE_RATE = 0.0;

    private static final String DOWN_SAMPLE_TOOL_TIP =
            "<html>" +
                    "Data sent to screen may be too large for your graphics<br>" +
                    " card.  Therefore, they are downsampled at the minimum rate" +
                    " found usable for your card.<br>However, you may wish to move " +
                    "that rate up or down, depending on your knowledge of the<br>" +
                    "advanced hardware on your system.  Higher values mean larger, " +
                    "blockier voxels, and less memory."
                    + "</html>";

    private static final String GEO_SEARCH_TOOLTIP = "<html>" +
            "Adjust the X, Y, and Z sliders, to leave only the template volume of <br>" +
            "search lit.  This template volume will then be used to derive the coordinates, <br>" +
            "to search other specimens and present the resulting overlappoing volume." +
            "</html>";

    private static final String CONNECT_EVENTS_TOOLTIP_TEXT =
            "<html>Control whether add/delete makes the alignment board rebuild.<br>" +
            "If this is off, the board will not reflect changes in the Layers Panel.</html>";

    private static final String SAVE_AS_SEARCH_TIFF = "Save Search Mask";
    private static final String SAVE_AS_SEARCH_TIFF_TOOLTIP_TEXT = SAVE_AS_SEARCH_TIFF;
    private static final String SAVE_AS_COLOR_TIFF = "Save Color TIFF";
    private static final String SAVE_AS_COLOR_TIFF_TOOLTIP_TEXT = SAVE_AS_COLOR_TIFF;

    private static final String GAMMA_TOOLTIP = "Adjust the gamma level, or brightness.";
    private static final Dimension NEURON_LIMIT_TF_SIZE = new Dimension(150, 50);
    private static final String COMMIT_CHANGES = "Apply";
    private static final String COMMIT_CHANGES_TOOLTIP_TEXT = COMMIT_CHANGES;
    private static final String SAVE_SCREEN_SHOT_MIP = "Screen Shot/MIP";
    private static final String SAVE_SCREEN_SHOT_TOOLTIP_TEXT = SAVE_SCREEN_SHOT_MIP;
    private static final String DOWN_SAMPLE_RATE = "Down Sample";
    private static final String USE_SIGNAL_DATA = "Use Signal Data";
    private static final String OR_BUTTON_TIP = "<html>Combine <font color='red'>this</font> selection region<br>" +
            "with previous selection region(s)<br></html>";
    private static final String OR_BUTTON_LABEL = "OR";
    private static final String BACKGROUND_LABEL_TIP = "<html>Toggle background color between white (<emph>true</emph>) and black (<emph>false</emph>)</html>";
    private static final String CLEAR_BUTTON_LABEL = "Clear Selection";
    private static final String CLEAR_BUTTON_TOOLTIP_TEXT = "Drop all sub-volume selections made in this session.";
    private static final String NON_SELECT_BLACKOUT = "Non-selected region blacked out";
    private static final String NON_SELECT_BLACKOUT_ICON = "non_selected_black.png";
    private static final String NON_SELECT_DIM_ICON = "non_selected_dim.png";
    private static final String NON_SELECT_BLACKOUT_TOOLTIP_TEXT = NON_SELECT_BLACKOUT;
    private static final String SAVE_COLORS_BRIGHT = "Apply Brightness to Color TIFF";
    private static final String SAVE_COLORS_BRIGHT_TOOLTIP_TEXT = SAVE_COLORS_BRIGHT;
    private static final String ESTIMATED_BEST_RESOLUTION = "Best Guess";
    private static final String DOWN_SAMPLE_PROP_NAME = "AlignmentBoard_Downsample_Rate";
    private static final String GUESS_LABEL_FMT = "Best Guess: %s";
    private static final String MINIMUM_VOXEL_COUNT = "  " + AlignmentBoardSettings.DEFAULT_NEURON_SIZE_CONSTRAINT;
    private static final String MAXIMUM_NEURON_COUNT = " " + AlignmentBoardSettings.DEFAULT_MAX_NEURON_COUNT_CONSTRAINT;

    private static final float APPLY_NO_GAMMA = -1.0f;

    private Component centering;
    private JSlider brightnessSlider;
    private JCheckBox useSignalDataCheckbox;
    private JComboBox downSampleRateDropdown;
    private JLabel downSampleGuess;
    private JTextField minimumVoxelCountTF;
    private JTextField maxNeuronCountTF;

    private JButton searchSave;
    private JButton colorSave;
    private JButton screenShot;
    private JButton search;
    private AbstractButton connectEvents;

    private JButton commitButton;

    private JButton orButton;
    private JButton clearButton;

    private RangeSlider xSlider;
    private RangeSlider ySlider;
    private RangeSlider zSlider;

    private ABTargetedSearchDialog searchDialog;

    private ChangeListener selectionSliderListener;

    private int xMax;
    private int yMax;
    private int zMax;

    private JToggleButton blackout;
    private AbstractButton colorSaveBrightness;
    
    private AbstractButton whiteBackground;

    private boolean readyForOutput = false;

    private Map<Integer,Integer> downSampleRateToIndex;
    private final Collection<ControlsListener> listeners = new ArrayList<>();
    private VolumeModel volumeModel;
    private AlignmentBoardSettings settings;

    private final Logger logger = LoggerFactory.getLogger( AlignmentBoardControls.class );

    /**
     * @param centering this controls will be centered over the "centering" component.  Push externally-created settings
     *                  in here as a seed.
     */
    public AlignmentBoardControls(Component centering, VolumeModel volumeModel, AlignmentBoardSettings settings) {
        initForCtor(settings, centering, volumeModel);
    }

    /** Update all controls. */
    public void update( boolean visible ) {
        if ( visible ) {
            updateControlsFromSettings();
            updateCurrentSelectionFromSettings();
            updateCropOutLevelFromVolumeModel();
            updateColorBrightnessSaveFromVolumeModel();
            updateWhiteBackgroundFromVolumeModel();
        }
    }

    /** Control who observes.  Synchronized for thread safety. */
    public synchronized void addSettingsListener( ControlsListener listener ) {
        listeners.add(listener);
    }

    public synchronized void removeSettingsListener( ControlsListener listener ) {
        listeners.remove(listener);
    }

    public synchronized void removeAllSettingsListeners() {
        listeners.clear();
    }

    public void setVolumeMaxima( int x, int y, int z ) {
        logger.info("Volume maxima provided {}, {}, " + z, x, y);
        xMax = x;
        yMax = y;
        zMax = z;
        initializeSelectionRanges();
        // May have controls whose proper settings depend on the maxima.
        update(true);
    }

    public void dispose() {
        removeAllSettingsListeners();
        if ( selectionSliderListener != null ) {
            xSlider.removeChangeListener( selectionSliderListener );
            ySlider.removeChangeListener( selectionSliderListener );
            zSlider.removeChangeListener( selectionSliderListener );
        }
        selectionSliderListener = null;
        xSlider = ySlider = zSlider = null;
        volumeModel = null;
        centering = null;
        settings = null;
    }

    //--------------------------------------------HELPERS
    private void initForCtor(AlignmentBoardSettings settings, Component centering, VolumeModel volumeModel) {
        this.settings = settings;
        this.centering = centering;
        this.volumeModel = volumeModel;
        createGui();
        Double downsampleRate = AlignmentBoardControls.UNSELECTED_DOWNSAMPLE_RATE;
        try {
            downsampleRate = (Double)SessionMgr.getSessionMgr().getModelProperty(DOWN_SAMPLE_PROP_NAME);
            if ( downsampleRate == null ) {
                downsampleRate = AlignmentBoardControls.UNSELECTED_DOWNSAMPLE_RATE;
            }
        } catch ( Throwable ex ) {
            logger.warn( "Failed to fetch downsample rate setting." );
        }
        this.setNonSerializedDownSampleRate(downsampleRate);
    }
    
    private boolean isUseSignalData() {
        if ( ! isReadyForOutput() ) {
            return this.settings.isShowChannelData();
        }
        else {
            return useSignalDataCheckbox.isSelected();
        }
    }

    /**
     * Compute a factor suitable for gamma adjustment in the shader, given a range of 0..10 from the slider.
     * Max-point of 10 yields 0.0 (exponent takes output value to 1.0 in shader).
     * Mid-point of 5 yields 1.0, or same output.
     * Low point of 0 yields 2.0.
     *
     * Smaller values get pushed more dim with lower slider pos.  Values close to 1.0 do not get diminished much.
     * May require further attention after testing.
     *
     * @return computation above
     * @see #updateControlsFromSettings()
     */
    private double getGammaFactor() {
        if ( ! isReadyForOutput() )
            return settings.getGammaFactor();
        int value = brightnessSlider.getValue();
        double rtnVal = (((double)value/100.0) - 10.0) / -5.0;
        logger.debug("Returning gamma factor of {} for {}.", rtnVal, value);
        return rtnVal;
    }

    /** These getters may be called after successful launch. */
    private double getDownsampleRate() {
        if ( ! isReadyForOutput() )
            return settings.getChosenDownSampleRate();
        String selectedValue = downSampleRateDropdown.getItemAt( downSampleRateDropdown.getSelectedIndex() ).toString();
        if ( Character.isDigit( selectedValue.charAt( 0 ) ) )
            return Integer.parseInt( selectedValue );
        else
            return 0.0;
    }

    private long getMinimumVoxelCount() {
        long minimumVoxelCount = settings.getMinimumVoxelCount();
        if ( ! isReadyForOutput() ) {
            return minimumVoxelCount;
        }
        return getNeuronConstraint( minimumVoxelCountTF, AlignmentBoardSettings.DEFAULT_NEURON_SIZE_CONSTRAINT );
    }

    private long getMaxNeuronCount() {
        if ( ! isReadyForOutput() )
            return settings.getMaximumNeuronCount();
        return getNeuronConstraint( maxNeuronCountTF, AlignmentBoardSettings.DEFAULT_MAX_NEURON_COUNT_CONSTRAINT );
    }

    private long getNeuronConstraint( JTextField constraintTextField, long defaultValue ) {
        constraintTextField.setForeground(centering.getForeground());
        String selectedValue = constraintTextField.getText().trim();
        long rtnVal = defaultValue;
        try {
            // Except out if the input value is non-numeric, out of range or less than 1.
            rtnVal = Long.parseLong( selectedValue );
            if ( rtnVal < AlignmentBoardSettings.NO_NEURON_SIZE_CONSTRAINT) {
                throw new RuntimeException();
            }
        } catch ( RuntimeException ex ) {
            JOptionPane.showMessageDialog( SessionMgr.getMainFrame(), "Failed to parse count " + selectedValue + " to integer value.");
            constraintTextField.setForeground( Color.red );
        }

        return rtnVal;
    }

    /**
     * Causes the controls to be updated, per information in the settings.  Does not fire any listener updates.
     * Use this when a listener is causing the update, rather than receiving it.
     * @see #getGammaFactor()
     */
    private void updateControlsFromSettings() {
        useSignalDataCheckbox.setSelected( settings.isShowChannelData() );

        int value = (int)Math.round( ( ( settings.getGammaFactor() * -5.0 ) + 10.0 ) * 100.0 );
        brightnessSlider.setValue(value);

        long minimumVoxelCount = settings.getMinimumVoxelCount();
        minimumVoxelCountTF.setText( "" + minimumVoxelCount);

        long maximumNeuronCount = settings.getMaximumNeuronCount();
        maxNeuronCountTF.setText( "" + maximumNeuronCount );

        boolean eventConnected = settings.isEventConnected();
        connectEvents.setSelected( eventConnected );
    }

    /** Call this when sufficient info is avail to get the sliders positions initialized off crop-coords. */
    public void updateCurrentSelectionFromSettings() {
        CropCoordSet cropCoordSet = volumeModel.getCropCoords();
        if ( cropCoordSet.isEmpty() ) {
            resetSelectionSliders();
        }
        else {
            float[] currentCoords = cropCoordSet.getCurrentCoordinates();
            // Roll back to an accepted coordinate set.
            if ( currentCoords == null  ||  currentCoords[ 0 ] == -1  ||  CropCoordSet.allMaxCoords( currentCoords ) ) {
                if ( cropCoordSet.getAcceptedCoordinates().size() > 0 )
                    currentCoords = cropCoordSet.getAcceptedCoordinates().iterator().next();
            }

            int[] maxima = new int[] {
                    xMax, yMax, zMax
            };
            CoordCropper3D coordCropper = new CoordCropper3D();
            float[] denormalizedCoords = coordCropper.getDenormalizedCropCoords(
                    currentCoords, maxima
            );
            xSlider.setValue(Math.round(denormalizedCoords[0]));
            xSlider.setUpperValue(Math.round(denormalizedCoords[1]));

            ySlider.setValue(Math.round(denormalizedCoords[2]));
            ySlider.setUpperValue(Math.round(denormalizedCoords[3]));

            zSlider.setValue(Math.round(denormalizedCoords[4]));
            zSlider.setUpperValue(Math.round(denormalizedCoords[5]));
        }
    }

    // Getters to expose controls for external use in layout.
    public boolean isReadyForOutput() {
        return readyForOutput;
    }

    public void setReadyForOutput(boolean readyForOutput) {
        this.readyForOutput = readyForOutput;
    }

    public JSlider getBrightnessSlider() {
        return brightnessSlider;
    }

    public JCheckBox getUseSignalDataCheckbox() {
        return useSignalDataCheckbox;
    }

    public JComboBox getDownSampleRateDropdown() {
        return downSampleRateDropdown;
    }

    public JLabel getDownSampleGuess() {
        return downSampleGuess;
    }

    public JTextField getMinimumVoxelCountTF() {
        return minimumVoxelCountTF;
    }

    public JTextField getMaxNeuronCountTF() {
        return maxNeuronCountTF;
    }

    public AbstractButton getSearchSave() {
        return searchSave;
    }

    public AbstractButton getColorSave() {
        return colorSave;
    }

    public AbstractButton getScreenShot() {
        return screenShot;
    }

    public AbstractButton getSearch() {
        return search;
    }

    public AbstractButton getConnectEvents() {
        return connectEvents;
    }

    public JButton getCommitButton() {
        return commitButton;
    }

    public JButton getOrButton() {
        return orButton;
    }

    public JButton getClearButton() {
        return clearButton;
    }

    public RangeSlider getxSlider() {
        return xSlider;
    }

    public RangeSlider getySlider() {
        return ySlider;
    }

    public RangeSlider getzSlider() {
        return zSlider;
    }

    public AbstractButton getBlackout() {
        return blackout;
    }

    public AbstractButton getColorSaveBrightness() {
        return colorSaveBrightness;
    }

    public AbstractButton getWhiteBackground() {
        return whiteBackground;
    }

    private void updateCropOutLevelFromVolumeModel() {
        // Cropout is either dim or dark.  The setting is a float, but varying the level is not being exploited at
        // this time; hence the default is dim, and if the default is not in use, then the non-default of dark is used.
        boolean isCropBlackout = false;
        float cropOutLevel = volumeModel.getCropOutLevel();
        if ( cropOutLevel != VolumeModel.DEFAULT_CROPOUT ) {
            isCropBlackout = true;
        }
        blackout.setSelected(isCropBlackout);
    }

    private void updateWhiteBackgroundFromVolumeModel() {
        whiteBackground.setSelected(volumeModel.isWhiteBackground());
    }
    
    private void updateColorBrightnessSaveFromVolumeModel() {
        colorSaveBrightness.setSelected(volumeModel.isColorSaveBrightness());
    }

    /** As soon as the ranges are known (set), listeners, and initial ranges may be set on volume selection. */
    private void initializeSelectionRanges() {
        if ( xMax == 0 || yMax == 0 || zMax == 0 ) {
            logger.error( "Updating sliders before maxima have been set." );
        }
        xSlider.setValue(0);
        xSlider.setMaximum(xMax);
        xSlider.setUpperValue(xMax);

        ySlider.setValue(0);
        ySlider.setMaximum(yMax);
        ySlider.setUpperValue(yMax);

        zSlider.setValue( 0 );
        zSlider.setMaximum(zMax);
        zSlider.setUpperValue(zMax);

        selectionSliderListener = new SliderChangeListener(
                new RangeSlider[]{ xSlider, ySlider, zSlider }, volumeModel, this
        );

        xSlider.addChangeListener(selectionSliderListener);
        ySlider.addChangeListener(selectionSliderListener);
        zSlider.addChangeListener(selectionSliderListener);

    }

    /** Set the rate in the GUI, for now, only. */
    private void setNonSerializedDownSampleRate(double downSampleRate) {
        int downSampleRateInt = (int)Math.round(downSampleRate);
        Integer downsampleIndex = downSampleRateToIndex.get( downSampleRateInt );
        if ( downsampleIndex == null ) {
            JOptionPane.showMessageDialog(
                    centering,
                    "Invalid downsample rate of " + downSampleRate + " given.  Instead setting image size to 1/2."
            );
            downsampleIndex = 2;
        }
        downSampleRateDropdown.setSelectedIndex( downsampleIndex );
        settings.setChosenDownSampleRate(downSampleRate);
    }

    private void serializeDownsampleRate(double downSampleRate) {
        SessionMgr.getSessionMgr().setModelProperty(DOWN_SAMPLE_PROP_NAME, downSampleRate);
    }

    /**
     * This will fire only those listener methods affected by the current user commitment.
     */
    private synchronized void fireSettingsEvent() {
        double newGamma = getGammaFactor();
        boolean deltaBrightness = false;
        boolean deltaSettings = false;
        // Possibly: NPE happens because of an intermediate state when user has a stale reference.
        if ( settings == null ) {
            return;
        }
        if ( newGamma != settings.getGammaFactor() ) {
            settings.setGammaFactor( newGamma );
            deltaBrightness = true;
        }

        boolean newUseSignal = isUseSignalData();
        if ( newUseSignal != settings.isShowChannelData() ) {
            settings.setShowChannelData(newUseSignal);
            deltaSettings = true;
        }

        double newDownSampleRate = getDownsampleRate();
        if ( newDownSampleRate != settings.getChosenDownSampleRate() ) {
            settings.setChosenDownSampleRate(newDownSampleRate);
            serializeDownsampleRate(newDownSampleRate);
            deltaSettings = true;
        }

        long minimumVoxelCount = getMinimumVoxelCount();
        if ( minimumVoxelCount != settings.getMinimumVoxelCount() ) {
            settings.setMinimumVoxelCount(minimumVoxelCount);
            deltaSettings = true;
        }

        long maxNeuronCount = getMaxNeuronCount();
        if ( maxNeuronCount != settings.getMaximumNeuronCount() ) {
            settings.setMaximumNeuronCount(maxNeuronCount);
            deltaSettings = true;
        }

        for ( ControlsListener listener: listeners ) {
            if ( deltaBrightness ) {
                logger.debug("Setting brightness to {}.", settings.getGammaFactor());
                listener.setBrightness( settings.getGammaFactor() );
            }
            if ( deltaSettings ) {
                listener.updateSettings();
            }

        }
    }        

    private synchronized void fireCropEvent() {
        CropCoordSet cropCoordSet = volumeModel.getCropCoords();
        if ( cropCoordSet.getAcceptedCoordinates().size() > 0  ||  cropCoordSet.getCurrentCoordinates() != null ) {
            for ( ControlsListener listener: listeners ) {
                listener.updateCropCoords();
            }
        }
    }

    private synchronized void fireForceCropEvent() {
        for ( ControlsListener listener: listeners ) {
            listener.updateCropCoords();
        }
    }

    private synchronized void fireSavebackEvent( SavebackEvent event ) {
        for ( ControlsListener listener: listeners ) {
            listener.exportSelection(event);
        }

    }

    private synchronized void fireBlackOutCrop( boolean blackout ) {
        for ( ControlsListener listener: listeners ) {
            listener.setCropBlackout(blackout);
        }
    }

    private synchronized void fireEventConnectToggle() {
        boolean connectState = settings.isEventConnected();
        for ( ControlsListener listener: listeners ) {
            listener.setConnectEditEvents( connectState );
        }
    }
    
    private synchronized void fireBackgroundChangeEvent( boolean whiteBackgroundFlag ) {
        for ( ControlsListener listener: listeners ) {
            listener.setWhiteBackground( whiteBackgroundFlag );
        }
    }

    private synchronized void fireRebuild() {
        for ( ControlsListener listener: listeners ) {
            listener.forceRebuild();
        }
    }

    private void createGui() {
        Font oldFont = this.centering.getFont();
        if ( oldFont == null ) {
            oldFont = new JLabel().getFont();
        }
        Font newFont = new Font( oldFont.getName(), Font.PLAIN, 12 );
        //Font newFont = oldFont.deriveFont( 9 );

        xSlider = new RangeSlider();
        ySlider = new RangeSlider();
        zSlider = new RangeSlider();

        //        this(null, title, LEADING, DEFAULT_POSITION, null, null);
        xSlider.setBorder( makeFontedBorder("Selection X Bounds", newFont));
        ySlider.setBorder( makeFontedBorder( "Selection Y Bounds", newFont ) );
        zSlider.setBorder( makeFontedBorder( "Selection Z Bounds", newFont ) );

        blackout = new StateDrivenIconToggleButton( Icons.getIcon( NON_SELECT_BLACKOUT_ICON ), Icons.getIcon( NON_SELECT_DIM_ICON ) );
        blackout.setToolTipText(NON_SELECT_BLACKOUT_TOOLTIP_TEXT);
        blackout.setSelected(false);
        blackout.setFocusable(false);
        blackout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                fireBlackOutCrop(blackout.isSelected());
            }
        });

        colorSaveBrightness = new StateDrivenIconToggleButton( Icons.getIcon( "bright_color_save.png" ), Icons.getIcon( "dim_color_save.png" ) );
        colorSaveBrightness.setFocusable( false );
        colorSaveBrightness.setToolTipText( SAVE_COLORS_BRIGHT_TOOLTIP_TEXT );
        colorSaveBrightness.setSelected(VolumeModel.DEFAULT_SAVE_BRIGHTNESS);
        colorSaveBrightness.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        volumeModel.setColorSaveBrightness(colorSaveBrightness.isSelected());
                    }
                }
        );

        //TODO replace both of these icons with something more appropriate.
        whiteBackground = new StateDrivenIconToggleButton( Icons.getIcon( "brick_grey.png" ), Icons.getIcon( "brick.png" ) );
        whiteBackground.setFocusable(false);
        whiteBackground.setToolTipText(BACKGROUND_LABEL_TIP);
        whiteBackground.setSelected(volumeModel.isWhiteBackground());
        whiteBackground.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                volumeModel.setWhiteBackground( whiteBackground.isSelected() );
                fireBackgroundChangeEvent( volumeModel.isWhiteBackground() );
            }
        });

        commitButton = new JButton( COMMIT_CHANGES );
        commitButton.setToolTipText( COMMIT_CHANGES_TOOLTIP_TEXT );
        commitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                doSettingsEvent();
                commitButton.setEnabled( false );
            }
        });
        commitButton.setEnabled( false );

        searchSave = new JButton( Icons.getIcon( "masked_drive_go.png" ) );
        searchSave.setToolTipText( SAVE_AS_SEARCH_TIFF_TOOLTIP_TEXT );
        searchSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                logger.info(
                        "Selection covers (X): " + xSlider.getValue() + ".." + xSlider.getUpperValue() +
                                " and (Y): " + ySlider.getValue() + ".." + ySlider.getUpperValue() +
                                " and (Z): " + zSlider.getValue() + ".." + zSlider.getUpperValue()
                );
                setButtonBusy(searchSave);
                CompletionListener buttonEnableListener = new CompletionListener() {
                    @Override
                    public void complete() {
                        setButtonRelaxed(searchSave, SAVE_AS_SEARCH_TIFF_TOOLTIP_TEXT);
                    }
                };

                Collection<float[]> acceptedCords = getCombinedCropCoords(1.0);
                SavebackEvent event = new SavebackEvent();
                event.setAbsoluteCoords(acceptedCords);
                event.setCompletionListener(buttonEnableListener);
                event.setMethod(ControlsListener.ExportMethod.binary);
                event.setGammaFactor( APPLY_NO_GAMMA );
                fireSavebackEvent(event);

            }
        });

        colorSave = new JButton( Icons.getIcon( "color_drive_go.png" ) );
        colorSave.setToolTipText( SAVE_AS_COLOR_TIFF_TOOLTIP_TEXT );
        colorSave.addActionListener(new ActionListener() {
            private final CompletionListener buttonEnableListener = new CompletionListener() {
                @Override
                public void complete() {
                    setButtonRelaxed(colorSave, SAVE_AS_COLOR_TIFF_TOOLTIP_TEXT);
                }
            };

            @Override
            public void actionPerformed(ActionEvent ae) {
                setButtonBusy(colorSave);
                Collection<float[]> acceptedCords = getCombinedCropCoords(1.0);
                SavebackEvent event = new SavebackEvent();
                event.setAbsoluteCoords(acceptedCords);
                event.setCompletionListener(buttonEnableListener);
                event.setMethod(ControlsListener.ExportMethod.color);
                if (volumeModel.isColorSaveBrightness()) {
                    event.setGammaFactor(settings.getGammaFactor() * VolumeModel.STANDARDIZED_GAMMA_MULTIPLIER);
                } else {
                    event.setGammaFactor( APPLY_NO_GAMMA );
                }
                fireSavebackEvent(event);
            }
        });

        screenShot = new JButton( Icons.getIcon( "drive_go.png" ) );
        screenShot.setToolTipText(SAVE_SCREEN_SHOT_TOOLTIP_TEXT);
        screenShot.addActionListener(new ActionListener() {
            private final CompletionListener buttonEnableListener = new CompletionListener() {
                @Override
                public void complete() {
                    screenShot.setEnabled(true);
                }
            };

            @Override
            public void actionPerformed(ActionEvent ae) {
                screenShot.setEnabled(false);
                Collection<float[]> acceptedCords = getCombinedCropCoords();
                SavebackEvent event = new SavebackEvent();
                event.setAbsoluteCoords(acceptedCords);
                event.setCompletionListener(buttonEnableListener);
                event.setMethod(ControlsListener.ExportMethod.mip);
                event.setGammaFactor(settings.getGammaFactor());
                fireSavebackEvent(event);
            }
        });

        search = new JButton( Icons.getIcon( "find.png" ) );
        search.setText("Add to Board");
        search.setToolTipText("Add data to current Alignment Board");
        search.setText("Add Data to Board");
        // NOTE: for now, not disabling the button.  Launched dialog is modal, and will prevent other launch.
        search.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent ae ) {
                if ( searchDialog == null ) {
                    searchDialog = new ABTargetedSearchDialog(
                            AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext()
                    );
                }
                searchDialog.showDialog();
            }
        });

        clearButton = new JButton( CLEAR_BUTTON_LABEL );
        clearButton.setToolTipText( CLEAR_BUTTON_TOOLTIP_TEXT );
        clearButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent ae ) {
                resetSelectionSliders();
                CropCoordSet cropCoordSet = volumeModel.getCropCoords();
                cropCoordSet.setCurrentCoordinates(getCurrentCropCoords( getDownsampleRate() ));
                cropCoordSet.getAcceptedCoordinates().clear();
                fireForceCropEvent();
            }
        });

        connectEvents = new StateDrivenIconToggleButton( Icons.getIcon( "link.png" ), Icons.getIcon( "link_break.png" ) );
        connectEvents.setToolTipText(CONNECT_EVENTS_TOOLTIP_TEXT);
        connectEvents.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ( connectEvents.isSelected() ) {
                    int confirmation = JOptionPane.showConfirmDialog( SessionMgr.getMainFrame(),
                            "<html>This will cause Drag-and-Drop, deletion, or color/hide changes to be reflected in your alignment board,<br>" +
                                    "and will incur the usual delays in rebuilding the alignment board after such changes.<br>" +
                                    "Do you wish to rebuild the board now to reflect recent changes?</html>"
                    );
                    if ( confirmation == JOptionPane.YES_OPTION ) {
                        boolean connectState = settings.isEventConnected();
                        settings.setEventConnected( ! connectState );
                        fireEventConnectToggle();
                        // Must now fire the catch-up event.
                        fireRebuild();
                    }
                    else if ( confirmation == JOptionPane.NO_OPTION ) {
                        boolean connectState = settings.isEventConnected();
                        settings.setEventConnected( ! connectState );
                        fireEventConnectToggle();
                    }
                    else {
                        // must restore button to old toggle state.
                        getConnectEvents().setSelected( false );
                    }
                }
                else {
                    // Here: toggle the current state of event listening.
                    int confirmation = JOptionPane.showConfirmDialog( SessionMgr.getMainFrame(),
                            "<html>This will stop any Drag-and-Drop, deletion, or color/hide changes from appearing in the alignment board<br>" +
                                    "until you turn the link back on.  If you reopen the alignment board or restart the workstation,<br>" +
                                    "the link setting will automatically be turned back on. Please use this with caution.<br>" +
                                    "Would you like to continue?</html>",
                            "Temporarily Turn off Alignment Board Rebuilds",
                            JOptionPane.OK_CANCEL_OPTION
                    );
                    if ( confirmation == JOptionPane.OK_OPTION ) {
                        boolean connectState = settings.isEventConnected();
                        settings.setEventConnected( ! connectState );
                        fireEventConnectToggle();
                    }
                    else {
                        // must restore button to old toggle state.
                        getConnectEvents().setSelected( true );
                    }
                }

            }
        });
        
        orButton = new JButton( OR_BUTTON_LABEL );
        orButton.setToolTipText(OR_BUTTON_TIP);
        orButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                CropCoordSet cropCoordSet = volumeModel.getCropCoords();
                cropCoordSet.acceptCurrentCoordinates();
                fireCropEvent();
            }
        });

        brightnessSlider = new JSlider();
        brightnessSlider.setMaximum(1000);
        brightnessSlider.setMinimum(0);
        brightnessSlider.setMajorTickSpacing(500);
        brightnessSlider.setMinorTickSpacing(100);
        brightnessSlider.setLabelTable(brightnessSlider.createStandardLabels(500));
        brightnessSlider.setOrientation(JSlider.HORIZONTAL);
        brightnessSlider.setValue(500);  // Center it up.
        brightnessSlider.setPaintLabels(true);
        brightnessSlider.setPaintTicks(true);
        brightnessSlider.setToolTipText(GAMMA_TOOLTIP);
        brightnessSlider.setBorder(makeFontedBorder("Brightness", newFont));

        downSampleRateToIndex = new HashMap<>();
        downSampleRateToIndex.put( 0, 0 );
        downSampleRateToIndex.put( 1, 1 );
        downSampleRateToIndex.put( 2, 2 );
        downSampleRateToIndex.put( 4, 3 );
        downSampleRateToIndex.put( 8, 4 );

        downSampleRateDropdown = new JComboBox(
                new ABSDComboBoxModel( downSampleRateToIndex )
        );
        downSampleRateDropdown.setBorder(makeFontedBorder(DOWN_SAMPLE_RATE, newFont));
        downSampleRateDropdown.setToolTipText( DOWN_SAMPLE_TOOL_TIP );

        useSignalDataCheckbox = new JCheckBox( USE_SIGNAL_DATA );
        useSignalDataCheckbox.setSelected(true);

        // Prepare button for commit-to-screen.  If the user touches any of these, the commit-button is active.
        MouseAdapter commitEnablerMouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                commitButton.setEnabled( true );
            }
        };

        KeyListener newlineFireKeyListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if ( e.getKeyCode() == KeyEvent.VK_ENTER ) {
                    doSettingsEvent();
                }
            }
        };

        minimumVoxelCountTF = new JTextField(MINIMUM_VOXEL_COUNT);
        minimumVoxelCountTF.setMinimumSize(NEURON_LIMIT_TF_SIZE);
        minimumVoxelCountTF.setPreferredSize(NEURON_LIMIT_TF_SIZE);
        minimumVoxelCountTF.setMaximumSize(NEURON_LIMIT_TF_SIZE);
        minimumVoxelCountTF.setBorder(makeFontedBorder("Min Neuron Size", newFont));
        minimumVoxelCountTF.setToolTipText(
                "Integer: least number of neuron voxels before a neuron fragment is not rendered.\n" +
                        "Value of " + AlignmentBoardSettings.NO_NEURON_SIZE_CONSTRAINT + " implies no such filtering."
        );
        minimumVoxelCountTF.addMouseListener( commitEnablerMouseListener );
        minimumVoxelCountTF.addKeyListener( newlineFireKeyListener );

        maxNeuronCountTF = new JTextField(MAXIMUM_NEURON_COUNT);
        maxNeuronCountTF.setMinimumSize(NEURON_LIMIT_TF_SIZE);
        maxNeuronCountTF.setMaximumSize(NEURON_LIMIT_TF_SIZE);
        maxNeuronCountTF.setPreferredSize(NEURON_LIMIT_TF_SIZE);
        maxNeuronCountTF.setBorder(makeFontedBorder("Max Neuron Count", newFont));
        maxNeuronCountTF.setToolTipText(
                "Integer: max number of neurons allowed to be rendered.\n" +
                        "Value of " + AlignmentBoardSettings.NO_NEURON_SIZE_CONSTRAINT + " implies no such filtering."
        );
        maxNeuronCountTF.addMouseListener( commitEnablerMouseListener );
        maxNeuronCountTF.addKeyListener( newlineFireKeyListener );

        // Special Note: must avoid using "state change", as external methods can change the
        // "selected" state of this checkbox, inadvertently triggering the event, and firing off
        // the expensive (and with state-change, endlessly-looping) settings event.
        MouseAdapter fireSettingsMouseListener = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                fireSettingsEvent();
            }
        };
        ActionListener fireSettingsActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireSettingsEvent();
            }
        };
        downSampleRateDropdown.addMouseListener( fireSettingsMouseListener );
        for ( Component c: downSampleRateDropdown.getComponents() ) {
            // NOTE: must add the listener to all subcomponents of the combo box, or response is unreliable.
            if ( c instanceof AbstractButton ) {
                ((AbstractButton)c).addActionListener(fireSettingsActionListener);
            }
        }
        downSampleRateDropdown.addItemListener( new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                fireSettingsEvent();
            }
        });

        useSignalDataCheckbox.addChangeListener( new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                fireSettingsEvent();
            }
        });

        brightnessSlider.addChangeListener( new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                fireSettingsEvent();
            }
        });

        downSampleGuess = new JLabel( String.format( GUESS_LABEL_FMT, "0" ) );
        Observer downsampleRateObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                downSampleGuess.setText(  String.format( GUESS_LABEL_FMT, settings.getDownSampleGuessStr() ) );
            }
        };
        settings.setDownSampleRateObserver(downsampleRateObserver);

        this.getDownSampleRateDropdown().setFont( newFont );
        this.getDownSampleGuess().setFont( newFont );
        this.getCommitButton().setFont( newFont );
        this.getMaxNeuronCountTF().setFont( newFont );
        this.getMinimumVoxelCountTF().setFont( newFont );
        this.getClearButton().setFont( newFont );
        this.getOrButton().setFont( newFont );
        this.getConnectEvents().setFont( newFont );
        this.getBrightnessSlider().setFont(newFont);
        this.getUseSignalDataCheckbox().setFont(newFont);

    }

    private TitledBorder makeFontedBorder(String title, Font newFont) {
        return new TitledBorder( null, title, TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, newFont, null );
    }

    private void doSettingsEvent() {
        setReadyForOutput( true );
        fireSettingsEvent();
    }

    private void setButtonRelaxed(JButton saveButton, String tooltipText ) {
        saveButton.setCursor(Cursor.getDefaultCursor());
        saveButton.setToolTipText( tooltipText );
        saveButton.setEnabled(true);
    }

    private void setButtonBusy(JButton saveButton) {
        saveButton.setEnabled(false);
        saveButton.setToolTipText("Saving...");
        saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    private void resetSelectionSliders() {
        resetSelectionSlider(xSlider);
        resetSelectionSlider(ySlider);
        resetSelectionSlider(zSlider);
    }

    private void resetSelectionSlider( RangeSlider rangeSlider ) {
        rangeSlider.setUpperValue(rangeSlider.getMaximum());
        rangeSlider.setValue(rangeSlider.getMinimum());
    }

    private Collection<float[]> getCombinedCropCoords() {
        return getCombinedCropCoords( getDownsampleRate() );
    }

    private Collection<float[]> getCombinedCropCoords( double downSampleRate ) {
        float[] absoluteCropCoords = getCurrentCropCoords( downSampleRate );
        Collection<float[]> acceptedCoords = getMicrometerCropCoords( downSampleRate );
        if ( ! CropCoordSet.alreadyAccepted( acceptedCoords, absoluteCropCoords ) ) {
            Collection<float[]> combinedCoords = new HashSet<>();
            combinedCoords.add( absoluteCropCoords );
            combinedCoords.addAll( acceptedCoords );
            return combinedCoords;
        }
        else {
            return acceptedCoords;
        }
    }

    private Collection<float[]> getMicrometerCropCoords( double downSampleRate ) {
        CropCoordSet cropCoordSet = volumeModel.getCropCoords();
        Collection<float[]> micrometerCropCoords = new HashSet<>( cropCoordSet.getAcceptedCoordinates().size() );
        int[] maxima = new int[] {
                xSlider.getMaximum(), ySlider.getMaximum(), zSlider.getMaximum()
        };
        for ( float[] nextAccepted: cropCoordSet.getAcceptedCoordinates() ) {
            CoordCropper3D cropper3D = new CoordCropper3D();
            float[] adjusted = cropper3D.getDenormalizedCropCoords( nextAccepted, maxima, downSampleRate );
            micrometerCropCoords.add( adjusted );
        }
        return micrometerCropCoords;
    }

    private float[] getCurrentCropCoords( double downSampleRate ) {
        boolean partialVolumeConstraints = false;
        if ( xSlider.getValue() != 0  ||  ySlider.getValue() != 0  ||  zSlider.getValue() != 0 ) {
            partialVolumeConstraints = true;
        }
        else if ( xSlider.getUpperValue() != xSlider.getMaximum()  ||
                ySlider.getUpperValue() != ySlider.getMaximum()  ||
                zSlider.getUpperValue() != zSlider.getMaximum() ) {
            partialVolumeConstraints = true;
        }

        CoordCropper3D cropper = new CoordCropper3D();
        return partialVolumeConstraints ?
                cropper.getCropCoords( xSlider, ySlider, zSlider, downSampleRate ) :
                null;
    }

    //-------------------------------------------------INNER CLASSES/INTERFACES

    class StateDrivenIconToggleButton extends JToggleButton {
        private final Icon setIcon;
        private final Icon unsetIcon;
        public StateDrivenIconToggleButton( Icon setIcon, Icon unsetIcon ) {
            this.setIcon = setIcon;
            this.unsetIcon = unsetIcon;
        }

        @Override
        public Icon getIcon() {
            if ( this.isSelected() ) {
                return setIcon;
            }
            else {
                return unsetIcon;
            }
        }
    }

    /** Simple list-and-map-driven combo box model. */
    class ABSDComboBoxModel implements ComboBoxModel {

        private final List<Integer> rates = new ArrayList<>();
        private Object selectedItem;

        public ABSDComboBoxModel( Map<Integer,Integer> rateToIndex ) {
            for ( Integer rate: rateToIndex.keySet() ) {
                rates.add( rate );
            }
            selectedItem = UNSELECTED_DOWNSAMPLE_RATE;
        }

        @Override
        public int getSize() {
            return rates.size();
        }

        @Override
        public Object getElementAt(int index) {
            return wrapWithDescription(rates.get(index));
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            // NADA.  Ignore.
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            // NADA.  Ignore.
        }

        @Override
        public void setSelectedItem(Object anItem) {
            selectedItem = anItem;
        }

        @Override
        public Object getSelectedItem() {
            return selectedItem;
        }

        private String wrapWithDescription( Object item ) {
            if ( item.equals( 0 ) ) {
                return ESTIMATED_BEST_RESOLUTION;
            }
            else {
                return item.toString();
            }
        }
    }

    /** This listener updates the current display with latest slider tweaks. */
    public static class SliderChangeListener implements ChangeListener {
        private final RangeSlider[] sliders;
        private final AlignmentBoardControls controls;
        private final VolumeModel volumeModel;

        public SliderChangeListener(
                RangeSlider[] rangeSliders,
                VolumeModel volumeModel,
                AlignmentBoardControls controls
        ) {
            this.sliders = rangeSliders;
            this.volumeModel = volumeModel;
            this.controls = controls;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            float[] cropCoords = new CoordCropper3D().getNormalizedCropCoords(sliders);
            CropCoordSet cropCoordSet = volumeModel.getCropCoords();
            cropCoordSet.setCurrentCoordinates( cropCoords );
            controls.fireCropEvent();
        }

    }

}
