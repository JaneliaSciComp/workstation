package org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeModel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.CoordCropper3D;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.CropCoordSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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
public class AlignmentBoardControlsDialog extends JDialog {
    public static final double UNSELECTED_DOWNSAMPLE_RATE = 0.0;
    private static final String DOWN_SAMPLE_TIP =
            "Data sent to screen may be too large for your graphics card. Therefore, they are\n" +
            "downsampled at the minimum rate found usable for your card. However, you may wish\n" +
            "to move that rate up or down, depending on your knowledge of the advanced hardware\n" +
            "on your system.  Higher values mean larger, blockier voxels, and less memory.";

    private static final String DOWN_SAMPLE_TOOL_TIP =
            "<html>" +
                    "Data sent to screen may be too large for your graphics card.<br>" +
                    "Therefore, they are downsampled at the minimum rate found usable for your card." +
                    "<br>However, you may wish to move that rate up or down, depending on your knowledge of the<br>" +
                    "advanced hardware on your system.  Higher values mean larger, blockier voxels, and less memory."
                    + "</html>";

    private static final String GEO_SEARCH_TOOLTIP = "<html>" +
            "Adjust the X, Y, and Z sliders, to leave only the template volume of <br>" +
            "search lit.  This template volume will then be used to derive the coordinates, <br>" +
            "to search other specimens and present the resulting overlappoing volume." +
            "</html>";

    private static final String SAVE_AS_SEARCH_TIFF = "Save Search Mask";
    private static final String SAVE_AS_SEARCH_TIFF_TOOLTIP_TEXT = SAVE_AS_SEARCH_TIFF;
    private static final String SAVE_AS_COLOR_TIFF = "Save Color TIFF";
    private static final String SAVE_AS_COLOR_TIFF_TOOLTIP_TEXT = SAVE_AS_COLOR_TIFF;

    private static final int WIDTH = 600;
    private static final int HEIGHT = 800;
    private static final Dimension DOWNSAMPLE_TIP_DIM = new Dimension(WIDTH, 70);

    private static final String LAUNCH_AS = "Controls";
    private static final String LAUNCH_DESCRIPTION = "Present a dialog allowing users to change settings.";
    private static final Dimension SIZE = new Dimension( WIDTH, HEIGHT);
    private static final String GAMMA_TOOLTIP = "Adjust the gamma level, or brightness.";
    private static final Dimension DN_SAMPLE_DROPDOWN_SIZE = new Dimension(130, 50);
    private static final String COMMIT_CHANGES = "Commit Changes";
    private static final String COMMIT_CHANGES_TOOLTIP_TEXT = COMMIT_CHANGES;
    private static final String DISMISS_DIALOG = "Dismiss";
    private static final String SAVE_SCREEN_SHOT_MIP = "Screen Shot/MIP";
    private static final String SAVE_SCREEN_SHOT_TOOLTIP_TEXT = SAVE_SCREEN_SHOT_MIP;
    private static final String DOWN_SAMPLE_RATE = "Down Sample Rate";
    private static final String USE_SIGNAL_DATA = "Use Signal Data";
    private static final String OR_BUTTON_TIP = "<html>Combine <font color='red'>this</font> selection region<br>" +
            "with previous selection region(s)<br></html>";
    private static final String OR_BUTTON_LABEL = "OR";
    private static final String CLEAR_BUTTON_LABEL = "Clear Selection";
    private static final String CLEAR_BUTTON_TOOLTIP_TEXT = "Drop all sub-volume selections made in this session.";
    private static final String NON_SELECT_BLACKOUT = "Non-selected region blacked out";
    private static final String NON_SELECT_BLACKOUT_TOOLTIP_TEXT = NON_SELECT_BLACKOUT;
    private static final String ESTIMATED_BEST_RESOLUTION = "Best Guess";
    private static final String DOWN_SAMPLE_PROP_NAME = "AlignmentBoard_Downsample_Rate";

    private final Component centering;
    private JSlider brightnessSlider;
    private JCheckBox useSignalDataCheckbox;
    private JComboBox downSampleRateDropdown;

    private RangeSlider xSlider;
    private RangeSlider ySlider;
    private RangeSlider zSlider;

    private ChangeListener selectionSliderListener;

    private int xMax;
    private int yMax;
    private int zMax;

    private JCheckBox blackoutCheckbox;

    private boolean readyForOutput = false;

    private Map<Integer,Integer> downSampleRateToIndex;
    private final Collection<ControlsListener> listeners;
    private final VolumeModel volumeModel;
    private final AlignmentBoardSettings settings;

    private final Logger logger = LoggerFactory.getLogger( AlignmentBoardControlsDialog.class );

    /**
     * @param centering this dialog will be centered over the "centering" component.
     */
    public AlignmentBoardControlsDialog( Component centering, VolumeModel volumeModel ) {
        this(centering, volumeModel, new AlignmentBoardSettings());
    }

    /**
     * @param centering this dialog will be centered over the "centering" component.  Push externally-created settings
     *                  in here as a seed.
     */
    public AlignmentBoardControlsDialog( Component centering, VolumeModel volumeModel, AlignmentBoardSettings settings ) {
        this.settings = settings;
        this.setModal( false );
        this.setSize(SIZE);
        this.centering = centering;
        this.listeners = new ArrayList<ControlsListener>();
        this.setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
        this.volumeModel = volumeModel;
        createGui();
        Double downsampleRate = AlignmentBoardControlsDialog.UNSELECTED_DOWNSAMPLE_RATE;
        try {
            downsampleRate = (Double)SessionMgr.getSessionMgr().getModelProperty(DOWN_SAMPLE_PROP_NAME);
            if ( downsampleRate == null ) {
                downsampleRate = AlignmentBoardControlsDialog.UNSELECTED_DOWNSAMPLE_RATE;
            }
        } catch ( Throwable ex ) {
            logger.warn( "Failed to fetch downsample rate setting." );
        }
        this.setNonSerializedDownSampleRate(downsampleRate);
    }

    /** Take the launch method as the place to update all controls. */
    public void setVisible( boolean visible ) {
        if ( visible ) {
            updateControlsFromSettings();
            updateCurrentSelectionFromSettings();
        }
        super.setVisible(visible);
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
    }

    /**
     * Returns something sufficient to get this thing on screen.
     *
     * @return an Action that will launch the settings dialog.
     */
    public AbstractAction getLaunchAction() {
        return new LaunchAction();
    }

    //--------------------------------------------HELPERS
    private boolean isUseSignalData() {
        if ( ! readyForOutput ) {
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
        if ( ! readyForOutput )
            return settings.getGammaFactor();
        int value = brightnessSlider.getValue();
        double rtnVal = (((double)value/100.0) - 10.0) / -5.0;
        logger.info( "Returning gamma factor of {} for {}.", rtnVal, value );
        return rtnVal;
    }

    /** These getters may be called after successful launch. */
    private double getDownsampleRate() {
        if ( ! readyForOutput )
            return settings.getChosenDownSampleRate();
        String selectedValue = downSampleRateDropdown.getItemAt( downSampleRateDropdown.getSelectedIndex() ).toString();
        if ( Character.isDigit( selectedValue.charAt( 0 ) ) )
            return Integer.parseInt( selectedValue );
        else
            return 0.0;
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
    }

    /** Call this when sufficient info is avail to get the sliders positions initialized off crop-coords. */
    private void updateCurrentSelectionFromSettings() {
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

    /** As soon as the ranges are known (set), listeners, and initial ranges may be set on volume selection. */
    private void initializeSelectionRanges() {
        if ( selectionSliderListener != null  &&  xSlider != null ) {
            xSlider.removeChangeListener( selectionSliderListener );
            ySlider.removeChangeListener( selectionSliderListener );
            zSlider.removeChangeListener( selectionSliderListener );
        }

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
        if ( newGamma != settings.getGammaFactor() ) {
            settings.setGammaFactor( newGamma );
            deltaBrightness = true;
        }

        boolean newUseSignal = isUseSignalData();
        if ( newUseSignal != settings.isShowChannelData() ) {
            settings.setShowChannelData( newUseSignal );
            deltaSettings = true;
        }

        double newDownSampleRate = getDownsampleRate();
        if ( newDownSampleRate != settings.getChosenDownSampleRate() ) {
            settings.setChosenDownSampleRate(newDownSampleRate);
            serializeDownsampleRate( newDownSampleRate );
            deltaSettings = true;
        }

        for ( ControlsListener listener: listeners ) {
            if ( deltaBrightness ) {
                logger.info("Setting brightness to {}.", settings.getGammaFactor() );
                listener.setBrightness( settings.getGammaFactor() );
            }
            if ( deltaSettings ) {
                logger.info("Serializing show-channel as {} / {}.", settings.isShowChannelData(), newUseSignal );
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

    private synchronized void fireSavebackEvent(
            Collection<float[]> absoluteCoords,
            CompletionListener completionListener,
            ControlsListener.ExportMethod method
    ) {
        for ( ControlsListener listener: listeners ) {
            listener.exportSelection(absoluteCoords, completionListener, method);
        }

    }

    private synchronized void fireBlackOutCrop( boolean blackout ) {
        for ( ControlsListener listener: listeners ) {
            listener.setCropBlackout(blackout);
        }
    }

    private void createGui() {
        setLayout( new BorderLayout() );

        xSlider = new RangeSlider();
        ySlider = new RangeSlider();
        zSlider = new RangeSlider();

        xSlider.setBorder( new TitledBorder( "Selection X Bounds" ) );
        ySlider.setBorder( new TitledBorder( "Selection Y Bounds" ) );
        zSlider.setBorder(new TitledBorder("Selection Z Bounds"));

        blackoutCheckbox = new JCheckBox( NON_SELECT_BLACKOUT );
        blackoutCheckbox.setToolTipText( NON_SELECT_BLACKOUT_TOOLTIP_TEXT );
        blackoutCheckbox.setSelected( false );
        blackoutCheckbox.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                fireBlackOutCrop( blackoutCheckbox.isSelected() );
            }
        });

        final JButton commitButton = new JButton( COMMIT_CHANGES );
        commitButton.setToolTipText( COMMIT_CHANGES_TOOLTIP_TEXT );
        commitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                readyForOutput = true;
                fireSettingsEvent();
                commitButton.setEnabled( false );
            }
        });
        commitButton.setEnabled( false );

        final JButton searchSaveButton = new JButton( SAVE_AS_SEARCH_TIFF );
        searchSaveButton.setToolTipText( SAVE_AS_SEARCH_TIFF_TOOLTIP_TEXT );
        searchSaveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                logger.info(
                        "Selection covers (X): " + xSlider.getValue() + ".." + xSlider.getUpperValue() +
                                " and (Y): " + ySlider.getValue() + ".." + ySlider.getUpperValue() +
                                " and (Z): " + zSlider.getValue() + ".." + zSlider.getUpperValue()
                );
                setButtonBusy(searchSaveButton);
                CompletionListener buttonEnableListener = new CompletionListener() {
                    @Override
                    public void complete() {
                        setButtonRelaxed(searchSaveButton, SAVE_AS_SEARCH_TIFF_TOOLTIP_TEXT);
                    }
                };

                Collection<float[]> acceptedCords = getCombinedCropCoords();
                fireSavebackEvent( acceptedCords, buttonEnableListener, ControlsListener.ExportMethod.binary );

            }
        });

        final JButton colorSaveButton = new JButton( SAVE_AS_COLOR_TIFF );
        colorSaveButton.setToolTipText( SAVE_AS_COLOR_TIFF_TOOLTIP_TEXT );
        colorSaveButton.addActionListener(new ActionListener() {
            CompletionListener buttonEnableListener = new CompletionListener() {
                @Override
                public void complete() {
                    setButtonRelaxed(colorSaveButton, SAVE_AS_COLOR_TIFF_TOOLTIP_TEXT);
                }
            };

            public void actionPerformed(ActionEvent ae) {
                setButtonBusy(colorSaveButton);
                Collection<float[]> acceptedCords = getCombinedCropCoords();
                fireSavebackEvent(acceptedCords, buttonEnableListener, ControlsListener.ExportMethod.color);
            }
        });

        final JButton screenShotButton = new JButton( SAVE_SCREEN_SHOT_MIP );
        screenShotButton.setToolTipText( SAVE_SCREEN_SHOT_TOOLTIP_TEXT );
        screenShotButton.addActionListener(new ActionListener() {
            CompletionListener buttonEnableListener = new CompletionListener() {
                @Override
                public void complete() {
                    screenShotButton.setEnabled(true);
                }
            };

            public void actionPerformed( ActionEvent ae ) {
                screenShotButton.setEnabled( false );
                Collection<float[]> acceptedCords = getCombinedCropCoords();
                fireSavebackEvent( acceptedCords, buttonEnableListener, ControlsListener.ExportMethod.mip );
            }
        });

        JPanel regionSelectionPanel = new JPanel();
        regionSelectionPanel.setLayout(new GridLayout(4, 1));
        regionSelectionPanel.add(xSlider);
        regionSelectionPanel.add(ySlider);
        regionSelectionPanel.add(zSlider);

        JButton clearButton = new JButton( CLEAR_BUTTON_LABEL );
        clearButton.setToolTipText( CLEAR_BUTTON_TOOLTIP_TEXT );
        clearButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                resetSelectionSliders();
                CropCoordSet cropCoordSet = volumeModel.getCropCoords();
                cropCoordSet.setCurrentCoordinates(getCurrentCropCoords());
                cropCoordSet.getAcceptedCoordinates().clear();
                fireForceCropEvent();
            }
        });

        JButton orButton = new JButton( OR_BUTTON_LABEL );
        orButton.setToolTipText( OR_BUTTON_TIP );
        orButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                CropCoordSet cropCoordSet = volumeModel.getCropCoords();
                cropCoordSet.acceptCurrentCoordinates();
                fireCropEvent();
            }
        });

        JPanel accumulatorButtonsPanel = new JPanel();
        accumulatorButtonsPanel.setLayout( new BorderLayout() );
        accumulatorButtonsPanel.add( orButton, BorderLayout.EAST );
        accumulatorButtonsPanel.add( clearButton, BorderLayout.WEST );
        regionSelectionPanel.add( accumulatorButtonsPanel );

        regionSelectionPanel.setToolTipText(GEO_SEARCH_TOOLTIP);

        brightnessSlider = new JSlider();
        brightnessSlider.setMaximum( 1000 );
        brightnessSlider.setMinimum(0);
        brightnessSlider.setMajorTickSpacing(500);
        brightnessSlider.setMinorTickSpacing(100);
        brightnessSlider.setLabelTable(brightnessSlider.createStandardLabels(100));
        brightnessSlider.setOrientation(JSlider.HORIZONTAL);
        brightnessSlider.setValue(500);  // Center it up.
        brightnessSlider.setPaintLabels(true);
        brightnessSlider.setPaintTicks(true);
        brightnessSlider.setToolTipText( GAMMA_TOOLTIP );
        brightnessSlider.setBorder(new TitledBorder("Brightness"));

        downSampleRateToIndex = new HashMap<Integer,Integer>();
        downSampleRateToIndex.put( 0, 0 );
        downSampleRateToIndex.put( 1, 1 );
        downSampleRateToIndex.put( 2, 2 );
        downSampleRateToIndex.put( 4, 3 );
        downSampleRateToIndex.put( 8, 4 );

        downSampleRateDropdown = new JComboBox(
                new ABSDComboBoxModel( downSampleRateToIndex )
        );
        downSampleRateDropdown.setBorder( new TitledBorder( DOWN_SAMPLE_RATE ) );
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

        brightnessSlider.addMouseListener( commitEnablerMouseListener );
        downSampleRateDropdown.addMouseListener( commitEnablerMouseListener );
        for ( Component c: downSampleRateDropdown.getComponents() ) {
            // NOTE: must add the listener to all subcomponents of the combo box, or response is unreliable.
            c.addMouseListener( commitEnablerMouseListener );
        }
        useSignalDataCheckbox.addMouseListener( commitEnablerMouseListener );

        JPanel centralPanel = new JPanel();
        centralPanel.setLayout( new GridBagLayout() );
        /*
         * Run-down of GBC constructor params.
         * @param gridx     The initial gridx value.
         * @param gridy     The initial gridy value.
         * @param gridwidth The initial gridwidth value.
         * @param gridheight        The initial gridheight value.
         * @param weightx   The initial weightx value.
         * @param weighty   The initial weighty value.
         * @param anchor    The initial anchor value.
         * @param fill      The initial fill value.
         * @param insets    The initial insets value.
         * @param ipadx     The initial ipadx value.
         * @param ipady     The initial ipady value.
         */
        Insets insets = new Insets( 8, 8, 8, 8 );
        int rowHeight = 1;
        int nextRow = 0;
        GridBagConstraints brightnessConstraints = new GridBagConstraints(
                0, nextRow, 4, rowHeight, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0
        );

        nextRow += rowHeight;
        GridBagConstraints downSampleConstraints = new GridBagConstraints(
                0, nextRow, 3, rowHeight, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, insets, 0, 0
        );
        downSampleRateDropdown.setMinimumSize(DN_SAMPLE_DROPDOWN_SIZE);
        downSampleRateDropdown.setMaximumSize(DN_SAMPLE_DROPDOWN_SIZE);
        downSampleRateDropdown.setPreferredSize(DN_SAMPLE_DROPDOWN_SIZE);

        nextRow += rowHeight;
        GridBagConstraints signalDataConstraints = new GridBagConstraints(
                1, nextRow, 2, rowHeight, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, insets, 0, 0
        );

        rowHeight = 2;
        GridBagConstraints commitBtnConstraints = new GridBagConstraints(
                0, nextRow, 2, rowHeight, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0
        );

        nextRow += rowHeight;
        rowHeight = 3;
        GridBagConstraints downSampleTipConstraints = new GridBagConstraints(
                0, nextRow, 4, rowHeight, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0
        );

        nextRow += rowHeight;
        rowHeight = 1;
        GridBagConstraints regionSelectionPanelConstraints = new GridBagConstraints(
                0, nextRow, 3, rowHeight, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0
        );

        nextRow += rowHeight;
        GridBagConstraints blackoutCheckboxConstraints = new GridBagConstraints(
                0, nextRow, 3, rowHeight, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, insets, 0, 0
        );

        nextRow += rowHeight;
        Insets buttonInsets = new Insets( 5, 5, 5, 5 );
        GridBagConstraints saveSearchConstraints = new GridBagConstraints(
                0, nextRow, 1, rowHeight, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, buttonInsets, 0, 0
        );

        GridBagConstraints saveColorConstraints = new GridBagConstraints(
                1, nextRow, 1, rowHeight, 1.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.VERTICAL, buttonInsets, 0, 0
        );

        GridBagConstraints saveScreenShotConstraints = new GridBagConstraints(
                2, nextRow, 1, rowHeight, 1.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.VERTICAL, buttonInsets, 0, 0
        );

        centralPanel.add( brightnessSlider, brightnessConstraints );
        centralPanel.add( downSampleRateDropdown, downSampleConstraints );
        centralPanel.add( useSignalDataCheckbox, signalDataConstraints );
        centralPanel.add( commitButton, commitBtnConstraints );
        JTextArea downSampleRateText = new JTextArea( DOWN_SAMPLE_TIP );
        downSampleRateText.setLineWrap(true);
        downSampleRateText.setColumns(DOWN_SAMPLE_TIP.length() / 2);
        downSampleRateText.setEditable(false);
        downSampleRateText.setBorder(new LineBorder(Color.black));
        downSampleRateText.setPreferredSize( DOWNSAMPLE_TIP_DIM );
        downSampleRateText.setSize( DOWNSAMPLE_TIP_DIM );
        downSampleRateText.setMinimumSize( DOWNSAMPLE_TIP_DIM );

        centralPanel.add(downSampleRateText, downSampleTipConstraints);
        centralPanel.add( blackoutCheckbox, blackoutCheckboxConstraints );
        centralPanel.add( regionSelectionPanel, regionSelectionPanelConstraints );
        centralPanel.add( searchSaveButton, saveSearchConstraints );
        centralPanel.add( colorSaveButton, saveColorConstraints );
        centralPanel.add( screenShotButton, saveScreenShotConstraints );
        add(centralPanel, BorderLayout.CENTER);

        JPanel bottomButtonPanel = new JPanel();
        bottomButtonPanel.setLayout(new BorderLayout());
        JButton cancel = new JButton( DISMISS_DIALOG );
        cancel.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                readyForOutput = false;
                setVisible( false );
            }
        });

        // Mac-like layout for buttons.
        bottomButtonPanel.add( cancel, BorderLayout.WEST );
        bottomButtonPanel.setBorder( new EmptyBorder( insets ) );
        add(bottomButtonPanel, BorderLayout.SOUTH);
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

    private Collection<float[]> getMicrometerCropCoords() {
        CropCoordSet cropCoordSet = volumeModel.getCropCoords();
        Collection<float[]> micrometerCropCoords = new HashSet<float[]>( cropCoordSet.getAcceptedCoordinates().size() );
        int[] maxima = new int[] {
                xSlider.getMaximum(), ySlider.getMaximum(), zSlider.getMaximum()
        };
        for ( float[] nextAccepted: cropCoordSet.getAcceptedCoordinates() ) {
            CoordCropper3D cropper3D = new CoordCropper3D();
            float[] adjusted = cropper3D.getDenormalizedCropCoords( nextAccepted, maxima, getDownsampleRate() );
            micrometerCropCoords.add( adjusted );
        }
        return micrometerCropCoords;
    }

    private Collection<float[]> getCombinedCropCoords() {
        float[] absoluteCropCoords = getCurrentCropCoords();
        Collection<float[]> acceptedCoords = getMicrometerCropCoords();
        if ( ! CropCoordSet.alreadyAccepted( acceptedCoords, absoluteCropCoords ) ) {
            Collection<float[]> combinedCoords = new HashSet<float[]>();
            combinedCoords.add( absoluteCropCoords );
            combinedCoords.addAll( acceptedCoords );
            return combinedCoords;
        }
        else {
            return acceptedCoords;
        }
    }

    private float[] getCurrentCropCoords() {
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
                cropper.getCropCoords( xSlider, ySlider, zSlider, getDownsampleRate() ) :
                null;
    }

    //-------------------------------------------------INNER CLASSES/INTERFACES

    /** Simple list-and-map-driven combo box model. */
    class ABSDComboBoxModel implements ComboBoxModel {

        private List<Integer> rates = new ArrayList<Integer>();
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

    public class LaunchAction extends AbstractAction {
        public LaunchAction() {
            super( LAUNCH_AS );
        }

        @Override
        public Object getValue(String key) {
            if ( key.equals( AbstractAction.ACTION_COMMAND_KEY ) )
                return LAUNCH_AS;
            else if ( key.equals( AbstractAction.LONG_DESCRIPTION ) )
                return LAUNCH_DESCRIPTION;
            else if ( key.equals( AbstractAction.NAME ) )
                return LAUNCH_AS;
            else
                return null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            readyForOutput = false;

            int width = (int)SIZE.getWidth();
            int height = (int)SIZE.getHeight();
            int x = centering.getLocation().x + ( centering.getWidth() / 2 ) - ( width / 2 );
            int y = centering.getLocation().y + ( centering.getHeight() / 2 ) - ( height / 2 );
            AlignmentBoardControlsDialog.this.setLocation( x, y );
            AlignmentBoardControlsDialog.this.setVisible( true );
        }

    }

    /** This listener updates the current display with latest slider tweaks. */
    public static class SliderChangeListener implements ChangeListener {
        private final RangeSlider[] sliders;
        private final AlignmentBoardControlsDialog dialog;
        private final VolumeModel volumeModel;

        public SliderChangeListener(
                RangeSlider[] rangeSliders,
                VolumeModel volumeModel,
                AlignmentBoardControlsDialog dialog
        ) {
            this.sliders = rangeSliders;
            this.volumeModel = volumeModel;
            this.dialog = dialog;
        }

        public void stateChanged(ChangeEvent e) {
            float[] cropCoords = new CoordCropper3D().getNormalizedCropCoords(sliders);
            CropCoordSet cropCoordSet = volumeModel.getCropCoords();
            cropCoordSet.setCurrentCoordinates( cropCoords );
            dialog.fireCropEvent();
        }

    }

}
