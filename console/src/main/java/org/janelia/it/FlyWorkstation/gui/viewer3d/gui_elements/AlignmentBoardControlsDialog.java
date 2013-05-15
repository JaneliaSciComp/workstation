package org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.CoordCropper3D;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.CropCoordSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    public static final double DEFAULT_DOWNSAMPLE_RATE = 2.0;
    private static final String DOWN_SAMPLE_TOOLTIP =
            "<html>" +
            "Data sent to screen may be too large for your graphics card.<br>" +
            "Therefore, they are downsampled at a default rate of 1.0:" + DEFAULT_DOWNSAMPLE_RATE +
            ".<br>However, you may wish to move that rate up or down, depending on your knowledge of the<br>" +
            "advanced hardware on your system.  Higher values mean larger, blockier voxels, and less memory."
            + "</html>";
    public static final double DEFAULT_GAMMA = 1.0;

    private static final String GEO_SEARCH_TOOLTIP = "<html>" +
            "Adjust the X, Y, and Z sliders, to leave only the template volume of <br>" +
            "search lit.  This template volume will then be used to derive the coordinates, <br>" +
            "to search other specimens and present the resulting overlappoing volume." +
            "</html>";

    private static final String SAVE_AS_SEARCH_TIFF = "Save Search Mask";
    private static final String SAVE_AS_COLOR_TIFF = "Save Color TIFF";

    private static final String LAUNCH_AS = "Controls";
    private static final String LAUNCH_DESCRIPTION = "Present a dialog allowing users to change settings.";
    private static final Dimension SIZE = new Dimension( 450, 600 );
    private static final String GAMMA_TOOLTIP = "Adjust the gamma level, or brightness.";
    private static final Dimension DN_SAMPLE_DROPDOWN_SIZE = new Dimension(130, 50);
    private static final String COMMIT_CHANGES = "Commit Changes";
    private static final String DISMISS_DIALOG = "Done";
    private static final String SAVE_SCREEN_SHOT_MIP = "Screen Shot/MIP";
    private static final String DOWN_SAMPLE_RATE = "Down Sample Rate";
    private static final String USE_SIGNAL_DATA = "Use Signal Data";
    private static final String OR_BUTTON_TIP = "<html>Combine <font color='red'>this</font> selection region<br>" +
            "with previous selection region(s)<br></html>";
    private static final String OR_BUTTON_LABEL = "OR";
    private static final String CLEAR_BUTTON_LABEL = "Clear Selection";
    private static final String CLEAR_BUTTON_TOOLTIP_TEXT = "Drop all sub-volume selections made in this session.";

    private Component centering;
    private JSlider brightnessSlider;
    private JCheckBox useSignalDataCheckbox;
    private JComboBox downSampleRateDropdown;

    private RangeSlider xSlider;
    private RangeSlider ySlider;
    private RangeSlider zSlider;

    private JCheckBox blackoutCheckbox;

    private double currentGamma = DEFAULT_GAMMA;
    private double currentDownSampleRate;
    private boolean currentUseSignalData = true;

    private boolean readyForOutput = false;

    private Map<Integer,Integer> downSampleRateToIndex;
    private CropCoordSet cropCoordSet;
    private Collection<ControlsListener> listeners;

    private Logger logger = LoggerFactory.getLogger( AlignmentBoardControlsDialog.class );

    /**
     * @param centering this dialog will be centered over the "centering" component.
     */
    public AlignmentBoardControlsDialog(Component centering, CropCoordSet cropCoordSet ) {
        this.setModal( false );
        this.setSize(SIZE);
        this.centering = centering;
        this.listeners = new ArrayList<ControlsListener>();
        this.cropCoordSet = cropCoordSet;
        this.setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
        createGui();
    }

    /** Control who observes.  Synchronized for thread safety. */
    public synchronized void addSettingsListener( ControlsListener listener ) {
        listeners.add(listener);
    }

    public synchronized void removeSettingsListener( ControlsListener listener ) {
        listeners.remove(listener);
    }

    /**
     * Returns all settings here, at one swoop.
     * @return user input.
     */
    public AlignmentBoardSettings getAlignmentBoardSettings() {
        AlignmentBoardSettings settings = new AlignmentBoardSettings();
        settings.setDownSampleRate( getDownsampleRate() );
        settings.setGammaFactor( getGammaFactor() );
        settings.setShowChannelData( isUseSignalData() );
        return settings;
    }

    public void setVolumeMaxima( int x, int y, int z ) {
        logger.info("Volume maxima provided {}, {}, " + z , x, y );
        xSlider.setValue(0);
        xSlider.setMaximum(x);
        xSlider.setUpperValue(x);

        ySlider.setValue(0);
        ySlider.setMaximum(y);
        ySlider.setUpperValue(y);

        zSlider.setValue( 0 );
        zSlider.setMaximum(z);
        zSlider.setUpperValue(z);
    }

    /**
     * Returns something sufficient to get this thing on screen.
     *
     * @return an Action that will launch the settings dialog.
     */
    public AbstractAction getLaunchAction() {
        return new LaunchAction();
    }

    /** These getters may be called after successful launch. */
    public double getDownsampleRate() {
        if ( ! readyForOutput )
            return currentDownSampleRate;
        return (Integer)downSampleRateDropdown.getItemAt( downSampleRateDropdown.getSelectedIndex() );
    }

    /**
     * Compute a factor suitable for gamma adjustment in the shader, given a range of 0..10 from the slider.
     * Max-point of 10 yields 0.0 (exponent takes to 1.0 in shader).
     * Mid-point of 5 yields 1.0, or same output.
     * Low point of 0 yeilds 2.0.
     *
     * Smaller values get pushed more dim with lower slider pos.  Values close to 1.0 do not get diminished much.
     * May require further attention after testing.
     *
     * @return computation above
     */
    public double getGammaFactor() {
        if ( ! readyForOutput )
            return currentGamma;
        int value = brightnessSlider.getValue();
        double rtnVal = (((double)value/100.0) - 10.0) / -5.0;
        logger.info( "Returning gamma factor of {} for {}.", rtnVal, value );
        return rtnVal;
    }

    public boolean isReadyForOutput() {
        return readyForOutput;
    }

    public void setDownSampleRate( double downSampleRate ) {
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
        currentDownSampleRate = downSampleRate;
    }

    public void setUseSignalData( boolean use ) {
        this.currentUseSignalData = use;
    }

    public boolean isUseSignalData() {
        if ( ! readyForOutput ) {
            return this.currentUseSignalData;
        }
        else {
            return useSignalDataCheckbox.isSelected();
        }
    }

    //--------------------------------------------HELPERS

    /**
     * This will fire only those listener methods affected by the current user commitment.
     */
    private synchronized void fireSettingsEvent() {
        for ( ControlsListener listener: listeners ) {
            double newGamma = getGammaFactor();
            if ( newGamma != currentGamma ) {
                currentGamma = newGamma;
                listener.setBrightness( currentGamma );
            }
            double newDownSampleRate = getDownsampleRate();
            boolean newUseSignal = isUseSignalData();
            if ( newDownSampleRate != currentDownSampleRate  ||
                    newUseSignal != currentUseSignalData ) {

                currentDownSampleRate = newDownSampleRate;
                currentUseSignalData = newUseSignal;
                listener.updateSettings();

            }

        }
    }

    private synchronized void fireSettingsEvent( CropCoordSet cropCoordSet ) {
        if ( cropCoordSet.getAcceptedCoordinates().size() > 0  ||  cropCoordSet.getCurrentCoordinates() != null ) {
            for ( ControlsListener listener: listeners ) {
                listener.setSelectedCoords( cropCoordSet );
            }
        }
    }

    private synchronized void fireForceCropEvent( CropCoordSet cropCoordSet ) {
        for ( ControlsListener listener: listeners ) {
            listener.setSelectedCoords( cropCoordSet );
            listener.updateSettings();
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

        xSlider = new RangeSlider( 0, 100 );  // Dummy starting ranges.
        ySlider = new RangeSlider( 0, 100 );
        zSlider = new RangeSlider( 0, 100 );

        ChangeListener listener = new SliderChangeListener(
                new RangeSlider[]{ xSlider, ySlider, zSlider }, cropCoordSet, this
        );

        xSlider.addChangeListener( listener );
        ySlider.addChangeListener( listener );
        zSlider.addChangeListener( listener );

        xSlider.setBorder( new TitledBorder( "Selection X Bounds" ) );
        ySlider.setBorder( new TitledBorder( "Selection Y Bounds" ) );
        zSlider.setBorder( new TitledBorder( "Selection Z Bounds" ) );

        blackoutCheckbox = new JCheckBox( "Non-selected region blacked out" );
        blackoutCheckbox.setSelected( false );
        blackoutCheckbox.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                fireBlackOutCrop( blackoutCheckbox.isSelected() );
            }
        });

        final JButton commitButton = new JButton( COMMIT_CHANGES );
        commitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                readyForOutput = true;
                fireSettingsEvent();
                commitButton.setEnabled( false );
            }
        });
        commitButton.setEnabled( false );

        final JButton searchSaveButton = new JButton( SAVE_AS_SEARCH_TIFF );
        searchSaveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                logger.info(
                        "Selection covers (X): " + xSlider.getValue() + ".." + xSlider.getUpperValue() +
                                " and (Y): " + ySlider.getValue() + ".." + ySlider.getUpperValue() +
                                " and (Z): " + zSlider.getValue() + ".." + zSlider.getUpperValue()
                );
                searchSaveButton.setEnabled(false);
                CompletionListener buttonEnableListener = new CompletionListener() {
                    @Override
                    public void complete() {
                        searchSaveButton.setEnabled(true);
                    }
                };

                Collection<float[]> acceptedCords = getCombinedCropCoords();
                fireSavebackEvent( acceptedCords, buttonEnableListener, ControlsListener.ExportMethod.binary );

            }
        });

        final JButton colorSaveButton = new JButton( SAVE_AS_COLOR_TIFF );
        colorSaveButton.addActionListener(new ActionListener() {
            CompletionListener buttonEnableListener = new CompletionListener() {
                @Override
                public void complete() {
                    colorSaveButton.setEnabled( true );
                }
            };

            public void actionPerformed(ActionEvent ae) {
                colorSaveButton.setEnabled( false );
                Collection<float[]> acceptedCords = getCombinedCropCoords();
                fireSavebackEvent(acceptedCords, buttonEnableListener, ControlsListener.ExportMethod.color);
            }
        });

        final JButton screenShotButton = new JButton( SAVE_SCREEN_SHOT_MIP );
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
                cropCoordSet.setCurrentCoordinates( getCurrentCropCoords() );
                cropCoordSet.setAcceptedCoordinates(new ArrayList<float[]>());
                fireForceCropEvent(cropCoordSet);
            }
        });

        JButton orButton = new JButton( OR_BUTTON_LABEL );
        orButton.setToolTipText( OR_BUTTON_TIP );
        orButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                cropCoordSet.acceptCurrentCoordinates();
                fireSettingsEvent( cropCoordSet );
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
        downSampleRateToIndex.put( 1, 0 );
        downSampleRateToIndex.put( 2, 1 );
        downSampleRateToIndex.put( 4, 2 );
        downSampleRateToIndex.put( 8, 3 );

        downSampleRateDropdown = new JComboBox(
                new ABSDComboBoxModel( downSampleRateToIndex )
        );
        downSampleRateDropdown.setBorder( new TitledBorder( DOWN_SAMPLE_RATE ) );
        downSampleRateDropdown.setToolTipText( DOWN_SAMPLE_TOOLTIP );

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
        GridBagConstraints brightnessConstraints = new GridBagConstraints(
                0, 0, 3, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0
        );

        GridBagConstraints downSampleConstraints = new GridBagConstraints(
                0, 1, 3, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, insets, 0, 0
        );
        downSampleRateDropdown.setMinimumSize(DN_SAMPLE_DROPDOWN_SIZE);
        downSampleRateDropdown.setMaximumSize(DN_SAMPLE_DROPDOWN_SIZE);
        downSampleRateDropdown.setPreferredSize(DN_SAMPLE_DROPDOWN_SIZE);

        GridBagConstraints signalDataConstraints = new GridBagConstraints(
                1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.VERTICAL, insets, 0, 0
        );

        GridBagConstraints commitBtnConstraints = new GridBagConstraints(
                0, 2, 2, 2, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0
        );

        GridBagConstraints regionSelectionPanelConstraints = new GridBagConstraints(
                0, 4, 3, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0
        );

        GridBagConstraints blackoutCheckboxConstraints = new GridBagConstraints(
                0, 5, 3, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, insets, 0, 0
        );

        Insets buttonInsets = new Insets( 5, 5, 5, 5 );
        GridBagConstraints saveSearchConstraints = new GridBagConstraints(
                0, 6, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, buttonInsets, 0, 0
        );

        GridBagConstraints saveColorConstraints = new GridBagConstraints(
                1, 6, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.VERTICAL, buttonInsets, 0, 0
        );

        GridBagConstraints saveScreenShotConstraints = new GridBagConstraints(
                2, 6, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.VERTICAL, buttonInsets, 0, 0
        );

        centralPanel.add( brightnessSlider, brightnessConstraints );
        centralPanel.add( downSampleRateDropdown, downSampleConstraints );
        centralPanel.add( useSignalDataCheckbox, signalDataConstraints );
        centralPanel.add( commitButton, commitBtnConstraints );
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
            selectedItem = DEFAULT_DOWNSAMPLE_RATE;
        }

        @Override
        public int getSize() {
            return rates.size();
        }

        @Override
        public Object getElementAt(int index) {
            return rates.get( index );
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
        private RangeSlider[] sliders;
        private AlignmentBoardControlsDialog dialog;
        private CropCoordSet cropCoordSet;

        public SliderChangeListener(
                RangeSlider[] rangeSliders,
                CropCoordSet cropCoordSet,
                AlignmentBoardControlsDialog dialog
        ) {
            this.sliders = rangeSliders;
            this.cropCoordSet = cropCoordSet;
            this.dialog = dialog;
        }

        public void stateChanged(ChangeEvent e) {
            float[] cropCoords = new CoordCropper3D().getNormalizedCropCoords( sliders );
            cropCoordSet.setCurrentCoordinates( cropCoords );
            dialog.fireSettingsEvent( cropCoordSet );
        }

    }

}
