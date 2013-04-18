package org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.CoordCropper3D;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.VolumeWritebackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
public class AlignmentBoardSettingsDialog extends JDialog {
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

    private static final String LAUNCH_AS = "Alignment Board Settings";
    private static final String LAUNCH_DESCRIPTION = "Present a dialog allowing users to change settings.";
    private static final Dimension SIZE = new Dimension( 350, 250 );
    private static final String GAMMA_TOOLTIP = "Adjust the gamma level, or brightness.";

    private Component centering;
    private JButton go;
    private JSlider brightnessSlider;
    private JCheckBox useSignalDataCheckbox;
    private JComboBox downSampleRateDropdown;

    private RangeSlider xSlider;
    private RangeSlider ySlider;
    private RangeSlider zSlider;

    private double currentGamma = DEFAULT_GAMMA;
    private double currentDownSampleRate;
    private boolean currentUseSignalData = true;

    private boolean readyForOutput = false;

    private Map<Integer,Integer> downSampleRateToIndex;
    private Collection<SettingsListener> listeners;

    private Logger logger = LoggerFactory.getLogger( AlignmentBoardSettingsDialog.class );


    /**
     * @param centering this dialog will be centered over the "centering" component.
     */
    public AlignmentBoardSettingsDialog( Component centering ) {
        this.setModal( false );
        this.setSize(SIZE);
        this.centering = centering;
        this.listeners = new ArrayList<SettingsListener>();
        this.setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
        createGui();
    }

    /** Control who observes.  Synchronized for thread safety. */
    public synchronized void addSettingsListener( SettingsListener listener ) {
        listeners.add(listener);
    }

    public synchronized void removeSettingsListener( SettingsListener listener ) {
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
        xSlider.setUpperValue( x );

        ySlider.setValue(0);
        ySlider.setMaximum(y);
        ySlider.setUpperValue(y);

        zSlider.setValue( 0 );
        zSlider.setMaximum( z );
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
        for ( SettingsListener listener: listeners ) {
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

    private synchronized void fireSettingsEvent( float[] cropCoords ) {
        for ( SettingsListener listener: listeners ) {
            listener.setSelectedCoords( cropCoords );
        }
    }

    private synchronized void fireSavebackEvent( float[] absoluteCoords ) {
        for ( SettingsListener listener: listeners ) {
            listener.exportSelection( absoluteCoords );
        }

    }

    private void createGui() {
        setLayout( new BorderLayout() );

        xSlider = new RangeSlider( 0, 100 );  // Dummy starting ranges.
        ySlider = new RangeSlider( 0, 100 );
        zSlider = new RangeSlider( 0, 100 );

        ChangeListener listener = new SliderChangeListener( xSlider, ySlider, zSlider, this );

        xSlider.addChangeListener( listener );
        ySlider.addChangeListener( listener );
        zSlider.addChangeListener( listener );

        xSlider.setBorder( new TitledBorder( "Selection X Bounds" ) );
        ySlider.setBorder( new TitledBorder( "Selection Y Bounds" ) );
        zSlider.setBorder( new TitledBorder( "Selection Z Bounds" ) );

        JButton searchButton = new JButton( "Geometric Search" );
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout( new GridLayout( 1, 4 ) );
        sliderPanel.add( xSlider );
        sliderPanel.add( ySlider );
        sliderPanel.add( zSlider );
        sliderPanel.add( searchButton );
        sliderPanel.setToolTipText(GEO_SEARCH_TOOLTIP);
        searchButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                logger.info(
                        "Selection covers (X): " + xSlider.getValue() + ".." + xSlider.getUpperValue() +
                                " and (Y): " + ySlider.getValue() + ".." + ySlider.getUpperValue() +
                                " and (Z): " + zSlider.getValue() + ".." + zSlider.getUpperValue()
                );
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
                float[] absoluteCropCoords =
                        partialVolumeConstraints ?
                                cropper.getCropCoords( xSlider, ySlider, zSlider, getDownsampleRate() ) :
                                null;

                fireSavebackEvent( absoluteCropCoords );

            }
        });

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
        downSampleRateDropdown.setBorder( new TitledBorder( "Down Sample Rate" ) );
        downSampleRateDropdown.setToolTipText( DOWN_SAMPLE_TOOLTIP );

        useSignalDataCheckbox = new JCheckBox( "Use Signal Data" );
        useSignalDataCheckbox.setSelected(true);

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
        Insets insets = new Insets( 3, 3, 3, 3 );
        GridBagConstraints brightnessConstraints = new GridBagConstraints(
                0, 0, 3, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0
        );

        GridBagConstraints downSampleConstraints = new GridBagConstraints(
                0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0
        );

        GridBagConstraints signalDataConstraints = new GridBagConstraints(
                0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0
        );

        GridBagConstraints sliderPanelConstraints = new GridBagConstraints(
                0, 3, 2, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0
        );

        centralPanel.add( brightnessSlider, brightnessConstraints );
        centralPanel.add( downSampleRateDropdown, downSampleConstraints );
        centralPanel.add( useSignalDataCheckbox, signalDataConstraints );
        centralPanel.add( sliderPanel, sliderPanelConstraints );
        add(centralPanel, BorderLayout.CENTER);

        JPanel bottomButtonPanel = new JPanel();
        bottomButtonPanel.setLayout( new BorderLayout() );
        go = new JButton( "OK" );
        go.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                readyForOutput = true;
                fireSettingsEvent();
            }
        });
        JButton cancel = new JButton( "Done/Cancel" );
        cancel.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                readyForOutput = false;
                setVisible( false );
            }
        });

        // Mac-like layout for buttons.
        bottomButtonPanel.add( cancel, BorderLayout.WEST );
        bottomButtonPanel.add( go, BorderLayout.EAST );
        Insets buttonBorderInsets = new Insets( 2, 2, 2, 2 );
        bottomButtonPanel.setBorder( new EmptyBorder( buttonBorderInsets ) );
        add(bottomButtonPanel, BorderLayout.SOUTH);
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

    /** Callers should implement this to observe the input settings provided by uesr. */
    public static interface SettingsListener {
        void setBrightness( double brightness );
        void updateSettings();
        void setSelectedCoords( float[] normalizedCoords );
        void exportSelection( float[] absoluteCoords );
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
            AlignmentBoardSettingsDialog.this.setLocation( x, y );
            AlignmentBoardSettingsDialog.this.setVisible( true );
        }

    }

    // Add listener to update display.
    public static class SliderChangeListener implements ChangeListener {
        private RangeSlider[] sliders;
        private AlignmentBoardSettingsDialog dialog;

        public SliderChangeListener(
                RangeSlider xSlider, RangeSlider ySlider, RangeSlider zSlider, AlignmentBoardSettingsDialog dialog
        ) {
            this.sliders = new RangeSlider[] {
                    xSlider, ySlider, zSlider
            };
            this.dialog = dialog;
        }

        public void stateChanged(ChangeEvent e) {
            float[] cropCoords = new CoordCropper3D().getNormalizedCropCoords( sliders );
            dialog.fireSettingsEvent( cropCoords );

        }

    }

}
